package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.ReportController;
import com.fu.cinema.desktop.controller.MovieController;
import com.fu.cinema.desktop.controller.MovieDialogController;
import com.fu.cinema.desktop.facade.MovieFacade;
import com.fu.cinema.desktop.model.GenreOption;
import com.fu.cinema.desktop.model.MovieItem;
import com.fu.cinema.desktop.model.PageResult;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.util.FxValidator;
import com.fu.cinema.dto.MovieForm;
import com.fu.cinema.enums.AgeRating;
import com.fu.cinema.enums.MovieStatus;
import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named = "desktop.ui.tests", matches = "true")
class DesktopGuideRegressionTests {
    @BeforeAll static void startFx() throws Exception {
        var ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @AfterAll static void stopFx() { Platform.exit(); }

    @Test void exportButtonWritesCsvThroughRealAsyncWithAnUnboundBusyControl(@TempDir Path dir) throws Exception {
        var facade = mock(ReportFacade.class);
        var report = new ReportData(LocalDate.now(), LocalDate.now(),
                List.of(new RevenueRow("Phim, \"A\"", 1, new BigDecimal("90000"))),
                List.of(new ReportBookingRow(1L, LocalDate.now().atStartOfDay(), "Khách A",
                        "Phim, \"A\"", new BigDecimal("90000"))), 1, 1, new BigDecimal("90000"));
        when(facade.generate(any(), any())).thenReturn(report);
        var async = spy(new FxAsync());
        var exported = new CountDownLatch(1);
        // Keep the production Task, busy handling and CSV writer; avoid a modal success alert in the test.
        doAnswer(call -> {
            Runnable work = call.getArgument(0);
            javafx.scene.Node busy = call.getArgument(2);
            return async.run(() -> { work.run(); return null; }, ignored -> exported.countDown(), busy);
        }).when(async).runVoid(any(), any(), any());
        Stage window = fx(() -> {
            var loader = new FXMLLoader(getClass().getResource("/desktop/fxml/report.fxml"));
            loader.setControllerFactory(type -> new ReportController(facade, async));
            var stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            stage.show();
            return stage;
        });
        try {
            Button export = fx(() -> (Button) window.getScene().lookup("#btnExport"));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (fx(export::isDisabled) && System.nanoTime() < deadline) Thread.sleep(20);
            assertFalse(fx(export::isDisabled));
            Path csv = dir.resolve("report.csv");
            fx(() -> {
                try (var chooser = mockConstruction(FileChooser.class, (mock, context) -> {
                    when(mock.getExtensionFilters()).thenReturn(javafx.collections.FXCollections.observableArrayList());
                    when(mock.showSaveDialog(any())).thenReturn(csv.toFile());
                })) {
                    export.fire();
                    assertEquals(1, chooser.constructed().size());
                }
                return null;
            });
            assertTrue(exported.await(10, TimeUnit.SECONDS), "Export must reach the CSV writer");
            String contents = Files.readString(csv);
            assertTrue(contents.startsWith("\uFEFF"));
            assertTrue(contents.contains("\"Phim, \"\"A\"\"\""));
            fx(() -> { assertFalse(window.getScene().lookup("#btnGenerate").isDisabled()); return null; });
        } finally {
            fx(() -> { window.close(); return null; });
            async.destroy();
        }
    }

    private static <T> T fx(Callable<T> callable) throws Exception {
        var task = new FutureTask<T>(callable);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    @Test void movieDialogKeepsInvalidDurationOpenAndReturnsTheValidatedForm() throws Exception {
        try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
            fx(() -> {
                var loader = new FXMLLoader(getClass().getResource("/desktop/fxml/movie-dialog.fxml"));
                loader.setControllerFactory(type -> new MovieDialogController(new FxValidator(validators.getValidator())));
                javafx.scene.Parent root = loader.load();
                MovieDialogController controller = loader.getController();
                var owner = new Stage();
                owner.setScene(new Scene(new javafx.scene.layout.StackPane()));
                owner.getScene().getStylesheets().add(getClass().getResource("/desktop/css/desktop.css").toExternalForm());
                owner.show();
                Platform.runLater(() -> {
                    try {
                        Spinner<?> duration = (Spinner<?>) root.lookup("#spnDuration");
                        Button save = (Button) root.getScene().lookup("#btnSave");
                        for (int invalid : new int[]{20, 301}) {
                            duration.getEditor().setText(String.valueOf(invalid));
                            save.fire();
                            assertTrue(root.getScene().getWindow().isShowing());
                            assertEquals(invalid, duration.getValue());
                            assertTrue(duration.getPseudoClassStates().contains(javafx.css.PseudoClass.getPseudoClass("error")));
                            assertNotNull(duration.getTooltip());
                        }
                        snapshot(root.getScene(), "movie-duration-validation");
                        duration.getEditor().setText("120");
                        save.fire();
                    } catch (Throwable error) {
                        failure.set(error);
                        ((Button) root.getScene().lookup("#btnCancel")).fire();
                    }
                });
                try {
                    var result = controller.open(owner, movieForm(), List.of(new GenreOption(1L, "Hành động")));
                    if (failure.get() != null) throw new AssertionError(failure.get());
                    assertTrue(result.isPresent());
                    assertEquals(42L, result.get().getMovieId());
                    assertEquals(120, result.get().getDuration());
                    assertEquals(1L, result.get().getGenreId());
                } finally { owner.close(); }
                return null;
            });
        }
    }

    @Test void editingMovieLoadsFormOffFxThreadAndCancelDoesNotSave() throws Exception {
        var requested = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var facade = mock(MovieFacade.class);
        when(facade.getGenreOptions()).thenAnswer(call -> {
            assertFalse(Platform.isFxApplicationThread());
            return List.of(new GenreOption(1L, "Hành động"));
        });
        when(facade.getMovieForm(42L)).thenAnswer(call -> {
            assertFalse(Platform.isFxApplicationThread());
            requested.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            return movieForm();
        });
        when(facade.searchMovies(any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 1, 0));
        var async = new FxAsync();
        var cancelled = new CountDownLatch(1);
        var failure = new java.util.concurrent.atomic.AtomicReference<Throwable>();
        try (var validators = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var manager = mock(SceneManager.class);
            when(manager.<MovieDialogController>load(View.MOVIE_DIALOG)).thenAnswer(call -> {
                assertTrue(Platform.isFxApplicationThread());
                var loader = new FXMLLoader(getClass().getResource("/desktop/fxml/movie-dialog.fxml"));
                loader.setControllerFactory(type -> new MovieDialogController(new FxValidator(validators.getValidator())));
                javafx.scene.Parent root = loader.load();
                Platform.runLater(() -> {
                    try {
                        assertEquals("Phim kiểm thử", ((javafx.scene.control.TextField) root.lookup("#txtTitle")).getText());
                    } catch (Throwable error) { failure.set(error); }
                    finally {
                        ((Button) root.getScene().lookup("#btnCancel")).fire();
                        cancelled.countDown();
                    }
                });
                return new SceneManager.Loaded<>(root, loader.getController());
            });
            Stage owner = fx(() -> {
                var loader = new FXMLLoader(getClass().getResource("/desktop/fxml/movie.fxml"));
                loader.setControllerFactory(type -> new MovieController(facade, async, manager));
                var stage = new Stage();
                stage.setScene(new Scene(loader.load()));
                stage.show();
                MovieController controller = loader.getController();
                var selected = mock(MovieItem.class);
                when(selected.movieId()).thenReturn(42L);
                controller.onEditMovie(selected);
                return stage;
            });
            try {
                assertTrue(requested.await(10, TimeUnit.SECONDS));
                assertTrue(fx(Platform::isFxApplicationThread), "FX thread stays responsive while loading the form");
                release.countDown();
                assertTrue(cancelled.await(10, TimeUnit.SECONDS));
                fx(() -> null); // Let the nested dialog callback finish.
                if (failure.get() != null) throw new AssertionError(failure.get());
                verify(facade, never()).createMovie(any());
                verify(facade, never()).updateMovie(any(), any());
            } finally { fx(() -> { owner.close(); return null; }); }
        } finally { release.countDown(); async.destroy(); }
    }

    private static MovieForm movieForm() {
        var form = new MovieForm();
        form.setMovieId(42L);
        form.setTitle("Phim kiểm thử");
        form.setDirector("Đạo diễn A");
        form.setDuration(120);
        form.setReleaseDate(LocalDate.now());
        form.setAgeRating(AgeRating.P);
        form.setGenreId(1L);
        form.setStatus(MovieStatus.SHOWING);
        form.setPosterUrl("/images/posters/test.jpg");
        form.setDescription("Mô tả phim kiểm thử");
        return form;
    }

    private static void snapshot(Scene scene, String name) throws Exception {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        var image = scene.snapshot(null);
        var buffered = new java.awt.image.BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                buffered.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        Path directory = Path.of("target", "guide-screenshots");
        Files.createDirectories(directory);
        javax.imageio.ImageIO.write(buffered, "png", directory.resolve(name + ".png").toFile());
    }
}
