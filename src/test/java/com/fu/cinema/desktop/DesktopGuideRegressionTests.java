package com.fu.cinema.desktop;

import com.fu.cinema.desktop.controller.ReportController;
import com.fu.cinema.desktop.facade.ReportFacade;
import com.fu.cinema.desktop.model.ReportData;
import com.fu.cinema.desktop.model.ReportBookingRow;
import com.fu.cinema.desktop.model.RevenueRow;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
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
}
