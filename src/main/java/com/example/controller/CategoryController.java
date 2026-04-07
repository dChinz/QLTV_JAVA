package com.example.controller;

import com.example.dao.CategoryDAO;
import com.example.model.Category;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class CategoryController {

    @FXML private TextField  searchField;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableColumn<Category, String> colId;
    @FXML private TableColumn<Category, String> colName;
    @FXML private TableColumn<Category, String> colBooks;
    @FXML private TableColumn<Category, String> colDesc;
    @FXML private Label      statusLabel;

    @FXML private Label      formTitle;
    @FXML private TextField  nameField;
    @FXML private TextArea   descField;
    @FXML private Button     saveButton;
    @FXML private Button     deleteButton;
    @FXML private Label      formMessage;

    private final CategoryDAO categoryDAO = new CategoryDAO();
    private Category selectedCategory;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colBooks.setCellValueFactory(c -> new SimpleStringProperty(
            String.valueOf(categoryDAO.countBooksInCategory(c.getValue().getId()))));
        colDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));
        loadCategories();
    }

    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim();
        List<Category> list = kw.isEmpty() ? categoryDAO.findAll() : categoryDAO.search(kw);
        categoryTable.setItems(FXCollections.observableArrayList(list));
        statusLabel.setText("Tìm thấy " + list.size() + " danh mục");
    }

    @FXML
    private void handleTableClick() {
        Category c = categoryTable.getSelectionModel().getSelectedItem();
        if (c == null) return;
        selectedCategory = c;
        nameField.setText(c.getName());
        descField.setText(c.getDescription() != null ? c.getDescription() : "");
        formTitle.setText("Cập nhật danh mục");
        saveButton.setText("Cập nhật");
        deleteButton.setVisible(true);
        deleteButton.setManaged(true);
        formMessage.setText("");
    }

    @FXML
    private void handleSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            setMessage("Tên danh mục không được để trống.", true);
            return;
        }
        try {
            if (selectedCategory == null) {
                Category c = new Category(name, descField.getText().trim());
                categoryDAO.save(c);
                setMessage("Thêm danh mục thành công.", false);
            } else {
                selectedCategory.setName(name);
                selectedCategory.setDescription(descField.getText().trim());
                categoryDAO.update(selectedCategory);
                setMessage("Cập nhật thành công.", false);
            }
            handleClear();
            loadCategories();
        } catch (Exception e) {
            setMessage("Lỗi: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedCategory == null) return;
        long bookCount = categoryDAO.countBooksInCategory(selectedCategory.getId());
        if (bookCount > 0) {
            setMessage("Không thể xóa: danh mục có " + bookCount + " sách.", true);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Xóa danh mục \"" + selectedCategory.getName() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(t -> {
            if (t == ButtonType.YES) {
                categoryDAO.delete(selectedCategory.getId());
                setMessage("Đã xóa danh mục.", false);
                handleClear();
                loadCategories();
            }
        });
    }

    @FXML
    private void handleClear() {
        selectedCategory = null;
        nameField.clear();
        descField.clear();
        formTitle.setText("Thêm danh mục mới");
        saveButton.setText("Lưu");
        deleteButton.setVisible(false);
        deleteButton.setManaged(false);
        categoryTable.getSelectionModel().clearSelection();
    }

    private void loadCategories() {
        List<Category> list = categoryDAO.findAll();
        categoryTable.setItems(FXCollections.observableArrayList(list));
        statusLabel.setText("Tổng cộng " + list.size() + " danh mục");
    }

    private void setMessage(String msg, boolean isError) {
        formMessage.setText(msg);
        formMessage.setStyle(isError ? "-fx-text-fill: #DC2626;" : "-fx-text-fill: #16A34A;");
    }
}
