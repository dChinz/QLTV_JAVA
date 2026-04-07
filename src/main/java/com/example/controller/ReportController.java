package com.example.controller;

import com.example.service.ReportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportController {

    @FXML private ComboBox<Integer> yearCombo;

    @FXML private TableView<Map<String, Object>> monthlyTable;
    @FXML private TableColumn<Map<String, Object>, String> colMonth;
    @FXML private TableColumn<Map<String, Object>, String> colMonthly;
    @FXML private TableColumn<Map<String, Object>, String> colBar;

    @FXML private TableView<Map<String, Object>> topBooksTable;
    @FXML private TableColumn<Map<String, Object>, String> colBRank;
    @FXML private TableColumn<Map<String, Object>, String> colBTitle;
    @FXML private TableColumn<Map<String, Object>, String> colBCount;

    @FXML private TableView<Map<String, Object>> categoryTable;
    @FXML private TableColumn<Map<String, Object>, String> colCatName;
    @FXML private TableColumn<Map<String, Object>, String> colCatCount;
    @FXML private TableColumn<Map<String, Object>, String> colCatBar;

    @FXML private TableView<Map<String, Object>> topMembersTable;
    @FXML private TableColumn<Map<String, Object>, String> colMRank;
    @FXML private TableColumn<Map<String, Object>, String> colMCode;
    @FXML private TableColumn<Map<String, Object>, String> colMName;
    @FXML private TableColumn<Map<String, Object>, String> colMCount;

    private final ReportService reportService = new ReportService();
    private static final String[] MONTH_NAMES = {
        "", "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
        "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
    };

    @FXML
    public void initialize() {
        int currentYear = LocalDate.now().getYear();
        yearCombo.getItems().addAll(currentYear - 2, currentYear - 1, currentYear);
        yearCombo.setValue(currentYear);
        yearCombo.setOnAction(e -> loadData());

        setupMonthlyTable();
        setupTopBooksTable();
        setupCategoryTable();
        setupTopMembersTable();
        loadData();
    }

    @FXML
    private void refresh() {
        loadData();
    }

    private void loadData() {
        int year = yearCombo.getValue() != null ? yearCombo.getValue() : LocalDate.now().getYear();

        // Monthly
        List<Map<String, Object>> monthly = reportService.getMonthlyBorrowStats(year);
        long maxMonthly = monthly.stream()
            .mapToLong(m -> ((Number) m.get("total")).longValue()).max().orElse(1);
        monthly.forEach(m -> m.put("bar", buildBar(((Number) m.get("total")).longValue(), maxMonthly)));
        monthlyTable.setItems(FXCollections.observableArrayList(monthly));

        // Top books
        List<Map<String, Object>> topBooks = reportService.getTopBorrowedBooks(10);
        AtomicInteger rank = new AtomicInteger(1);
        topBooks.forEach(m -> m.put("rank", String.valueOf(rank.getAndIncrement())));
        topBooksTable.setItems(FXCollections.observableArrayList(topBooks));

        // Category
        List<Map<String, Object>> cats = reportService.getBooksPerCategory();
        long maxCat = cats.stream()
            .mapToLong(m -> ((Number) m.get("bookCount")).longValue()).max().orElse(1);
        cats.forEach(m -> m.put("bar", buildBar(((Number) m.get("bookCount")).longValue(), maxCat)));
        categoryTable.setItems(FXCollections.observableArrayList(cats));

        // Top members
        List<Map<String, Object>> topMembers = reportService.getTopActiveMembers(10);
        rank.set(1);
        topMembers.forEach(m -> m.put("rank", String.valueOf(rank.getAndIncrement())));
        topMembersTable.setItems(FXCollections.observableArrayList(topMembers));
    }

    private String buildBar(long value, long max) {
        int filled = max > 0 ? (int) (value * 20 / max) : 0;
        return "█".repeat(filled) + " " + value;
    }

    private void setupMonthlyTable() {
        colMonth.setCellValueFactory(c -> {
            int m = ((Number) c.getValue().get("month")).intValue();
            return new SimpleStringProperty(m < MONTH_NAMES.length ? MONTH_NAMES[m] : "Tháng " + m);
        });
        colMonthly.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("total"))));
        colBar.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("bar")));
    }

    private void setupTopBooksTable() {
        colBRank.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("rank")));
        colBTitle.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("title")));
        colBCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("borrowCount"))));
    }

    private void setupCategoryTable() {
        colCatName.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("category")));
        colCatCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("bookCount"))));
        colCatBar.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("bar")));
    }

    private void setupTopMembersTable() {
        colMRank.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("rank")));
        colMCode.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("memberCode")));
        colMName.setCellValueFactory(c -> new SimpleStringProperty((String) c.getValue().get("fullName")));
        colMCount.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().get("borrowCount"))));
    }
}
