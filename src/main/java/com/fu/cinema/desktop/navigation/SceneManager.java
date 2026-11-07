package com.fu.cinema.desktop.navigation;

import com.fu.cinema.desktop.session.DesktopSession;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SceneManager {
    private final ApplicationContext context;
    private final DesktopSession session;
    private Stage stage;
    private Scene scene;

    public void init(Stage stage) {
        this.stage = stage;
        scene = new Scene(new StackPane(), 1200, 720);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/desktop/css/desktop.css")).toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
    }

    public void switchTo(View view) {
        if (view != View.LOGIN) session.requireAdmin();
        scene.setRoot(load(view).root());
        stage.setTitle(view.title());
        stage.show();
    }

    public void showIn(BorderPane host, View view) {
        session.requireAdmin();
        if (getClass().getResource(view.path()) == null) {
            // Feature modules are owned by the other members; do not create stub FXML/controllers.
            VBox unavailable = new VBox(12, new Label(view.title()), new Label("Chức năng chưa sẵn sàng."));
            unavailable.getStyleClass().add("page");
            host.setCenter(unavailable);
            return;
        }
        host.setCenter(load(view).root());
    }

    public <C> Loaded<C> load(View view) {
        var resource = Objects.requireNonNull(getClass().getResource(view.path()), "Không tìm thấy " + view.path());
        FXMLLoader loader = new FXMLLoader(resource);
        loader.setControllerFactory(context::getBean);
        try {
            return new Loaded<>(loader.load(), loader.getController());
        } catch (IOException ex) {
            throw new UncheckedIOException("Không nạp được " + view.path(), ex);
        }
    }

    public Stage getStage() { return stage; }
    public record Loaded<C>(Parent root, C controller) { }
}
