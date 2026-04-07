package com.example.dao;

import com.example.config.DatabaseConfig;
import com.example.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookDAO {

    private Connection getConn() {
        return DatabaseConfig.getInstance().getConnection();
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setId(rs.getInt("id"));
        b.setIsbn(rs.getString("isbn"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setPublisher(rs.getString("publisher"));
        int year = rs.getInt("publish_year");
        if (!rs.wasNull()) b.setPublishYear(year);
        b.setCategoryId(rs.getInt("category_id"));
        b.setTotalCopies(rs.getInt("total_copies"));
        b.setAvailableCopies(rs.getInt("available_copies"));
        b.setDescription(rs.getString("description"));
        b.setCoverImagePath(rs.getString("cover_image_path"));
        try { b.setDeleted(rs.getBoolean("deleted")); } catch (SQLException ignored) { b.setDeleted(false); }
        try { b.setCategoryName(rs.getString("category_name")); } catch (SQLException ignored) {}
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) b.setCreatedAt(ts.toLocalDateTime());
        return b;
    }

    public List<Book> findAll() {
        List<Book> list = new ArrayList<>();
        String sql = """
            SELECT b.*, c.name AS category_name
            FROM books b LEFT JOIN categories c ON b.category_id = c.id
            WHERE b.deleted = FALSE
            ORDER BY b.title
            """;
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching books", e);
        }
        return list;
    }

    public Optional<Book> findById(int id) {
        String sql = """
            SELECT b.*, c.name AS category_name
            FROM books b LEFT JOIN categories c ON b.category_id = c.id
            WHERE b.id = ? AND b.deleted = FALSE
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding book by id", e);
        }
        return Optional.empty();
    }

    public List<Book> search(String keyword) {
        List<Book> list = new ArrayList<>();
        String sql = """
            SELECT b.*, c.name AS category_name
            FROM books b LEFT JOIN categories c ON b.category_id = c.id
            WHERE b.deleted = FALSE AND (b.title LIKE ? OR b.author LIKE ? OR b.isbn LIKE ?)
            ORDER BY b.title
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error searching books", e);
        }
        return list;
    }

    public List<Book> findByCategory(int categoryId) {
        List<Book> list = new ArrayList<>();
        String sql = """
            SELECT b.*, c.name AS category_name
            FROM books b LEFT JOIN categories c ON b.category_id = c.id
            WHERE b.category_id = ? AND b.deleted = FALSE
            ORDER BY b.title
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding books by category", e);
        }
        return list;
    }

    public void save(Book book) {
        String sql = """
            INSERT INTO books (isbn, title, author, publisher, publish_year,
                category_id, total_copies, available_copies, description, cover_image_path)
            VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublisher());
            if (book.getPublishYear() != null) ps.setInt(5, book.getPublishYear());
            else ps.setNull(5, Types.INTEGER);
            ps.setInt(6, book.getCategoryId());
            ps.setInt(7, book.getTotalCopies());
            ps.setInt(8, book.getAvailableCopies());
            ps.setString(9, book.getDescription());
            ps.setString(10, book.getCoverImagePath());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) book.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Error saving book", e);
        }
    }

    public void update(Book book) {
        String sql = """
            UPDATE books SET isbn=?, title=?, author=?, publisher=?, publish_year=?,
                category_id=?, total_copies=?, available_copies=?, description=?, cover_image_path=?
            WHERE id=? AND deleted = FALSE
            """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, book.getIsbn());
            ps.setString(2, book.getTitle());
            ps.setString(3, book.getAuthor());
            ps.setString(4, book.getPublisher());
            if (book.getPublishYear() != null) ps.setInt(5, book.getPublishYear());
            else ps.setNull(5, Types.INTEGER);
            ps.setInt(6, book.getCategoryId());
            ps.setInt(7, book.getTotalCopies());
            ps.setInt(8, book.getAvailableCopies());
            ps.setString(9, book.getDescription());
            ps.setString(10, book.getCoverImagePath());
            ps.setInt(11, book.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating book", e);
        }
    }

    /** Soft delete: ẩn sách khỏi danh sách, giữ FK phiếu mượn. */
    public void softDelete(int id) {
        String sql = "UPDATE books SET deleted = TRUE WHERE id = ? AND deleted = FALSE";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error soft-deleting book", e);
        }
    }

    public void updateAvailableCopies(int bookId, int delta) {
        String sql = "UPDATE books SET available_copies = available_copies + ? WHERE id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating available copies", e);
        }
    }

    public long countTotal() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM books WHERE deleted = FALSE")) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error counting books", e);
        }
        return 0;
    }
}
