package com.example.controller;

import com.example.App;
import com.example.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private StackPane contentPane;
    @FXML private Label     pageTitle;
    @FXML private Label     userLabel;

    @FXML private Button btnDashboard;
    @FXML private Button btnBooks;
    @FXML private Button btnCategories;
    @FXML private Button btnMembers;
    @FXML private Button btnBorrow;
    @FXML private Button btnFines;
    @FXML private Button btnReports;

    private final Map<String, String> pageTitles = new HashMap<>();
    private Button activeButton;

    @FXML
    public void initialize() {
        pageTitles.put("dashboard",  "Tổng quan");
        pageTitles.put("book",       "Quản lý sách");
        pageTitles.put("category",   "Danh mục sách");
        pageTitles.put("member",     "Đọc giả");
        pageTitles.put("borrow",     "Mượn / trả sách");
        pageTitles.put("fine",       "Phí phạt");
        pageTitles.put("report",     "Báo cáo");

        var user = AuthService.getInstance().getCurrentUser();
        if (user != null) {
            userLabel.setText(user.getFullName() + "\n" + user.getRole().name());
        }

        activeButton = btnDashboard;
        loadPage("dashboard");
    }

    @FXML
    private void navigate(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String page = (String) clicked.getUserData();
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        clicked.getStyleClass().add("active");
        activeButton = clicked;
        pageTitle.setText(pageTitles.getOrDefault(page, page));
        loadPage(page);
    }

    private void loadPage(String page) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/example/" + page + ".fxml"));
            Node node = loader.load();
            contentPane.getChildren().setAll(node);
        } catch (Exception e) {
            // Unwrap to find the true root cause
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            String msg = root.getClass().getSimpleName() + ": " + root.getMessage();
            Label err = new Label("Lỗi tải trang [" + page + "]: " + msg);
            err.setStyle("-fx-text-fill: red; -fx-wrap-text: true;");
            err.setWrapText(true);
            contentPane.getChildren().setAll(err);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Bạn có chắc muốn đăng xuất?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Xác nhận đăng xuất");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(type -> {
            if (type == ButtonType.YES) {
                AuthService.getInstance().logout();
                try { App.setRoot("login"); } catch (IOException e) { e.printStackTrace(); }
            }
        });
    }
}
