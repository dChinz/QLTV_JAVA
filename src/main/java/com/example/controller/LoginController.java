package com.example.controller;

import com.example.App;
import com.example.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;
    @FXML private Region        errorSpacer;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Vui lòng nhập tên đăng nhập và mật khẩu.");
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
                passwordField.clear();
            }
        } catch (Exception e) {
            // Unwrap to find the true root cause
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
}
