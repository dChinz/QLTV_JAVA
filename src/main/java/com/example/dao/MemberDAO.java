package com.example.dao;

import com.example.config.DatabaseConfig;
import com.example.model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public class MemberDAO {

    private Connection getConn() {
        return DatabaseConfig.getInstance().getConnection();
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setId(rs.getInt("id"));
        m.setMemberCode(rs.getString("member_code"));
        m.setFullName(rs.getString("full_name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        m.setAddress(rs.getString("address"));

        // Oracle trả về java.sql.Date bình thường
        Date joinDate = rs.getDate("join_date");
        if (joinDate != null) m.setJoinDate(joinDate.toLocalDate());

        Date expiryDate = rs.getDate("expiry_date");
        if (expiryDate != null) m.setExpiryDate(expiryDate.toLocalDate());

        m.setStatus(Member.Status.valueOf(rs.getString("status")));

        // Oracle không có BIT/BOOLEAN; cột deleted lưu NUMBER(1): 0/1
        try {
            m.setDeleted(rs.getInt("deleted") == 1);
        } catch (SQLException ignored) {
            m.setDeleted(false);
        }

        // Oracle TIMESTAMP → LocalDateTime
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) m.setCreatedAt(ts.toLocalDateTime());

        return m;
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------
    public List<Member> findAll() {
        List<Member> list = new ArrayList<>();
        // Oracle: dùng 0 thay vì FALSE
        String sql = "SELECT * FROM members WHERE deleted = 0 ORDER BY full_name";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching members", e);
        }
        return list;
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------
    public Optional<Member> findById(int id) {
        String sql = "SELECT * FROM members WHERE id = ? AND deleted = 0";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding member", e);
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // findByCode
    // -------------------------------------------------------------------------
    public Optional<Member> findByCode(String code) {
        String sql = "SELECT * FROM members WHERE member_code = ? AND deleted = 0";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding member by code", e);
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // search  — dùng UPPER() để tìm kiếm không phân biệt hoa/thường trên Oracle
    // -------------------------------------------------------------------------
    public List<Member> search(String keyword) {
        List<Member> list = new ArrayList<>();
        String sql = """
                SELECT * FROM members
                WHERE deleted = 0
                  AND (UPPER(full_name)    LIKE UPPER(?)
                    OR UPPER(member_code)  LIKE UPPER(?)
                    OR UPPER(email)        LIKE UPPER(?)
                    OR phone               LIKE ?)
                ORDER BY full_name
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ps.setString(4, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching members", e);
        }
        return list;
    }

    public void save(Member member) {
        // Bước 1: lấy ID mới từ sequence
        int newId = getNextSequenceValue("members_seq");

        // Bước 2: INSERT kèm ID vừa lấy
        String sql = """
                INSERT INTO members
                    (id, member_code, full_name, email, phone, address,
                     join_date, expiry_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, newId);
            ps.setString(2, member.getMemberCode());
            ps.setString(3, member.getFullName());
            ps.setString(4, member.getEmail());
            ps.setString(5, member.getPhone());
            ps.setString(6, member.getAddress());
            ps.setDate(7, Date.valueOf(member.getJoinDate()));
            ps.setDate(8, Date.valueOf(member.getExpiryDate()));
            ps.setString(9, member.getStatus().name());
            ps.executeUpdate();
            member.setId(newId);   // gán ngược lại object
        } catch (SQLException e) {
            throw new RuntimeException("Error saving member" + e.getMessage(), e);
        }
    }

    public void update(Member member) {
        String sql = """
                UPDATE members
                SET full_name   = ?,
                    email       = ?,
                    phone       = ?,
                    address     = ?,
                    expiry_date = ?,
                    status      = ?
                WHERE id = ? AND deleted = 0
                """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, member.getFullName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getAddress());
            ps.setDate(5, Date.valueOf(member.getExpiryDate()));
            ps.setString(6, member.getStatus().name());
            ps.setInt(7, member.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error updating member", e);
        }
    }

    public void softDelete(int id) {
        String sql = "UPDATE members SET deleted = 1 WHERE id = ? AND deleted = 0";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Error soft-deleting member", e);
        }
    }

    public long countTotal() {
        String sql = "SELECT COUNT(*) FROM members WHERE deleted = 0";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error counting members", e);
        }
        return 0;
    }

    public String generateNextMemberCode() {
        // Lấy member_code mới nhất theo id giảm dần
        String sql = "SELECT member_code FROM members ORDER BY id DESC FETCH FIRST 1 ROWS ONLY";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                String last = rs.getString("member_code");          // vd: "TV005"
                int num = Integer.parseInt(last.replaceAll("[^0-9]", "")) + 1;
                return String.format("TV%03d", num);               // vd: "TV006"
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error generating member code", e);
        }
        return "TV001";
    }

    private int getNextSequenceValue(String sequenceName) {
        String sql = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
            throw new RuntimeException("Sequence returned no value: " + sequenceName);
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching next sequence value: " + sequenceName, e);
        }
    }
}