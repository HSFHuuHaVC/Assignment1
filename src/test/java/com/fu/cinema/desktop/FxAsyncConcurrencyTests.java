package com.fu.cinema.desktop;

import com.fu.cinema.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "desktop.ui.tests", matches = "true")
class FxAsyncConcurrencyTests {
    @BeforeAll static void startFx() throws Exception {
        var ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @AfterAll static void stopFx() { Platform.exit(); }

    @Test void overlappingTasksKeepControlDisabledUntilLastCompletionInEitherOrder() throws Exception {
        for (boolean initiallyDisabled : new boolean[]{false, true}) {
            for (int first : new int[]{0, 1}) {
                var gates = new CountDownLatch[]{new CountDownLatch(1), new CountDownLatch(1)};
                var finished = new CountDownLatch[]{new CountDownLatch(1), new CountDownLatch(1)};
                var async = new FxAsync();
                Button busy = fx(() -> {
                    var button = new Button("Loading");
                    button.setDisable(initiallyDisabled);
                    for (int i = 0; i < 2; i++) {
                        int index = i;
                        async.run(() -> { gates[index].await(); return index; },
                                value -> finished[index].countDown(), error -> finished[index].countDown(), button);
                    }
                    return button;
                });
                try {
                    gates[first].countDown();
                    assertTrue(finished[first].await(5, TimeUnit.SECONDS));
                    assertTrue(fx(busy::isDisable), "Another task still owns the busy control");
                    gates[1 - first].countDown();
                    assertTrue(finished[1 - first].await(5, TimeUnit.SECONDS));
                    assertEquals(initiallyDisabled, fx(busy::isDisable), "Restore state from before the first task");
                } finally {
                    for (var gate : gates) gate.countDown();
                    async.destroy();
                }
            }
        }
    }

    @Test void cancellationAndFailureAlsoReleaseSharedBusyControl() throws Exception {
        var gate = new CountDownLatch(1);
        var cancelled = new CountDownLatch(1);
        var failed = new CountDownLatch(1);
        var async = new FxAsync();
        Button busy = fx(() -> {
            var button = new Button("Loading");
            var task = async.run(() -> { gate.await(); return 1; }, ignored -> {}, error -> {}, button);
            task.addEventHandler(javafx.concurrent.WorkerStateEvent.WORKER_STATE_CANCELLED,
                    event -> cancelled.countDown());
            async.run(() -> { gate.await(); throw new IllegalStateException("Expected failure"); },
                    ignored -> {}, error -> failed.countDown(), button);
            task.cancel(true);
            return button;
        });
        try {
            assertTrue(cancelled.await(5, TimeUnit.SECONDS));
            assertTrue(fx(busy::isDisable), "Cancellation must not unlock another active task");
            gate.countDown();
            assertTrue(failed.await(5, TimeUnit.SECONDS));
            assertFalse(fx(busy::isDisable));
        } finally { gate.countDown(); async.destroy(); }
    }

    private static <T> T fx(Callable<T> callable) throws Exception {
        var task = new FutureTask<T>(callable);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
