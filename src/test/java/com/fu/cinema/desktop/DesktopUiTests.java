package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.MainController;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import javafx.application.Platform;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Button;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.support.GenericApplicationContext;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

/** Opt in on a machine with an interactive desktop: -Ddesktop.ui.tests=true. */
@EnabledIfSystemProperty(named = "desktop.ui.tests", matches = "true")
class DesktopUiTests {
    @BeforeAll static void toolkit() throws Exception {
        var ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }
    @AfterAll static void exit() { Platform.exit(); }

    @Test void mainFxmlUsesPrototypeControllersAndFitsBothWindowSizes() throws Exception {
        fx(() -> {
            var session = new DesktopSession();
            session.login(new SessionUser(1L, null, "Administrator", Role.ADMIN));
            try (var context = new GenericApplicationContext()) {
                var manager = new SceneManager(context, session);
                context.registerBean(MainController.class, () -> new MainController(manager, session),
                        definition -> definition.setScope("prototype"));
                context.refresh();
                Stage stage = new Stage();
                try {
                    manager.init(stage);
                    manager.switchTo(View.MAIN);
                    assertNotSame(manager.load(View.MAIN).controller(), manager.load(View.MAIN).controller());
                    assertTrue(stage.getScene().lookup("#lblUser").isVisible());
                    for (int width : new int[]{1200, 1000}) {
                        stage.setWidth(width);
                        stage.setHeight(width == 1200 ? 720 : 640);
                        stage.getScene().getRoot().applyCss();
                        stage.getScene().getRoot().layout();
                        var logout = stage.getScene().lookup("#btnLogout");
                        var bounds = logout.localToScene(logout.getBoundsInLocal());
                        assertTrue(bounds.getMaxY() <= stage.getScene().getHeight());
                        snapshot(stage, "main-" + width);
                    }
                } finally { stage.close(); session.logout(); }
            }
            return null;
        });
    }

    @Test void asyncWorkUsesVirtualThreadAndRestoresBusyOnSuccessFailureAndCancellation() throws Exception {
        var async = new FxAsync();
        try {
            var complete = new CountDownLatch(1);
            var onFx = new AtomicBoolean();
            Button button = fx(() -> new Button("Work"));
            fx(() -> {
                async.run(() -> { assertTrue(Thread.currentThread().isVirtual()); return 42; }, value -> {
                    onFx.set(Platform.isFxApplicationThread() && !button.isDisable() && value == 42);
                    complete.countDown();
                }, error -> fail(error), button);
                assertTrue(button.isDisable());
                return null;
            });
            assertTrue(complete.await(10, TimeUnit.SECONDS));
            assertTrue(onFx.get());
            var failed = new CountDownLatch(1);
            fx(() -> {
                async.run(() -> { throw new IllegalStateException("expected"); }, value -> fail(), error -> {
                    assertFalse(button.isDisable()); failed.countDown();
                }, button);
                return null;
            });
            assertTrue(failed.await(10, TimeUnit.SECONDS));
            var cancelled = new CountDownLatch(1);
            fx(() -> {
                var task = async.run(() -> { new CountDownLatch(1).await(); return 1; }, value -> fail(), button);
                task.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
                    cancelled.countDown();
                });
                task.cancel(true);
                return null;
            });
            assertTrue(cancelled.await(10, TimeUnit.SECONDS));
            fx(() -> { assertFalse(button.isDisable()); return null; });
        } finally { async.destroy(); }
    }

    private static <T> T fx(java.util.concurrent.Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private static void snapshot(Stage stage, String name) throws Exception {
        WritableImage image = stage.getScene().snapshot(null);
        BufferedImage png = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        var pixels = image.getPixelReader();
        for (int y = 0; y < png.getHeight(); y++) for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, pixels.getArgb(x, y));
        Path output = Path.of("target", "desktop-ui", name + ".png");
        Files.createDirectories(output.getParent());
        ImageIO.write(png, "png", output.toFile());
    }
}
