package com.example.controller;

import com.example.App;
import com.example.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import org.kordamp.ikonli.javafx.FontIcon;

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
    private void handleChangePassword() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        dialog.setHeaderText(null);

        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        // Password fields with show/hide wrappers
        PasswordField oldPwField     = new PasswordField();
        PasswordField newPwField     = new PasswordField();
        PasswordField confirmPwField = new PasswordField();
        TextField oldPwVisible       = new TextField();
        TextField newPwVisible       = new TextField();
        TextField confirmPwVisible   = new TextField();

        oldPwField.setPrefWidth(220);    newPwField.setPrefWidth(220);    confirmPwField.setPrefWidth(220);
        oldPwVisible.setPrefWidth(220);  newPwVisible.setPrefWidth(220);  confirmPwVisible.setPrefWidth(220);
        oldPwVisible.setManaged(false);  newPwVisible.setManaged(false);  confirmPwVisible.setManaged(false);
        oldPwVisible.setVisible(false);  newPwVisible.setVisible(false);  confirmPwVisible.setVisible(false);

        oldPwField.textProperty().bindBidirectional(oldPwVisible.textProperty());
        newPwField.textProperty().bindBidirectional(newPwVisible.textProperty());
        confirmPwField.textProperty().bindBidirectional(confirmPwVisible.textProperty());

        Button toggleOld     = new Button(); Button toggleNew = new Button(); Button toggleConfirm = new Button();
        FontIcon iOld = new FontIcon("fas-eye"); iOld.setIconSize(14);
        FontIcon iNew = new FontIcon("fas-eye"); iNew.setIconSize(14);
        FontIcon iCfm = new FontIcon("fas-eye"); iCfm.setIconSize(14);
        toggleOld.setGraphic(iOld); toggleNew.setGraphic(iNew); toggleConfirm.setGraphic(iCfm);
        for (Button btn : new Button[]{toggleOld, toggleNew, toggleConfirm}) {
            btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14px; -fx-padding: 0 4 0 4;");
        }

        HBox oldBox     = new HBox(0, oldPwField,     oldPwVisible,     toggleOld);
        HBox newBox     = new HBox(0, newPwField,     newPwVisible,     toggleNew);
        HBox confirmBox = new HBox(0, confirmPwField, confirmPwVisible, toggleConfirm);

        toggleOld.setOnAction(e -> {
            boolean show = !oldPwVisible.isManaged();
            oldPwField.setManaged(!show); oldPwField.setVisible(!show);
            oldPwVisible.setManaged(show); oldPwVisible.setVisible(show);
            iOld.setIconLiteral(show ? "fas-eye-slash" : "fas-eye");
        });
        toggleNew.setOnAction(e -> {
            boolean show = !newPwVisible.isManaged();
            newPwField.setManaged(!show); newPwField.setVisible(!show);
            newPwVisible.setManaged(show); newPwVisible.setVisible(show);
            iNew.setIconLiteral(show ? "fas-eye-slash" : "fas-eye");
        });
        toggleConfirm.setOnAction(e -> {
            boolean show = !confirmPwVisible.isManaged();
            confirmPwField.setManaged(!show); confirmPwField.setVisible(!show);
            confirmPwVisible.setManaged(show); confirmPwVisible.setVisible(show);
            iCfm.setIconLiteral(show ? "fas-eye-slash" : "fas-eye");
        });

        grid.add(new Label("Mật khẩu hiện tại *:"), 0, 0); grid.add(oldBox,     1, 0);
        grid.add(new Label("Mật khẩu mới *:"),       0, 1); grid.add(newBox,     1, 1);
        grid.add(new Label("Xác nhận mật khẩu *:"),  0, 2); grid.add(confirmBox, 1, 2);

        Label formError = new Label();
        formError.setVisible(false); formError.setManaged(false);
        formError.setWrapText(true); formError.setMaxWidth(340);
        formError.setStyle("-fx-text-fill: #E11D48; -fx-font-size: 12px;");
        grid.add(formError, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(450);

        boolean[] changed = {false};

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            String oldPw   = oldPwField.getText();
            String newPw   = newPwField.getText();
            String confirm = confirmPwField.getText();
            if (oldPw.isBlank() || newPw.isBlank() || confirm.isBlank()) {
                formError.setText("• Vui lòng điền đầy đủ tất cả các trường.");
                formError.setVisible(true); formError.setManaged(true);
                evt.consume(); return;
            }
            if (newPw.length() < 6) {
                formError.setText("• Mật khẩu mới phải có ít nhất 6 ký tự.");
                formError.setVisible(true); formError.setManaged(true);
                evt.consume(); return;
            }
            if (!newPw.equals(confirm)) {
                formError.setText("• Mật khẩu xác nhận không khớp.");
                formError.setVisible(true); formError.setManaged(true);
                evt.consume(); return;
            }
            try {
                AuthService.getInstance().changePassword(oldPw, newPw);
                changed[0] = true;
            } catch (Exception ex) {
                formError.setText("• " + ex.getMessage());
                formError.setVisible(true); formError.setManaged(true);
                evt.consume();
            }
        });

        dialog.setResultConverter(bt -> null);
        dialog.showAndWait();
        if (changed[0]) {
            Alert ok = new Alert(Alert.AlertType.INFORMATION, "Đổi mật khẩu thành công!", ButtonType.OK);
            ok.setHeaderText(null); ok.showAndWait();
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
