package com.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BorrowDAO {

    public int countActiveBorrows(Connection conn, int memberId) throws SQLException {
        String query = "SELECT COUNT(*) FROM borrowing_records WHERE member_id = ? AND return_date IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void insertRecord(Connection conn, int memberId, String isbn) throws SQLException {
        String query = "INSERT INTO borrowing_records (member_id, isbn, borrow_date) VALUES (?, ?, CURRENT_DATE)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, memberId);
            stmt.setString(2, isbn);
            stmt.executeUpdate();
        }
    }

    public int updateReturnDate(Connection conn, int memberId, String isbn) throws SQLException {
        String query = "UPDATE borrowing_records SET return_date = CURRENT_DATE WHERE member_id = ? AND isbn = ? AND return_date IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, memberId);
            stmt.setString(2, isbn);
            return stmt.executeUpdate();
        }
    }

    public List<String> selectAllRecords(Connection conn) throws SQLException {
        List<String> records = new ArrayList<>();
        String query = "SELECT record_id, member_id, isbn, borrow_date, return_date FROM borrowing_records";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                records.add(String.format("Record: %d | Member: %d | ISBN: %s | Borrowed: %s | Returned: %s",
                        rs.getInt("record_id"),
                        rs.getInt("member_id"),
                        rs.getString("isbn"),
                        rs.getDate("borrow_date"),
                        rs.getDate("return_date") != null ? rs.getDate("return_date") : "Not Returned"
                ));
            }
        }
        return records;
    }
}
