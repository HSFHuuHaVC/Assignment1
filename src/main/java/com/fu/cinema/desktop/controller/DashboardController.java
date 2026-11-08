package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.DashboardFacade;
import com.fu.cinema.desktop.model.DashboardData;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
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
    private final FxAsync fxAsync;
    private final BooleanProperty refreshing = new SimpleBooleanProperty();

    @FXML private VBox root;
    @FXML private Label lblShowing, lblCustomers, lblPending, lblUpdated;
    @FXML private BarChart<String, Number> chartRooms;
    @FXML private Button btnRefresh;
    @FXML private ProgressIndicator progress;

    private Timeline autoRefresh;
    private Task<DashboardData> loading;
    private long loadVersion;

    @FXML
    private void initialize() {
        chartRooms.setAnimated(false);
        btnRefresh.disableProperty().bind(refreshing);
        progress.visibleProperty().bind(refreshing);
        progress.managedProperty().bind(progress.visibleProperty());
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(60), event -> refresh()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                autoRefresh.stop();
                loadVersion++;
                if (loading != null) {
                    Task<DashboardData> previous = loading;
                    loading = null;
                    previous.cancel();
                }
                refreshing.set(false);
            } else {
                autoRefresh.playFromStart();
                refresh();
            }
        });
    }

    @FXML
    private void refresh() {
        if (refreshing.get()) return;

        long version = ++loadVersion;
        refreshing.set(true);
        lblUpdated.setText("Đang tải dữ liệu…");
        Task<DashboardData> task = fxAsync.run(facade::load, data -> {
            if (loadVersion != version) return;
            loading = null;
            refreshing.set(false);
            render(data);
        }, error -> {
            if (loadVersion != version) return;
            loading = null;
            refreshing.set(false);
            lblUpdated.setText(AlertUtil.describe(error).text() + " Bấm Làm mới để thử lại.");
        }, null);
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_CANCELLED, event -> {
            if (loadVersion != version) return;
            loading = null;
            refreshing.set(false);
            lblUpdated.setText("Đã dừng tải dữ liệu.");
        });
        if (refreshing.get() && loadVersion == version) loading = task;
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
                + (data.todayByRoom().stream().noneMatch(room -> room.count() > 0)
                ? " — Chưa có suất chiếu hôm nay" : ""));
    }
}
