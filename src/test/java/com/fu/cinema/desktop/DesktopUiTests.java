package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.MainController;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.desktop.util.FxAsync;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

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
        var session = new DesktopSession();
        var context = new GenericApplicationContext();
        var manager = new SceneManager(context, session);
        Stage stage = fx(() -> {
                session.login(new SessionUser(1L, null, "Administrator", Role.ADMIN));
                context.registerBean(MainController.class, () -> new MainController(manager, session),
                        definition -> definition.setScope("prototype"));
                context.refresh();
                Stage window = new Stage();
                    manager.init(window);
                    manager.switchTo(View.MAIN);
                    assertNotSame(manager.load(View.MAIN).controller(), manager.load(View.MAIN).controller());
                    assertTrue(window.getScene().lookup("#lblUser").isVisible());
                return window;
        });
        try {
                    for (int width : new int[]{1200, 1000}) {
                        fx(() -> {
                        stage.setWidth(width);
                        stage.setHeight(width == 1200 ? 720 : 640);
                        return null;
                        });
                        settle();
                        fx(() -> {
                        stage.getScene().getRoot().applyCss();
                        stage.getScene().getRoot().layout();
                        var logout = stage.getScene().lookup("#btnLogout");
                        var bounds = logout.localToScene(logout.getBoundsInLocal());
                        assertTrue(bounds.getMaxY() <= stage.getScene().getHeight());
                        assertTrue(stage.getScene().getWidth() <= width);
                        snapshot(stage, "main-" + width);
                        return null;
                        });
                    }
        } finally {
            fx(() -> { stage.close(); session.logout(); context.close(); return null; });
        }
    }

    @Test void loginFxmlValidatesAuthenticatesAndLogsOutAgainstRealH2Services() throws Exception {
        try (var context = new SpringApplicationBuilder(com.fu.cinema.FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE).headless(false).profiles("test", "desktop")
                .run("--spring.profiles.active=test", "--spring.datasource.url=jdbc:h2:mem:desktopUi;MODE=MSSQLServer")) {
            var manager = context.getBean(SceneManager.class);
            var session = context.getBean(DesktopSession.class);
            Stage stage = fx(() -> {
                Stage window = new Stage(); manager.init(window); manager.switchTo(View.LOGIN); return window;
            });
            try {
                for (int width : new int[]{1200, 1000}) {
                    fx(() -> { stage.setWidth(width); stage.setHeight(width == 1200 ? 720 : 640); return null; });
                    settle();
                    fx(() -> {
                        assertTrue(((Button) stage.getScene().lookup("#btnLogin")).isDisabled());
                        var bounds = stage.getScene().lookup("#form").localToScene(stage.getScene().lookup("#form").getBoundsInLocal());
                        assertTrue(bounds.getMinY() >= 0 && bounds.getMaxY() <= stage.getScene().getHeight());
                        snapshot(stage, "login-" + width); return null;
                    });
                }
                for (String username : new String[]{"admin@fucinema.vn", "an.nv@gmail.com"}) {
                    fx(() -> {
                        ((TextField) stage.getScene().lookup("#tfUsername")).setText(username);
                        ((TextField) stage.getScene().lookup("#pfPassword")).setText(username.startsWith("admin") ? "wrong" : "Customer@123");
                        ((Button) stage.getScene().lookup("#btnLogin")).fire(); return null;
                    });
                    waitFor(() -> dialog() != null);
                    fx(() -> {
                        assertNull(session.getUser());
                        assertTrue(((TextField) stage.getScene().lookup("#pfPassword")).getText().isEmpty());
                        DialogPane dialog = dialog();
                        if (username.startsWith("an")) assertTrue(dialog.getContentText().contains("ADMIN"));
                        ((Button) dialog.lookupButton(ButtonType.OK)).fire(); return null;
                    });
                }
                fx(() -> {
                    ((TextField) stage.getScene().lookup("#tfUsername")).setText("admin@fucinema.vn");
                    ((TextField) stage.getScene().lookup("#pfPassword")).setText("Admin@123");
                    ((Button) stage.getScene().lookup("#btnLogin")).fire(); return null;
                });
                waitFor(() -> stage.getScene().lookup("#btnLogout") != null);
                fx(() -> {
                    assertNotNull(session.getUser());
                    ((Button) stage.getScene().lookup("#btnLogout")).fire();
                    assertNull(session.getUser());
                    assertNotNull(stage.getScene().lookup("#btnLogin"));
                    assertThrows(com.fu.cinema.exception.BusinessException.class, session::requireAdmin);
                    return null;
                });
            } finally { fx(() -> { stage.close(); session.logout(); return null; }); }
        }
    }

    private static DialogPane dialog() {
        return Window.getWindows().stream().filter(Window::isShowing)
                .map(window -> window.getScene().lookup(".dialog-pane"))
                .filter(DialogPane.class::isInstance).map(DialogPane.class::cast).findFirst().orElse(null);
    }
    private static void waitFor(java.util.concurrent.Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12);
        while (System.nanoTime() < deadline) { if (fx(condition)) return; settle(); }
        fail("Timed out waiting for JavaFX state");
    }
    private static void settle() throws Exception {
        var ready = new CountDownLatch(1);
        fx(() -> { var pause = new PauseTransition(Duration.millis(150)); pause.setOnFinished(e -> ready.countDown()); pause.play(); return null; });
        assertTrue(ready.await(3, TimeUnit.SECONDS));
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
