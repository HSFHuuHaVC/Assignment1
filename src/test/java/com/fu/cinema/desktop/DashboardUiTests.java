package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.DashboardController;
import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.DashboardData;
import com.fu.cinema.desktop.model.RoomCount;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.exception.BusinessException;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named = "desktop.ui.tests", matches = "true")
class DashboardUiTests {
    private final FxAsync async = new FxAsync();

    @AfterEach
    void stopAsync() {
        async.destroy();
    }

    @BeforeAll
    static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void stopToolkit() {
        Platform.exit();
    }

    @Test
    void dashboardFxmlLoadsRendersAndStopsTimerWhenRemoved() throws Exception {
        DashboardFacade facade = mock(DashboardFacade.class);
        when(facade.load()).thenReturn(new DashboardData(4, 12, 2, List.of(new RoomCount("Room 1", 3))));
        CountDownLatch rendered = new CountDownLatch(1);
        Scene scene = onFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop/fxml/dashboard.fxml"));
            loader.setControllerFactory(type -> new DashboardController(facade, async));
            Parent root = loader.load();
            ((Label) root.lookup("#lblShowing")).textProperty().addListener((obs, old, value) -> {
                if (value.equals("4")) rendered.countDown();
            });
            root.getProperties().put("controller", loader.getController());
            return new Scene(root);
        });
        assertTrue(rendered.await(10, TimeUnit.SECONDS), "Dashboard did not render its statistics");
        onFx(() -> {
            Parent root = scene.getRoot();
            assertEquals("12", ((Label) root.lookup("#lblCustomers")).getText());
            assertEquals("2", ((Label) root.lookup("#lblPending")).getText());
            var controller = root.getProperties().get("controller");
            var timerField = DashboardController.class.getDeclaredField("autoRefresh");
            timerField.setAccessible(true);
            Timeline timer = (Timeline) timerField.get(controller);
            assertEquals(Animation.Status.RUNNING, timer.getStatus());
            scene.setRoot(new StackPane());
            assertEquals(Animation.Status.STOPPED, timer.getStatus());
            return null;
        });
    }

    @Test
    void zeroCountRoomsShowEmptyMessageAndRefreshButtonIsBoundWhileLoading() throws Exception {
        DashboardFacade facade = mock(DashboardFacade.class);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch rendered = new CountDownLatch(1);
        when(facade.load()).thenAnswer(invocation -> {
            assertFalse(Platform.isFxApplicationThread());
            assertTrue(Thread.currentThread().isVirtual());
            started.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            return new DashboardData(0, 0, 0, List.of(new RoomCount("Room 1", 0), new RoomCount("Room 2", 0)));
        });
        Scene scene = onFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop/fxml/dashboard.fxml"));
            loader.setControllerFactory(type -> new DashboardController(facade, async));
            Parent root = loader.load();
            ((Label) root.lookup("#lblUpdated")).textProperty().addListener((obs, old, value) -> {
                if (value.contains("Chưa có suất chiếu hôm nay")) rendered.countDown();
            });
            return new Scene(root);
        });
        try {
            assertTrue(started.await(10, TimeUnit.SECONDS));
            onFx(() -> {
                Button button = (Button) scene.lookup("#btnRefresh");
                assertTrue(button.disableProperty().isBound());
                assertTrue(button.isDisabled());
                assertTrue(scene.lookup("#progress").isVisible());
                button.fire();
                return null;
            });
            verify(facade, times(1)).load();
            release.countDown();
            assertTrue(rendered.await(10, TimeUnit.SECONDS));
            onFx(() -> {
                assertFalse(((Button) scene.lookup("#btnRefresh")).isDisabled());
                assertFalse(scene.lookup("#progress").isVisible());
                return null;
            });
        } finally {
            release.countDown();
            onFx(() -> { scene.setRoot(new StackPane()); return null; });
        }
    }

    @Test
    void failedLoadReleasesBindingAndCanBeRetriedWithAnEmptyDay() throws Exception {
        DashboardFacade facade = mock(DashboardFacade.class);
        when(facade.load()).thenThrow(new BusinessException("Không thể tải thống kê"))
                .thenReturn(new DashboardData(0, 0, 0, List.of()));
        CountDownLatch failed = new CountDownLatch(1);
        CountDownLatch retried = new CountDownLatch(1);
        Scene scene = onFx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/desktop/fxml/dashboard.fxml"));
            loader.setControllerFactory(type -> new DashboardController(facade, async));
            Parent root = loader.load();
            ((Label) root.lookup("#lblUpdated")).textProperty().addListener((obs, old, value) -> {
                if (value.contains("Không thể tải thống kê")) failed.countDown();
                if (value.contains("Chưa có suất chiếu hôm nay")) retried.countDown();
            });
            return new Scene(root);
        });
        try {
            assertTrue(failed.await(10, TimeUnit.SECONDS));
            onFx(() -> {
                Button button = (Button) scene.lookup("#btnRefresh");
                assertFalse(button.isDisabled());
                assertFalse(scene.lookup("#progress").isVisible());
                button.fire();
                return null;
            });
            assertTrue(retried.await(10, TimeUnit.SECONDS));
            verify(facade, times(2)).load();
        } finally {
            onFx(() -> { scene.setRoot(new StackPane()); return null; });
        }
    }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }
}
