package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class MainController {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss  dd/MM/yyyy");
    private final SceneManager sceneManager;
    private final DesktopSession session;
    @FXML private BorderPane root;
    @FXML private Label lblUser;
    @FXML private Label lblClock;
    private Timeline clock;

    @FXML private void initialize() {
        lblUser.textProperty().bind(Bindings.concat("Nhân viên: ", session.displayNameProperty()));
        updateClock();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateClock()));
        clock.setCycleCount(Animation.INDEFINITE);
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) clock.stop();
            else clock.play();
        });
        onDashboard();
    }
    private void updateClock() { lblClock.setText(LocalDateTime.now().format(CLOCK)); }
    @FXML private void onDashboard() { sceneManager.showIn(root, View.DASHBOARD); }
    @FXML private void onCounterSale() { sceneManager.showIn(root, View.COUNTER_SALE); }
    @FXML private void onBooking() { sceneManager.showIn(root, View.BOOKING); }
    @FXML private void onGenre() { sceneManager.showIn(root, View.GENRE); }
    @FXML private void onMovie() { sceneManager.showIn(root, View.MOVIE); }
    @FXML private void onSchedule() { sceneManager.showIn(root, View.SCHEDULE); }
    @FXML private void onReport() { sceneManager.showIn(root, View.REPORT); }
    @FXML private void onLogout() {
        clock.stop();
        session.logout();
        sceneManager.switchTo(View.LOGIN);
    }
}
