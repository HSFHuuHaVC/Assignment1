package com.fu.cinema.desktop;

import com.fu.cinema.FuCinemaTicketSystemApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class CinemaFxApp extends Application {
    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(FuCinemaTicketSystemApplication.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .profiles("desktop")
                .run(getParameters().getRaw().toArray(String[]::new));
    }

    @Override
    public void start(Stage stage) {
        try {
            context.publishEvent(new StageReadyEvent(stage));
        } catch (RuntimeException ex) {
            context.close();
            Platform.exit();
            throw ex;
        }
    }

    @Override
    public void stop() {
        if (context != null) context.close();
    }
}
