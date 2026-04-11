package com.example.controller;

import com.example.dao.BookDAO;
import com.example.dao.MemberDAO;
import com.example.model.Book;
import com.example.model.BorrowRecord;
import com.example.model.Fine;
import com.example.model.Member;
import com.example.service.BorrowService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

public class BorrowController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<BorrowRecord> borrowTable;
    @FXML private TableColumn<BorrowRecord, String> colId;
    @FXML private TableColumn<BorrowRecord, String> colMember;
    @FXML private TableColumn<BorrowRecord, String> colBook;
    @FXML private TableColumn<BorrowRecord, String> colBorrowDate;
    @FXML private TableColumn<BorrowRecord, String> colDueDate;
    @FXML private TableColumn<BorrowRecord, String> colReturnDate;
    @FXML private TableColumn<BorrowRecord, String> colStatus;
    @FXML private TableColumn<BorrowRecord, Void>   colActions;
    @FXML private Label statusLabel;

    private final BorrowService borrowService = new BorrowService();
    private final MemberDAO     memberDAO     = new MemberDAO();
    private final BookDAO       bookDAO       = new BookDAO();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "BORROWING", "RETURNED", "OVERDUE", "LOST"));
        statusFilter.setValue("Tất cả");
        setupColumns();
        loadRecords();
    }

    private void setupColumns() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colMember.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getMemberCode() + " - " + c.getValue().getMemberName()));
        colBook.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookTitle()));
        colBorrowDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getBorrowDate() != null ? c.getValue().getBorrowDate().toString() : ""));
        colDueDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDueDate() != null ? c.getValue().getDueDate().toString() : ""));
        colReturnDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getReturnDate() != null ? c.getValue().getReturnDate().toString() : "—"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                switch (item) {
                    case "RETURNED"  -> badge.getStyleClass().add("badge-success");
                    case "BORROWING" -> badge.getStyleClass().add("badge-info");
                    case "OVERDUE"   -> badge.getStyleClass().add("badge-danger");
                    case "LOST"      -> badge.getStyleClass().add("badge-warning");
                }
                setGraphic(badge);
            }
        });
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button returnBtn = new Button("Trả sách");
            private final Button lostBtn   = new Button("Mất sách");
            private final HBox   box       = new HBox(6, returnBtn, lostBtn);
            {
                returnBtn.getStyleClass().add("btn-success");
                returnBtn.setStyle("-fx-font-size:11px; -fx-padding:4 8 4 8;");
                lostBtn.getStyleClass().add("btn-danger");
                lostBtn.setStyle("-fx-font-size:11px; -fx-padding:4 8 4 8;");
                returnBtn.setOnAction(e -> handleReturn(getTableView().getItems().get(getIndex())));
                lostBtn.setOnAction(e -> handleMarkLost(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                BorrowRecord r = getTableView().getItems().get(getIndex());
                boolean active = r.getStatus() == BorrowRecord.Status.BORROWING
                              || r.getStatus() == BorrowRecord.Status.OVERDUE;
                setGraphic(active ? box : null);
            }
        });
    }

    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim();
        String statusVal = statusFilter.getValue();
        List<BorrowRecord> list = borrowService.search(kw);
        if (statusVal != null && !"Tất cả".equals(statusVal)) {
            BorrowRecord.Status st = BorrowRecord.Status.valueOf(statusVal);
            list = list.stream().filter(r -> r.getStatus() == st).toList();
        }
        borrowTable.setItems(FXCollections.observableArrayList(list));
        statusLabel.setText("Tìm thấy " + list.size() + " phiếu");
    }

    @FXML
    private void handleNewBorrow() {
        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Mượn sách mới");
        dialog.setHeaderText("Chọn đọc giả và sách cần mượn");

        ButtonType borrowType = new ButtonType("Xác nhận mượn", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(borrowType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        ComboBox<Member> memberCombo = new ComboBox<>();
        memberCombo.getItems().addAll(memberDAO.findAll());
        memberCombo.setPromptText("Chọn đọc giả");
        memberCombo.setPrefWidth(280);

        ComboBox<Book> bookCombo = new ComboBox<>();
        bookCombo.getItems().addAll(bookDAO.findAll().stream()
            .filter(Book::isAvailable).toList());
        bookCombo.setPromptText("Chọn sách (còn sẵn)");
        bookCombo.setPrefWidth(280);

        TextField loanDaysField = new TextField("14");

        grid.add(new Label("Đọc giả *:"),      0, 0); grid.add(memberCombo,  1, 0);
        grid.add(new Label("Sách *:"),          0, 1); grid.add(bookCombo,   1, 1);
        grid.add(new Label("Số ngày mượn:"),   0, 2); grid.add(loanDaysField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == borrowType && memberCombo.getValue() != null && bookCombo.getValue() != null) {
                int days = 14;
                try { days = Integer.parseInt(loanDaysField.getText().trim()); } catch (NumberFormatException ignored) {}
                return new int[]{ memberCombo.getValue().getId(), bookCombo.getValue().getId(), days };
            }
            return null;
        });

        dialog.showAndWait().ifPresent(arr -> {
            try {
                BorrowRecord record = borrowService.borrowBook(arr[0], arr[1], arr[2]);
                loadRecords();
                showInfo("Mượn sách thành công!\nHạn trả: " + record.getDueDate());
            } catch (Exception e) { showError(e.getMessage()); }
        });
    }

    private void handleReturn(BorrowRecord record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Xác nhận trả sách:\n\"" + record.getBookTitle() + "\"\nBởi: " + record.getMemberName(),
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Trả sách");
        confirm.showAndWait().ifPresent(t -> {
            if (t == ButtonType.YES) {
                try {
                    Optional<Fine> fine = borrowService.returnBook(record.getId());
                    loadRecords();
                    if (fine.isPresent()) {
                        showInfo("Trả sách thành công!\nPhí phạt quá hạn: "
                            + fine.get().getAmount().toPlainString() + "đ\nLý do: " + fine.get().getReason());
                    } else {
                        showInfo("Trả sách thành công!");
                    }
                } catch (Exception e) { showError(e.getMessage()); }
            }
        });
    }

    private void handleMarkLost(BorrowRecord record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Đánh dấu sách \"" + record.getBookTitle() + "\" là MẤT?\n"
                + "Phí bồi thường sách mất: 200,000đ sẽ được ghi vào phiếu phạt.",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Xác nhận mất sách");
        confirm.showAndWait().ifPresent(t -> {
            if (t == ButtonType.YES) {
                try {
                    Fine fine = borrowService.markAsLost(record.getId());
                    loadRecords();
                    showInfo("Đã đánh dấu mất sách.\nPhí bồi thường: "
                        + fine.getAmount().toPlainString() + "đ\nLý do: " + fine.getReason());
                } catch (Exception e) { showError(e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleSyncOverdue() {
        int count = borrowService.syncOverdueStatuses();
        loadRecords();
        showInfo("Đã cập nhật " + count + " phiếu mượn quá hạn.");
    }

    private void loadRecords() {
        List<BorrowRecord> list = borrowService.getAllRecords();
        borrowTable.setItems(FXCollections.observableArrayList(list));
        statusLabel.setText("Tổng cộng " + list.size() + " phiếu mượn");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Lỗi"); a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
}
