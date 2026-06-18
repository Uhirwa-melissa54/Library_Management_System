package com.library.dao;

import com.library.model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public void insert(Connection conn, Book book) throws SQLException {
        String query = "INSERT INTO books (isbn, title, author, publication_year, is_available) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, book.getIsbn());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getAuthor());
            stmt.setInt(4, book.getPublicationYear());
            stmt.setBoolean(5, true);
            stmt.executeUpdate();
        }
    }

    public List<Book> selectAll(Connection conn) throws SQLException {
        List<Book> books = new ArrayList<>();
        String query = "SELECT isbn, title, author, publication_year, is_available FROM books";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("publication_year")
                ));
            }
        }
        return books;
    }

    public List<Book> selectAvailable(Connection conn) throws SQLException {
        List<Book> books = new ArrayList<>();
        String query = "SELECT isbn, title, author, publication_year FROM books WHERE is_available = TRUE";
        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("publication_year")
                ));
            }
        }
        return books;
    }

    public boolean isAvailable(Connection conn, String isbn) throws SQLException {
        String query = "SELECT is_available FROM books WHERE isbn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, isbn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("is_available");
                }
            }
        }
        return false;
    }

    public void updateAvailability(Connection conn, String isbn, boolean isAvailable) throws SQLException {
        String query = "UPDATE books SET is_available = ? WHERE isbn = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, isAvailable);
            stmt.setString(2, isbn);
            stmt.executeUpdate();
        }
    }

    public List<Book> selectMemberBorrowedBooks(Connection conn, int memberId) throws SQLException {
        List<Book> books = new ArrayList<>();
        String query = "SELECT b.isbn, b.title, b.author, b.publication_year FROM books b " +
                       "JOIN borrowing_records r ON b.isbn = r.isbn " +
                       "WHERE r.member_id = ? AND r.return_date IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    books.add(new Book(
                            rs.getString("isbn"),
                            rs.getString("title"),
                            rs.getString("author"),
                            rs.getInt("publication_year")
                    ));
                }
            }
        }
        return books;
    }
}
