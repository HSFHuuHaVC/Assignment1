package com.fu.cinema.desktop;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Runs in a child JVM to detect leaked non-daemon threads after the window closes. */
public final class DesktopLifecycleProbe {
    private DesktopLifecycleProbe() { }
    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        Application.launch(Probe.class, "--spring.profiles.active=test",
                "--spring.datasource.url=jdbc:h2:mem:lifecycle;MODE=MSSQLServer");
        System.out.println("DESKTOP_STOP_COMPLETED");
    }
    public static class Probe extends CinemaFxApp {
        @Override public void start(Stage stage) {
            super.start(stage);
            PauseTransition closeWindow = new PauseTransition(Duration.millis(300));
            closeWindow.setOnFinished(event -> stage.close());
            closeWindow.play();
        }
    }
}
