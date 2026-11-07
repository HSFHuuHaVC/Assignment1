package com.fu.cinema.desktop;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StageInitializer implements ApplicationListener<StageReadyEvent> {
    @Override
    public void onApplicationEvent(StageReadyEvent event) {
        var stage = event.getStage();
        stage.setTitle("FU Cinema Desktop");
        stage.setScene(new Scene(new Label("FU Cinema Desktop"), 800, 600));
        stage.show();
    }
}
