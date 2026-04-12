package com.example.controller;

import com.example.App;
import com.example.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField     passwordVisible;
    @FXML private HBox          passwordWrapper;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;
    @FXML private Region        errorSpacer;

    @FXML private Button        togglePasswordBtn;

    private boolean showingPassword = false;
    private FontIcon iconEye;
    private FontIcon iconEyeSlash;

    @FXML
    public void initialize() {
        iconEye      = new FontIcon("fas-eye");
        iconEyeSlash = new FontIcon("fas-eye-slash");
        iconEye.setIconSize(14);
        iconEyeSlash.setIconSize(14);
        // setStyle() replaces the entire inline style and removes -fx-font-family that FontIcon sets.
        Color iconMuted = Color.web("#64748B");
        iconEye.setIconColor(iconMuted);
        iconEyeSlash.setIconColor(iconMuted);
        togglePasswordBtn.setGraphic(iconEye);
        passwordVisible.textProperty().bindBidirectional(passwordField.textProperty());

        // Show focused ring on the wrapper HBox
        passwordField.focusedProperty().addListener((obs, o, focused) -> updateWrapperFocus(focused));
        passwordVisible.focusedProperty().addListener((obs, o, focused) -> updateWrapperFocus(focused));

        // Clear validation highlights on input
        usernameField.textProperty().addListener((obs, o, n) -> {
            usernameField.setStyle("");
            clearError();
        });
        passwordField.textProperty().addListener((obs, o, n) -> {
            passwordWrapper.setStyle("");
            clearError();
        });
    }

    private void updateWrapperFocus(boolean focused) {
        if (focused) {
            passwordWrapper.setStyle(
                "-fx-border-color: #4F46E5; -fx-effect: dropshadow(gaussian, rgba(79,70,229,0.2), 6, 0, 0, 0);");
        } else {
            // Restore default; error style takes priority if previously set by validation
            if (!passwordWrapper.getStyle().contains("#E11D48")) {
                passwordWrapper.setStyle("");
            }
        }
    }

    @FXML
    private void togglePassword() {
        showingPassword = !showingPassword;
        if (showingPassword) {
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            passwordVisible.setVisible(true);
            passwordVisible.setManaged(true);
            passwordVisible.requestFocus();
            passwordVisible.positionCaret(passwordVisible.getLength());
            togglePasswordBtn.setGraphic(iconEyeSlash);
        } else {
            passwordVisible.setVisible(false);
            passwordVisible.setManaged(false);
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordField.requestFocus();
            passwordField.positionCaret(passwordField.getLength());
            togglePasswordBtn.setGraphic(iconEye);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        boolean valid = true;
        if (username.isEmpty()) {
            usernameField.setStyle("-fx-border-color: #E11D48;");
            valid = false;
        }
        if (password.isEmpty()) {
            passwordWrapper.setStyle("-fx-border-color: #E11D48;");
            valid = false;
        }
        if (!valid) {
            showError("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Đang đăng nhập...");

        try {
            boolean ok = AuthService.getInstance().login(username, password);
            if (ok) {
                App.setRoot("main");
            } else {
                showError("Tên đăng nhập hoặc mật khẩu không đúng.");
                passwordWrapper.setStyle("-fx-border-color: #E11D48;");
                passwordField.clear();
            }
        } catch (Exception e) {
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            showError("Lỗi: " + root.getMessage());
            e.printStackTrace();
        } finally {
            loginButton.setDisable(false);
            loginButton.setText("Đăng Nhập");
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorSpacer.setVisible(true);
        errorSpacer.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        errorSpacer.setVisible(false);
        errorSpacer.setManaged(false);
    }
}
