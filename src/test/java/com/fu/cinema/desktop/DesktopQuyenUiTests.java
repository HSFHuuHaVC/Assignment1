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

                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                while (onFx(() -> {
                    TableView<?> table = (TableView<?>) stage.getScene().lookup("#tableMovies");
                    return table.isDisabled() || table.getItems().isEmpty();
                }) && System.nanoTime() < deadline) {
                    Thread.sleep(20);
                }

                onFx(() -> {
                    stage.getScene().getRoot().applyCss();
                    stage.getScene().getRoot().layout();
                    TableView<?> table = (TableView<?>) stage.getScene().lookup("#tableMovies");
                    assertNotNull(table);
                    assertFalse(table.isDisabled(), "Movie table must be enabled after overlapping startup loads");
                    assertFalse(table.getItems().isEmpty());
                    table.getSelectionModel().selectFirst();
                    assertNotNull(table.getSelectionModel().getSelectedItem());
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
            // Preload the options off the FX thread, just like MovieController.
            var genres = context.getBean(com.fu.cinema.desktop.facade.MovieFacade.class).getGenreOptions();
            var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
            onFx(() -> {
                Stage owner = new Stage();
                owner.setScene(new Scene(new javafx.scene.layout.StackPane()));
                owner.show();
                SceneManager.Loaded<MovieDialogController> loaded = manager.load(View.MOVIE_DIALOG);
                Platform.runLater(() -> {
                    try {
                        var dialogRoot = loaded.root().getScene().getRoot();
                        assertNotNull(dialogRoot.lookup("#txtTitle"));
                        assertNotNull(dialogRoot.lookup("#spnDuration"));
                        assertNotNull(dialogRoot.lookup("#btnSave"));
                    } catch (Throwable error) {
                        failure.set(error);
                    } finally {
                        ((Button) loaded.root().getScene().lookup("#btnCancel")).fire();
                    }
                });
                try {
                    assertTrue(loaded.controller().open(owner, new com.fu.cinema.dto.MovieForm(), genres).isEmpty());
                    if (failure.get() != null) throw new AssertionError(failure.get());
                } finally {
                    owner.close();
                    session.logout();
                }
                return null;
            });
        }
    }
}
