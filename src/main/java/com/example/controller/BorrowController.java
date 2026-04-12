package com.example.controller;

import com.example.dao.BookDAO;
import com.example.dao.BorrowRecordDAO;
import com.example.dao.FineDAO;
import com.example.dao.MemberDAO;
import com.example.model.Book;
import com.example.model.BorrowRecord;
import com.example.model.Fine;
import com.example.model.Member;
import com.example.service.BorrowService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BorrowController {

    @FXML private TextField    searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private DatePicker   fromDatePicker;
    @FXML private DatePicker   toDatePicker;
    @FXML private TableView<BorrowRecord> borrowTable;
    @FXML private TableColumn<BorrowRecord, Void>   colSTT;
    @FXML private TableColumn<BorrowRecord, String> colMember;
    @FXML private TableColumn<BorrowRecord, String> colBook;
    @FXML private TableColumn<BorrowRecord, String> colBorrowDate;
    @FXML private TableColumn<BorrowRecord, String> colDueDate;
    @FXML private TableColumn<BorrowRecord, String> colReturnDate;
    @FXML private TableColumn<BorrowRecord, String> colStatus;
    @FXML private TableColumn<BorrowRecord, String> colNote;
    @FXML private TableColumn<BorrowRecord, Void>   colActions;
    @FXML private Label  statusLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Label  pageInfoLabel;

    private static final int PAGE_SIZE        = 20;
    private int              currentPage       = 0;
    private List<BorrowRecord> masterList      = new ArrayList<>();
    private List<BorrowRecord> filteredList    = new ArrayList<>();

    private final BorrowService    borrowService = new BorrowService();
    private final MemberDAO        memberDAO     = new MemberDAO();
    private final BookDAO          bookDAO       = new BookDAO();
    private final FineDAO          fineDAO       = new FineDAO();
    private final NumberFormat     fmt           =
        NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList(
            "Tất cả", "BORROWING", "RETURNED", "OVERDUE", "LOST"));
        statusFilter.setValue("Tất cả");
        setupColumns();
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        fromDatePicker.valueProperty().addListener((obs, o, n) -> applyFilter());
        toDatePicker.valueProperty().addListener((obs, o, n) -> applyFilter());
        loadRecords();
    }

    private void setupColumns() {
        colSTT.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(currentPage * PAGE_SIZE + getIndex() + 1));
            }
        });
        colMember.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getMemberCode() + " - " + c.getValue().getMemberName()));
        colBook.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBookTitle()));
        colBorrowDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getBorrowDate() != null ? c.getValue().getBorrowDate().toString() : ""));
        colDueDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDueDate() != null ? c.getValue().getDueDate().toString() : ""));
        colReturnDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getReturnDate() != null ? c.getValue().getReturnDate().toString() : "—"));
        colNote.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getNotes() != null ? c.getValue().getNotes() : ""));

        // Rich status cell
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) {
                    setGraphic(null); setText(null); return;
                }
                BorrowRecord r = getTableView().getItems().get(idx);
                Label badge = new Label();
                VBox box = new VBox(2, badge);
                switch (item) {
                    case "RETURNED"  -> { badge.setText("Đã trả");  badge.getStyleClass().add("badge-success"); }
                    case "BORROWING" -> { badge.setText("Đang mượn"); badge.getStyleClass().add("badge-info");    }
                    case "OVERDUE"   -> {
                        long days = r.getOverdueDays();
                        badge.setText("Quá hạn"); badge.getStyleClass().add("badge-danger");
                        if (days > 0) {
                            BigDecimal fee = BorrowService.FINE_PER_DAY.multiply(BigDecimal.valueOf(days));
                            Label dLabel = new Label("Trễ " + days + " ngày");
                            dLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#B91C1C;");
                            Label fLabel = new Label("Phạt: " + fmt.format(fee) + "đ");
                            fLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#D97706;");
                            box.getChildren().addAll(dLabel, fLabel);
                        }
                    }
                    case "LOST" -> {
                        long days = r.getOverdueDays();
                        badge.setText("Mất sách"); badge.getStyleClass().add("badge-warning");
                        Label lLabel = new Label("Bồi thường: 200,000đ");
                        lLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#DC2626;");
                        box.getChildren().add(lLabel);
                        if (days > 0) {
                            BigDecimal fee = BorrowService.FINE_PER_DAY.multiply(BigDecimal.valueOf(days));
                            Label fLabel = new Label("Phạt: " + fmt.format(fee) + "đ");
                            fLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#D97706;");
                            box.getChildren().add(fLabel);
                        }
                    }
                    default -> badge.setText(item);
                }
                box.setStyle("-fx-padding: 2 0 2 0;");
                setGraphic(box);
                setText(null);
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
                lostBtn.setOnAction(e  -> handleMarkLost(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) { setGraphic(null); return; }
                BorrowRecord r = getTableView().getItems().get(idx);
                boolean active = r.getStatus() == BorrowRecord.Status.BORROWING
                              || r.getStatus() == BorrowRecord.Status.OVERDUE;
                setGraphic(active ? box : null);
            }
        });
    }

    // ── Filter / Pagination ─────────────────────────────────────────────────

    private void applyFilter() {
        String kw      = searchField.getText().toLowerCase().trim();
        String stVal   = statusFilter.getValue();
        LocalDate from = fromDatePicker.getValue();
        LocalDate to   = toDatePicker.getValue();
        filteredList = masterList.stream()
            .filter(r -> {
                boolean stOk = stVal == null || "Tất cả".equals(stVal)
                    || r.getStatus().name().equals(stVal);
                boolean kwOk = kw.isEmpty()
                    || r.getMemberName().toLowerCase().contains(kw)
                    || r.getMemberCode().toLowerCase().contains(kw)
                    || r.getBookTitle().toLowerCase().contains(kw);
                boolean dateOk = true;
                if (from != null && r.getBorrowDate() != null)
                    dateOk = !r.getBorrowDate().isBefore(from);
                if (to   != null && r.getBorrowDate() != null)
                    dateOk = dateOk && !r.getBorrowDate().isAfter(to);
                return stOk && kwOk && dateOk;
            })
            .toList();
        currentPage = 0;
        showPage();
    }

    private void showPage() {
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredList.size());
        borrowTable.setItems(FXCollections.observableArrayList(
            from < filteredList.size() ? filteredList.subList(from, to) : List.of()));
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE));
        pageInfoLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        statusLabel.setText(filteredList.size() + " phiếu mượn");
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable((currentPage + 1) * PAGE_SIZE >= filteredList.size());
        borrowTable.refresh();
    }

    @FXML private void prevPage() { if (currentPage > 0) { currentPage--; showPage(); } }
    @FXML private void nextPage() { if ((currentPage + 1) * PAGE_SIZE < filteredList.size()) { currentPage++; showPage(); } }

    @FXML private void handleSearch() { applyFilter(); }

    @FXML
    private void clearDateFilter() {
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
    }

    // ── New Borrow ─────────────────────────────────────────────────────────

    @FXML
    private void handleNewBorrow() {
        List<Member> allMembers = memberDAO.findAll().stream()
            .filter(m -> m.getStatus() == Member.Status.ACTIVE)
            .sorted(java.util.Comparator.comparing(Member::getFullName))
            .toList();
        List<Book> allBooks = bookDAO.findAll().stream()
            .filter(Book::isAvailable)
            .sorted(java.util.Comparator.comparing(Book::getTitle))
            .toList();

        Dialog<int[]> dialog = new Dialog<>();
        dialog.setTitle("Mượn sách mới");
        dialog.setHeaderText(null);

        ButtonType borrowType = new ButtonType("Xác nhận mượn", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(borrowType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        // Autocomplete member combobox
        ObservableList<Member> memberItems = FXCollections.observableArrayList(allMembers);
        FilteredList<Member> filteredMembers = new FilteredList<>(memberItems, m -> true);
        ComboBox<Member> memberCombo = new ComboBox<>(filteredMembers);
        memberCombo.setEditable(true);
        memberCombo.setPrefWidth(300);
        StringConverter<Member> memberConv = new StringConverter<>() {
            public String toString(Member m) { return m == null ? "" : m.getMemberCode() + " – " + m.getFullName(); }
            public Member fromString(String s) { return null; }
        };
        memberCombo.setConverter(memberConv);
        memberCombo.getEditor().textProperty().addListener((obs, old, val) -> {
            Member sel = memberCombo.getValue();
            if (sel != null && memberConv.toString(sel).equals(val)) return;
            String lower = val == null ? "" : val.toLowerCase();
            filteredMembers.setPredicate(m -> lower.isEmpty()
                || m.getFullName().toLowerCase().contains(lower)
                || m.getMemberCode().toLowerCase().contains(lower));
            if (!memberCombo.isShowing()) memberCombo.show();
        });

        // Autocomplete book combobox
        ObservableList<Book> bookItems = FXCollections.observableArrayList(allBooks);
        FilteredList<Book> filteredBooks = new FilteredList<>(bookItems, b -> true);
        ComboBox<Book> bookCombo = new ComboBox<>(filteredBooks);
        bookCombo.setEditable(true);
        bookCombo.setPrefWidth(300);
        StringConverter<Book> bookConv = new StringConverter<>() {
            public String toString(Book b) { return b == null ? "" : b.getIsbn() + " – " + b.getTitle(); }
            public Book fromString(String s) { return null; }
        };
        bookCombo.setConverter(bookConv);
        bookCombo.getEditor().textProperty().addListener((obs, old, val) -> {
            Book sel = bookCombo.getValue();
            if (sel != null && bookConv.toString(sel).equals(val)) return;
            String lower = val == null ? "" : val.toLowerCase();
            filteredBooks.setPredicate(b -> lower.isEmpty()
                || b.getTitle().toLowerCase().contains(lower)
                || b.getIsbn().toLowerCase().contains(lower)
                || b.getAuthor().toLowerCase().contains(lower));
            if (!bookCombo.isShowing()) bookCombo.show();
        });

        DatePicker returnDatePicker = new DatePicker(LocalDate.now().plusDays(14));
        returnDatePicker.setPrefWidth(160);

        grid.add(new Label("Đọc giả *:"),    0, 0); grid.add(memberCombo,      1, 0);
        grid.add(new Label("Sách *:"),          0, 1); grid.add(bookCombo,        1, 1);
        grid.add(new Label("Ngày trả dự kiến:"), 0, 2); grid.add(returnDatePicker, 1, 2);

        Label formError = new Label();
        formError.setVisible(false); formError.setManaged(false);
        formError.setWrapText(true); formError.setMaxWidth(340);
        formError.setStyle("-fx-text-fill: #E11D48; -fx-font-size: 12px;");
        grid.add(formError, 0, 3, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(480);

        Button borrowBtn = (Button) dialog.getDialogPane().lookupButton(borrowType);
        borrowBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            List<String> errs = new ArrayList<>();
            if (memberCombo.getValue() == null) errs.add("• Vui lòng chọn đọc giả.");
            if (bookCombo.getValue() == null)   errs.add("• Vui lòng chọn sách.");
            if (returnDatePicker.getValue() == null ||
                !returnDatePicker.getValue().isAfter(LocalDate.now()))
                errs.add("• Ngày trả phải sau ngày hôm nay.");
            if (!errs.isEmpty()) {
                formError.setText(String.join("\n", errs));
                formError.setVisible(true); formError.setManaged(true);
                evt.consume();
            }
        });
        memberCombo.valueProperty().addListener((obs, o, n) -> { formError.setVisible(false); formError.setManaged(false); });
        bookCombo.valueProperty().addListener((obs, o, n)   -> { formError.setVisible(false); formError.setManaged(false); });

        dialog.setResultConverter(bt -> {
            if (bt == borrowType && memberCombo.getValue() != null && bookCombo.getValue() != null) {
                long loanDays = java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.now(), returnDatePicker.getValue());
                return new int[]{ memberCombo.getValue().getId(),
                                  bookCombo.getValue().getId(),
                                  (int) Math.max(1, loanDays) };
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

    // ── Return / Lost ──────────────────────────────────────────────────────

    private void handleReturn(BorrowRecord record) {
        long overdueDays = record.getOverdueDays();
        String detail = overdueDays > 0
            ? "\nQuá hạn " + overdueDays + " ngày. Dự kiến phạt: "
              + fmt.format(BorrowService.FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays))) + "đ"
            : " (trả đúng hạn)";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Trả sách \"" + record.getBookTitle() + "\"?" + detail,
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Xác nhận trả sách");
        confirm.showAndWait().ifPresent(t -> {
            if (t != ButtonType.YES) return;
            try {
                Optional<Fine> fine = borrowService.returnBook(record.getId());
                loadRecords();
                if (fine.isPresent()) {
                    Fine f = fine.get();
                    // Step 2: offer to pay fine immediately
                    Alert payDialog = new Alert(Alert.AlertType.CONFIRMATION);
                    payDialog.setTitle("Thu phí phạt");
                    payDialog.setHeaderText("Phiếu phạt quá hạn");
                    payDialog.setContentText(
                        "Số tiền: " + fmt.format(f.getAmount()) + "đ\n"
                        + "Lý do: " + f.getReason() + "\n\nThu phí phạt ngay?");
                    ButtonType payNow   = new ButtonType("Đã thu", ButtonBar.ButtonData.YES);
                    ButtonType payLater = new ButtonType("Sau", ButtonBar.ButtonData.NO);
                    payDialog.getButtonTypes().setAll(payNow, payLater);
                    payDialog.showAndWait().ifPresent(p -> {
                        if (p == payNow) {
                            fineDAO.markAsPaid(f.getId());
                            showInfo("Trả sách thành công. Đã thu phí phạt "
                                     + fmt.format(f.getAmount()) + "đ.");
                        } else {
                            showInfo("Trả sách thành công. Phí phạt chưa thu.");
                        }
                    });
                } else {
                    showInfo("Trả sách thành công!");
                }
            } catch (Exception e) { showError(e.getMessage()); }
        });
    }

    private void handleMarkLost(BorrowRecord record) {
        long days = record.getOverdueDays();
        String feeStr = days > 0
            ? "\nPhạt trễ: " + fmt.format(BorrowService.FINE_PER_DAY.multiply(BigDecimal.valueOf(days))) + "đ"
            : "";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Đánh dấu sách \"" + record.getBookTitle() + "\" là MẤT?\n"
            + "Phí bồi thường: 200,000đ" + feeStr,
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Xác nhận mất sách");
        confirm.showAndWait().ifPresent(t -> {
            if (t != ButtonType.YES) return;
            try {
                Fine fine = borrowService.markAsLost(record.getId());
                loadRecords();
                // Step 2: offer to pay fine immediately
                Alert payDialog = new Alert(Alert.AlertType.CONFIRMATION);
                payDialog.setTitle("Thu phí bồi thường");
                payDialog.setHeaderText("Phiếu bồi thường sách mất");
                payDialog.setContentText(
                    "Số tiền: " + fmt.format(fine.getAmount()) + "đ\n"
                    + "Lý do: " + fine.getReason() + "\n\nThu tiền bồi thường ngay?");
                ButtonType payNow   = new ButtonType("Đã thu", ButtonBar.ButtonData.YES);
                ButtonType payLater = new ButtonType("Sau", ButtonBar.ButtonData.NO);
                payDialog.getButtonTypes().setAll(payNow, payLater);
                payDialog.showAndWait().ifPresent(p -> {
                    if (p == payNow) {
                        fineDAO.markAsPaid(fine.getId());
                        showInfo("Đã ghi nhận mất sách. Đã thu bồi thường "
                                 + fmt.format(fine.getAmount()) + "đ.");
                    } else {
                        showInfo("Đã ghi nhận mất sách. Tiền bồi thường chưa thu.");
                    }
                });
            } catch (Exception e) { showError(e.getMessage()); }
        });
    }

    @FXML
    private void handleSyncOverdue() {
        int count = borrowService.syncOverdueStatuses();
        loadRecords();
        showInfo("Đã cập nhật " + count + " phiếu mượn quá hạn.");
    }

    private void loadRecords() {
        masterList = borrowService.getAllRecords();
        applyFilter();
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
