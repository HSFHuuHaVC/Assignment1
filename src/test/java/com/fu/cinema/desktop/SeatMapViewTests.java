package com.fu.cinema.desktop;

import com.fu.cinema.desktop.component.SeatMapView;
import com.fu.cinema.desktop.model.SeatMapData;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SeatMapViewTests {

    @BeforeAll
    static void initFx() throws Exception {
        var ready = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                ready.countDown();
            });
            assertTrue(ready.await(5, TimeUnit.SECONDS));
        } catch (IllegalStateException ignored) {
            // JavaFX Platform already initialized
        }
    }

    private static <T> T fx(java.util.concurrent.Callable<T> work) throws Exception {
        if (Platform.isFxApplicationThread()) {
            return work.call();
        }
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(5, TimeUnit.SECONDS);
    }

    @Test
    void testSeatMapRenders8x12SeatsAndRespectsSoldSeats() throws Exception {
        fx(() -> {
            SeatMapView view = new SeatMapView();
            List<String> rowLabels = List.of("A", "B", "C", "D", "E", "F", "G", "H"); // 8 rows
            int seatsPerRow = 12; // 12 seats -> total 96 seats
            Set<String> sold = Set.of("A1", "A2", "B5");

            SeatMapData data = new SeatMapData(1L, "Phim Test", "Room 01", LocalDateTime.now(),
                    BigDecimal.valueOf(100000), rowLabels, seatsPerRow, sold);

            view.setData(data);

            // getSelectedSeats ban đầu phải rỗng
            assertTrue(view.getSelectedSeats().isEmpty());

            // Tìm GridPane trong view
            GridPane grid = (GridPane) view.getChildren().stream()
                    .filter(GridPane.class::isInstance)
                    .findFirst()
                    .orElseThrow();

            List<ToggleButton> seats = new ArrayList<>();
            List<Label> labels = new ArrayList<>();
            for (Node node : grid.getChildren()) {
                if (node instanceof ToggleButton btn) {
                    seats.add(btn);
                    String seatCode = btn.getTooltip().getText();
                    if (sold.contains(seatCode)) {
                        assertTrue(btn.isDisable(), "Ghế đã bán " + seatCode + " phải bị disable");
                    } else {
                        assertFalse(btn.isDisable(), "Ghế trống " + seatCode + " phải được chọn");
                    }
                } else if (node instanceof Label lbl) {
                    labels.add(lbl);
                }
            }

            assertEquals(8, labels.size(), "Phải có 8 nhãn hàng A-H");
            assertEquals(96, seats.size(), "Phòng 8x12 phải render đủ 96 ghế");
            return null;
        });
    }

    @Test
    void testSelectionAndClearData() throws Exception {
        fx(() -> {
            SeatMapView view = new SeatMapView();
            List<String> rowLabels = List.of("A", "B");
            int seatsPerRow = 10;
            SeatMapData data = new SeatMapData(1L, "Phim Test", "Room 01", LocalDateTime.now(),
                    BigDecimal.valueOf(100000), rowLabels, seatsPerRow, Set.of());

            view.setData(data);

            GridPane grid = (GridPane) view.getChildren().stream()
                    .filter(GridPane.class::isInstance)
                    .findFirst()
                    .orElseThrow();

            List<ToggleButton> seats = grid.getChildren().stream()
                    .filter(ToggleButton.class::isInstance)
                    .map(ToggleButton.class::cast)
                    .toList();

            // Chọn lần lượt 8 ghế (tối đa theo BR-10)
            for (int i = 0; i < SeatMapView.MAX_SEATS; i++) {
                seats.get(i).setSelected(true);
            }
            assertEquals(8, view.getSelectedSeats().size(), "Danh sách ghế chọn phải có 8 ghế");
            assertTrue(view.getSelectedSeats().contains("A1"));
            assertTrue(view.getSelectedSeats().contains("A8"));

            // Bỏ chọn ghế đầu tiên
            seats.get(0).setSelected(false);
            assertEquals(7, view.getSelectedSeats().size());
            assertFalse(view.getSelectedSeats().contains("A1"));

            // Xoá dữ liệu (setData = null)
            view.setData(null);
            assertTrue(view.getSelectedSeats().isEmpty(), "Khi setData null, danh sách chọn phải được reset");
            assertEquals(0, grid.getChildren().size(), "Khi setData null, grid ghế phải trống");

            return null;
        });
    }
}

