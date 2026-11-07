package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.GenreController;
import com.fu.cinema.desktop.controller.MovieController;
import com.fu.cinema.desktop.controller.MovieDialogController;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.dto.SessionUser;
import com.fu.cinema.enums.Role;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "desktop.ui.tests", matches = "true")
class DesktopQuyenUiTests {

    @BeforeAll
    static void initFx() throws Exception {
        var ready = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                ready.countDown();
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS));
        } catch (IllegalStateException alreadyStarted) {
            // Already started by another test class
        }
    }

    @AfterAll
    static void tearDown() {
        // Keep FX platform open for other tests or exit cleanly
    }

    private static <T> T onFx(java.util.concurrent.Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<>(callable);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private static void settle() throws InterruptedException {
        Thread.sleep(400);
    }

    private static void snapshot(Stage stage, String name) throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of("docs/images");
        java.nio.file.Files.createDirectories(dir);
        javafx.scene.image.WritableImage image = stage.getScene().snapshot(null);
        java.awt.image.BufferedImage buffered = new java.awt.image.BufferedImage(
                (int) image.getWidth(), (int) image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < (int) image.getWidth(); x++) {
            for (int y = 0; y < (int) image.getHeight(); y++) {
                buffered.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        javax.imageio.ImageIO.write(buffered, "png", dir.resolve("desktop-" + name + ".png").toFile());
    }

    @Test
    void genreFxmlLoadsAndValidatesFields() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(com.fu.cinema.FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE).headless(false).profiles("test", "desktop")
                .run("--spring.profiles.active=test",
                        "--spring.datasource.url=jdbc:h2:mem:desktopUiGenre;MODE=MSSQLServer")) {

            var manager = context.getBean(SceneManager.class);
            var session = context.getBean(DesktopSession.class);

            Stage stage = onFx(() -> {
                session.login(new SessionUser(1L, null, "Administrator", Role.ADMIN));
                Stage window = new Stage();
                manager.init(window);
                manager.switchTo(View.MAIN);
                return window;
            });

            try {
                // Navigate to Genre
                onFx(() -> {
                    SceneManager.Loaded<GenreController> loaded = manager.load(View.GENRE);
                    assertNotNull(loaded.root());
                    assertNotNull(loaded.controller());
                    stage.getScene().setRoot(loaded.root());
                    stage.setTitle(View.GENRE.title());
                    return null;
                });
                settle();

                onFx(() -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    TableView<?> table = (TableView<?>) stage.getScene().lookup("#tableGenres");
                    assertNotNull(table);
                    TextField txtName = (TextField) stage.getScene().lookup("#txtName");
                    assertNotNull(txtName);
                    Button btnSave = (Button) stage.getScene().lookup("#btnSave");
                    assertNotNull(btnSave);
                    Button btnDelete = (Button) stage.getScene().lookup("#btnDelete");
                    assertNotNull(btnDelete);
                    assertTrue(btnDelete.isDisable(), "Delete button must be disabled initially without selection");
                    snapshot(stage, "genre");
                    return null;
                });
            } finally {
                onFx(() -> {
                    stage.close();
                    session.logout();
                    return null;
                });
            }
        }
    }

    @Test
    void movieFxmlLoadsAndShowsFiltersAndTable() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(com.fu.cinema.FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE).headless(false).profiles("test", "desktop")
                .run("--spring.profiles.active=test",
                        "--spring.datasource.url=jdbc:h2:mem:desktopUiMovie;MODE=MSSQLServer")) {

            var manager = context.getBean(SceneManager.class);
            var session = context.getBean(DesktopSession.class);

            Stage stage = onFx(() -> {
                session.login(new SessionUser(1L, null, "Administrator", Role.ADMIN));
                Stage window = new Stage();
                manager.init(window);
                manager.switchTo(View.MAIN);
                return window;
            });

            try {
                onFx(() -> {
                    SceneManager.Loaded<MovieController> loaded = manager.load(View.MOVIE);
                    assertNotNull(loaded.root());
                    assertNotNull(loaded.controller());
                    stage.getScene().setRoot(loaded.root());
                    stage.setTitle(View.MOVIE.title());
                    return null;
                });
                settle();

                onFx(() -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    TableView<?> table = (TableView<?>) stage.getScene().lookup("#tableMovies");
                    assertNotNull(table);
                    TextField txtKw = (TextField) stage.getScene().lookup("#txtKeyword");
                    assertNotNull(txtKw);
                    ComboBox<?> cmbGenre = (ComboBox<?>) stage.getScene().lookup("#cmbGenre");
                    assertNotNull(cmbGenre);
                    ComboBox<?> cmbStatus = (ComboBox<?>) stage.getScene().lookup("#cmbStatus");
                    assertNotNull(cmbStatus);
                    Pagination pagination = (Pagination) stage.getScene().lookup("#pagination");
                    assertNotNull(pagination);
                    snapshot(stage, "movie");
                    return null;
                });
            } finally {
                onFx(() -> {
                    stage.close();
                    session.logout();
                    return null;
                });
            }
        }
    }

    @Test
    void movieDialogFxmlValidatesAndHandlesCancel() throws Exception {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(com.fu.cinema.FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE).headless(false).profiles("test", "desktop")
                .run("--spring.profiles.active=test",
                        "--spring.datasource.url=jdbc:h2:mem:desktopUiDialog;MODE=MSSQLServer")) {

            var manager = context.getBean(SceneManager.class);
            var session = context.getBean(DesktopSession.class);

            Stage stage = onFx(() -> {
                session.login(new SessionUser(1L, null, "Administrator", Role.ADMIN));
                Stage window = new Stage();
                manager.init(window);

                SceneManager.Loaded<MovieDialogController> loaded = manager.load(View.MOVIE_DIALOG);
                window.setScene(new Scene(loaded.root()));
                loaded.controller().init(window, null);
                window.show();
                return window;
            });

            try {
                settle();
                onFx(() -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    snapshot(stage, "movie-dialog");

                    TextField txtTitle = (TextField) stage.getScene().lookup("#txtTitle");
                    assertNotNull(txtTitle);
                    Spinner<?> spnDuration = (Spinner<?>) stage.getScene().lookup("#spnDuration");
                    assertNotNull(spnDuration);
                    Button btnCancel = (Button) stage.getScene().lookup("#btnCancel");
                    assertNotNull(btnCancel);
                    Button btnSave = (Button) stage.getScene().lookup("#btnSave");
                    assertNotNull(btnSave);

                    // Test Cancel closes stage
                    btnCancel.fire();
                    assertFalse(stage.isShowing(), "Cancel must close the dialog");
                    return null;
                });
            } finally {
                onFx(() -> {
                    if (stage.isShowing()) stage.close();
                    session.logout();
                    return null;
                });
            }
        }
    }
}
