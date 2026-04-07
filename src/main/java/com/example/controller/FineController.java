package com.example.controller;

import com.example.dao.FineDAO;
import com.example.model.Fine;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FineController {

    @FXML private Label totalUnpaidLabel;
    @FXML private Label unpaidCountLabel;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> paidFilter;
    @FXML private TableView<Fine> fineTable;
    @FXML private TableColumn<Fine, String> colId;
    @FXML private TableColumn<Fine, String> colMember;
    @FXML private TableColumn<Fine, String> colBook;
    @FXML private TableColumn<Fine, String> colDueDate;
    @FXML private TableColumn<Fine, String> colReturnDate;
    @FXML private TableColumn<Fine, String> colAmount;
    @FXML private TableColumn<Fine, String> colReason;
    @FXML private TableColumn<Fine, String> colPaid;
    @FXML private TableColumn<Fine, Void>   colActions;
    @FXML private Label statusLabel;

    private final FineDAO fineDAO = new FineDAO();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        paidFilter.setItems(FXCollections.observableArrayList("Tất cả", "Chưa thanh toán", "Đã thanh toán"));
        paidFilter.setValue("Tất cả");
        setupColumns();
        loadFines();
        updateSummary();
    }

    private void setupColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colMember.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getMemberCode() + " - " + c.getValue().getMemberName()));
        colBook.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookTitle()));
        colDueDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDueDate() != null ? c.getValue().getDueDate().toString() : ""));
        colReturnDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getReturnDate() != null ? c.getValue().getReturnDate().toString() : "—"));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getAmount() != null ? currencyFormat.format(c.getValue().getAmount()) + "đ" : "0đ"));
        colReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getReason()));
        colPaid.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isPaid() ? "Đã thanh toán" : "Chưa TT"));
        colPaid.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add(item.startsWith("Đã") ? "badge-success" : "badge-danger");
                setGraphic(badge);
            }
        });
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button payBtn = new Button("Đã thu");
            private final HBox   box    = new HBox(payBtn);
            {
                payBtn.getStyleClass().add("btn-success");
                payBtn.setStyle("-fx-font-size:11px; -fx-padding:4 10 4 10;");
                payBtn.setOnAction(e -> handleMarkPaid(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Fine f = getTableView().getItems().get(getIndex());
                setGraphic(f.isPaid() ? null : box);
            }
        });
    }

    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim();
        String filterVal = paidFilter.getValue();
        List<Fine> list = kw.isEmpty() ? fineDAO.findAll() : fineDAO.search(kw);
        if ("Chưa thanh toán".equals(filterVal)) {
            list = list.stream().filter(f -> !f.isPaid()).toList();
        } else if ("Đã thanh toán".equals(filterVal)) {
            list = list.stream().filter(Fine::isPaid).toList();
        }
        fineTable.setItems(FXCollections.observableArrayList(list));
        statusLabel.setText("Tìm thấy " + list.size() + " phiếu phạt");
    }

    private void handleMarkPaid(Fine fine) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Xác nhận đã thu phí phạt " + currencyFormat.format(fine.getAmount()) + "đ\n" +
            "Từ: " + fine.getMemberName() + "\nSách: " + fine.getBookTitle(),
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Thu phí phạt");
        confirm.showAndWait().ifPresent(t -> {
            if (t == ButtonType.YES) {
                fineDAO.markAsPaid(fine.getId());
                loadFines();
                updateSummary();
                showInfo("Đã ghi nhận thanh toán phí phạt.");
            }
        });
    }

    private void loadFines() {
        List<Fine> list = fineDAO.findAll();
        fineTable.setItems(FXCollections.observableArrayList(list));
        statusLabel.setText("Tổng cộng " + list.size() + " phiếu phạt");
    }

    private void updateSummary() {
        var unpaidAmount = fineDAO.totalUnpaidAmount();
        var unpaid = fineDAO.findUnpaid();
        totalUnpaidLabel.setText(currencyFormat.format(unpaidAmount) + "đ");
        unpaidCountLabel.setText(String.valueOf(unpaid.size()));
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
}
