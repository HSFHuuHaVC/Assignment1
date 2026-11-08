package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.DashboardData;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardFacade facade;

    @FXML private VBox root;
    @FXML private Label lblShowing, lblCustomers, lblPending, lblUpdated;
    @FXML private BarChart<String, Number> chartRooms;
    @FXML private Button btnRefresh;

    private Timeline autoRefresh;
    private Task<DashboardData> loading;

    @FXML
    private void initialize() {
        chartRooms.setAnimated(false);
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(60), event -> refresh()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                autoRefresh.stop();
                if (loading != null) {
                    Task<DashboardData> previous = loading;
                    loading = null;
                    previous.cancel();
                    btnRefresh.setDisable(false);
                }
            } else {
                autoRefresh.playFromStart();
                refresh();
            }
        });
    }

    @FXML
    private void refresh() {
        if (loading != null) return;

        Task<DashboardData> task = new Task<>() {
            @Override
            protected DashboardData call() {
                return facade.load();
            }
        };
        loading = task;
        btnRefresh.setDisable(true);
        lblUpdated.setText("Đang tải dữ liệu…");
        task.setOnSucceeded(event -> {
            if (loading != task) return;
            loading = null;
            btnRefresh.setDisable(false);
            render(task.getValue());
        });
        task.setOnFailed(event -> {
            if (loading != task) return;
            loading = null;
            btnRefresh.setDisable(false);
            lblUpdated.setText("Không thể tải dữ liệu. Vui lòng làm mới để thử lại.");
        });
        Thread.startVirtualThread(task);
    }

    private void render(DashboardData data) {
        lblShowing.setText(String.valueOf(data.showingMovies()));
        lblCustomers.setText(String.valueOf(data.customers()));
        lblPending.setText(String.valueOf(data.pendingBookings()));
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Suất chiếu hôm nay");
        data.todayByRoom().forEach(room -> series.getData().add(
                new XYChart.Data<>(room.roomName(), room.count())));
        chartRooms.getData().setAll(List.of(series));
        lblUpdated.setText("Cập nhật lúc " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                + (data.todayByRoom().isEmpty() ? " — Chưa có suất chiếu hôm nay" : ""));
    }
}
