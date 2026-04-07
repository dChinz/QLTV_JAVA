package com.example.controller;

import com.example.model.BorrowRecord;
import com.example.service.BorrowService;
import com.example.service.ReportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DashboardController {

    @FXML private Label totalBooksLabel;
    @FXML private Label totalMembersLabel;
    @FXML private Label activeBorrowsLabel;
    @FXML private Label overdueLabel;
    @FXML private Label unpaidFinesLabel;

    @FXML private TableView<Map<String, Object>> topBooksTable;
    @FXML private TableColumn<Map<String, Object>, String> colBookRank;
    @FXML private TableColumn<Map<String, Object>, String> colBookTitle;
    @FXML private TableColumn<Map<String, Object>, String> colBookAuthor;
    @FXML private TableColumn<Map<String, Object>, String> colBookCount;

    @FXML private TableView<Map<String, Object>> topMembersTable;
    @FXML private TableColumn<Map<String, Object>, String> colMemberRank;
    @FXML private TableColumn<Map<String, Object>, String> colMemberCode;
    @FXML private TableColumn<Map<String, Object>, String> colMemberName;
    @FXML private TableColumn<Map<String, Object>, String> colMemberCount;

    @FXML private TableView<BorrowRecord> overdueTable;
    @FXML private TableColumn<BorrowRecord, String> colOvMember;
    @FXML private TableColumn<BorrowRecord, String> colOvBook;
    @FXML private TableColumn<BorrowRecord, String> colOvDueDate;
    @FXML private TableColumn<BorrowRecord, String> colOvDays;

    private final ReportService reportService = new ReportService();
    private final BorrowService borrowService = new BorrowService();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        setupTopBooksTable();
        setupTopMembersTable();
        setupOverdueTable();
        loadData();
    }

    @FXML
    private void refresh() {
        loadData();
    }

    private void loadData() {
        try {
            borrowService.syncOverdueStatuses();
            Map<String, Object> stats = reportService.getDashboardStats();

            totalBooksLabel.setText(String.valueOf(stats.get("totalBooks")));
            totalMembersLabel.setText(String.valueOf(stats.get("totalMembers")));
            activeBorrowsLabel.setText(String.valueOf(stats.get("activeBorrows")));
            overdueLabel.setText(String.valueOf(stats.get("overdueCount")));

            BigDecimal unpaid = (BigDecimal) stats.get("unpaidFines");
            unpaidFinesLabel.setText(currencyFormat.format(unpaid) + "đ");

            List<Map<String, Object>> topBooks = reportService.getTopBorrowedBooks(10);
            AtomicInteger rank = new AtomicInteger(1);
            topBooks.forEach(m -> m.put("rank", String.valueOf(rank.getAndIncrement())));
            topBooksTable.setItems(FXCollections.observableArrayList(topBooks));

            List<Map<String, Object>> topMembers = reportService.getTopActiveMembers(10);
            rank.set(1);
            topMembers.forEach(m -> m.put("rank", String.valueOf(rank.getAndIncrement())));
            topMembersTable.setItems(FXCollections.observableArrayList(topMembers));

            List<BorrowRecord> overdue = borrowService.getOverdueRecords();
            overdueTable.setItems(FXCollections.observableArrayList(overdue));

        } catch (Exception e) {
            System.err.println("Dashboard load error: " + e.getMessage());
        }
    }

    private void setupTopBooksTable() {
        colBookRank.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("rank")));
        colBookTitle.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("title")));
        colBookAuthor.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("author")));
        colBookCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("borrowCount"))));
    }

    private void setupTopMembersTable() {
        colMemberRank.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("rank")));
        colMemberCode.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("memberCode")));
        colMemberName.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("fullName")));
        colMemberCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("borrowCount"))));
    }

    private void setupOverdueTable() {
        colOvMember.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getMemberCode() + " - " + c.getValue().getMemberName()));
        colOvBook.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getBookTitle()));
        colOvDueDate.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getDueDate() != null ? c.getValue().getDueDate().toString() : ""));
        colOvDays.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().getOverdueDays() + " ngày"));
    }
}
