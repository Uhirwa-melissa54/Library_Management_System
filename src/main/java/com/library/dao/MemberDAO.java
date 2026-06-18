package com.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public int insert(Connection conn, String name) throws SQLException {
        String query = "INSERT INTO members (name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<String> selectAll(Connection conn) throws SQLException {
        List<String> members = new ArrayList<>();
        String query = "SELECT member_id, name FROM members";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                members.add("ID: " + rs.getInt("member_id") + " | Name: " + rs.getString("name"));
            }
        }
        return members;
    }

    public boolean exists(Connection conn, int memberId) throws SQLException {
        String query = "SELECT 1 FROM members WHERE member_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}
