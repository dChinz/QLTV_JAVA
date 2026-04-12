package com.example.controller;

import com.example.model.Member;
import com.example.service.MemberService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<Member> memberTable;
    @FXML private TableColumn<Member, Void>   colSTT;
    @FXML private TableColumn<Member, String> colCode;
    @FXML private TableColumn<Member, String> colName;
    @FXML private TableColumn<Member, String> colPhone;
    @FXML private TableColumn<Member, String> colEmail;
    @FXML private TableColumn<Member, String> colExpiry;
    @FXML private TableColumn<Member, String> colStatus;
    @FXML private TableColumn<Member, Void>   colActions;
    @FXML private Label  statusLabel;
    @FXML private Button prevPageBtn;
    @FXML private Button nextPageBtn;
    @FXML private Label  pageInfoLabel;

    private static final int PAGE_SIZE   = 20;
    private int            currentPage   = 0;
    private List<Member>   masterList    = new ArrayList<>();
    private List<Member>   filteredList  = new ArrayList<>();

    private final MemberService memberService = new MemberService();

    @FXML
    public void initialize() {
        statusFilter.setItems(FXCollections.observableArrayList("Tất cả", "ACTIVE", "EXPIRED", "SUSPENDED"));
        statusFilter.setValue("Tất cả");
        setupColumns();
        memberService.syncExpiredStatuses();
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        loadMembers();
    }

    private void setupColumns() {
        colSTT.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(currentPage * PAGE_SIZE + getIndex() + 1));
            }
        });
        colCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMemberCode()));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colExpiry.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getExpiryDate() != null ? c.getValue().getExpiryDate().toString() : ""));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label badge = new Label(item);
                switch (item) {
                    case "ACTIVE"    -> badge.getStyleClass().add("badge-success");
                    case "EXPIRED"   -> badge.getStyleClass().add("badge-warning");
                    case "SUSPENDED" -> badge.getStyleClass().add("badge-danger");
                }
                setGraphic(badge);
            }
        });
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Sửa");
            private final Button renewBtn  = new Button("Gia hạn");
            private final Button deleteBtn = new Button("Xóa");
            private final HBox   box       = new HBox(6, editBtn, renewBtn, deleteBtn);
            {
                editBtn.getStyleClass().add("btn-outline");
                editBtn.setStyle("-fx-font-size:11px; -fx-padding:4 8 4 8;");
                renewBtn.getStyleClass().add("btn-warning");
                renewBtn.setStyle("-fx-font-size:11px; -fx-padding:4 8 4 8;");
                deleteBtn.getStyleClass().add("btn-outline");
                deleteBtn.setStyle("-fx-font-size:11px; -fx-padding:4 8 4 8;");
                editBtn.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                renewBtn.setOnAction(e -> handleRenew(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    @FXML
    private void handleSearch() { applyFilter(); }

    private void applyFilter() {
        String kw = searchField.getText().toLowerCase().trim();
        String statusVal = statusFilter.getValue();
        filteredList = masterList.stream()
            .filter(m -> {
                boolean statusOk = statusVal == null || "Tất cả".equals(statusVal)
                    || m.getStatus().name().equals(statusVal);
                boolean kwOk = kw.isEmpty()
                    || m.getFullName().toLowerCase().contains(kw)
                    || m.getMemberCode().toLowerCase().contains(kw)
                    || m.getPhone().toLowerCase().contains(kw);
                return statusOk && kwOk;
            })
            .sorted(java.util.Comparator.comparing(Member::getFullName))
            .toList();
        currentPage = 0;
        showPage();
    }

    private void showPage() {
        int from = currentPage * PAGE_SIZE;
        int to   = Math.min(from + PAGE_SIZE, filteredList.size());
        memberTable.setItems(FXCollections.observableArrayList(
            from < filteredList.size() ? filteredList.subList(from, to) : List.of()));
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredList.size() / PAGE_SIZE));
        pageInfoLabel.setText("Trang " + (currentPage + 1) + " / " + totalPages);
        statusLabel.setText(filteredList.size() + " đọc giả");
        prevPageBtn.setDisable(currentPage == 0);
        nextPageBtn.setDisable((currentPage + 1) * PAGE_SIZE >= filteredList.size());
        memberTable.refresh();
    }

    @FXML private void prevPage() { if (currentPage > 0) { currentPage--; showPage(); } }
    @FXML private void nextPage() { if ((currentPage + 1) * PAGE_SIZE < filteredList.size()) { currentPage++; showPage(); } }

    private void loadMembers() {
        masterList = memberService.getAllMembers();
        applyFilter();
    }

    @FXML
    private void handleAdd() {
        showMemberDialog(null);
    }

    private void handleEdit(Member member) {
        showMemberDialog(member);
    }

    private void handleRenew(Member member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Gia hạn thẻ cho \"" + member.getFullName() + "\" thêm 2 năm?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Gia hạn thẻ đọc giả");
        confirm.showAndWait().ifPresent(t -> {
            if (t == ButtonType.YES) {
                memberService.renewCard(member.getId(), 2);
                loadMembers();
                showInfo("Đã gia hạn thẻ thành công.");
            }
        });
    }

    private void handleDelete(Member member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Xóa đọc giả \"" + member.getFullName() + "\" khỏi danh sách?",
            ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Xác nhận xóa đọc giả");
        confirm.showAndWait().ifPresent(t -> {
            if (t == ButtonType.YES) {
                try {
                    memberService.deleteMember(member.getId());
                    loadMembers();
                    showInfo("Đã xóa đọc giả khỏi danh sách.");
                } catch (Exception e) { showError(e.getMessage()); }
            }
        });
    }

    private void showMemberDialog(Member member) {
        boolean isEdit = (member != null);
        Dialog<Member> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Cập nhật đọc giả" : "Thêm đọc giả mới");
        dialog.setHeaderText(null);

        ButtonType saveType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));

        TextField nameField    = new TextField(isEdit ? member.getFullName() : "");
        TextField emailField   = new TextField(isEdit ? member.getEmail()    : "");
        TextField phoneField   = new TextField(isEdit ? member.getPhone()    : "");
        TextArea  addrField    = new TextArea(isEdit ? member.getAddress()   : "");
        DatePicker expiryPicker = new DatePicker(isEdit ? member.getExpiryDate() : LocalDate.now().plusYears(2));
        ComboBox<Member.Status> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Member.Status.values());
        statusCombo.setValue(isEdit ? member.getStatus() : Member.Status.ACTIVE);

        nameField.setPrefWidth(280);
        addrField.setPrefRowCount(2);

        grid.add(new Label("Họ tên *:"),     0, 0); grid.add(nameField,    1, 0);
        grid.add(new Label("Email:"),         0, 1); grid.add(emailField,   1, 1);
        grid.add(new Label("Số điện thoại:"),0, 2); grid.add(phoneField,   1, 2);
        grid.add(new Label("Địa chỉ:"),      0, 3); grid.add(addrField,    1, 3);
        grid.add(new Label("Hạn thẻ:"),      0, 4); grid.add(expiryPicker, 1, 4);
        if (isEdit) {
            grid.add(new Label("Trạng thái:"), 0, 5);
            grid.add(statusCombo,               1, 5);
        }

        Label formError = new Label();
        formError.setVisible(false);
        formError.setManaged(false);
        formError.setWrapText(true);
        formError.setMaxWidth(320);
        formError.setStyle("-fx-text-fill: #E11D48; -fx-font-size: 12px;");
        grid.add(formError, 0, isEdit ? 6 : 5, 2, 1);

        dialog.getDialogPane().setContent(grid);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, evt -> {
            List<String> errs = new ArrayList<>();
            if (nameField.getText().isBlank()) {
                errs.add("• Họ tên không được để trống.");
                nameField.setStyle("-fx-border-color: #E11D48;");
            }
            String email = emailField.getText().trim();
            if (!email.isEmpty() && !email.matches("^[\\w+.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                errs.add("• Email không đúng định dạng.");
                emailField.setStyle("-fx-border-color: #E11D48;");
            }
            String phone = phoneField.getText().trim();
            if (!phone.isEmpty() && !phone.matches("^(0|\\+84)\\d{9,10}$")) {
                errs.add("• Số điện thoại không hợp lệ (bắt đầu 0 hoặc +84, 10-11 chữ số).");
                phoneField.setStyle("-fx-border-color: #E11D48;");
            }
            if (!errs.isEmpty()) {
                formError.setText(String.join("\n", errs));
                formError.setVisible(true);
                formError.setManaged(true);
                evt.consume();
            }
        });
        nameField.textProperty().addListener((obs, o, n) -> { nameField.setStyle(""); formError.setVisible(false); formError.setManaged(false); });
        emailField.textProperty().addListener((obs, o, n) -> { emailField.setStyle(""); formError.setVisible(false); formError.setManaged(false); });
        phoneField.textProperty().addListener((obs, o, n) -> { phoneField.setStyle(""); formError.setVisible(false); formError.setManaged(false); });

        dialog.setResultConverter(bt -> {
            if (bt == saveType) {
                Member m = isEdit ? member : new Member();
                m.setFullName(nameField.getText().trim());
                m.setEmail(emailField.getText().trim());
                m.setPhone(phoneField.getText().trim());
                m.setAddress(addrField.getText().trim());
                m.setExpiryDate(expiryPicker.getValue());
                m.setStatus(statusCombo.getValue());
                if (!isEdit) m.setJoinDate(LocalDate.now());
                return m;
            }
            return null;
        });

        Optional<Member> result = dialog.showAndWait();
        result.ifPresent(m -> {
            try {
                if (isEdit) memberService.updateMember(m);
                else        memberService.addMember(m);
                loadMembers();
                showInfo(isEdit ? "Cập nhật thành công." : "Thêm đọc giả thành công.");
            } catch (Exception e) { showError(e.getMessage()); }
        });
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
