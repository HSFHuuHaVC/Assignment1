package com.fu.cinema.desktop.controller;

import com.fu.cinema.desktop.facade.AuthFacade;
import com.fu.cinema.desktop.navigation.SceneManager;
import com.fu.cinema.desktop.navigation.View;
import com.fu.cinema.desktop.session.DesktopSession;
import com.fu.cinema.desktop.util.AlertUtil;
import com.fu.cinema.desktop.util.FxAsync;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@RequiredArgsConstructor
public class LoginController {
    private final AuthFacade authFacade;
    private final DesktopSession session;
    private final SceneManager sceneManager;
    private final FxAsync fxAsync;
    @FXML private VBox form;
    @FXML private TextField tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Button btnLogin;
    @FXML private ProgressIndicator progress;
    private Task<?> pending;

    @FXML private void initialize() {
        btnLogin.disableProperty().bind(Bindings.createBooleanBinding(
                () -> tfUsername.getText().isBlank() || pfPassword.getText().isEmpty(),
                tfUsername.textProperty(), pfPassword.textProperty()));
        progress.managedProperty().bind(progress.visibleProperty());
        form.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (oldScene != null && newScene == null && pending != null) pending.cancel(true);
        });
        Platform.runLater(tfUsername::requestFocus);
    }

    @FXML private void onLogin() {
        if (btnLogin.isDisabled() || form.isDisable()) return;
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText();
        progress.setVisible(true);
        pending = fxAsync.run(() -> authFacade.loginAdmin(username, password), user -> {
            progress.setVisible(false);
            pfPassword.clear();
            session.login(user);
            sceneManager.switchTo(View.MAIN);
        }, error -> {
            progress.setVisible(false);
            pfPassword.clear();
            AlertUtil.showError(error);
            pfPassword.requestFocus();
        }, form);
    }
}
