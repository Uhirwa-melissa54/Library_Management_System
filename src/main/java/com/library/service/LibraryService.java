package com.library.service;

import com.library.model.Book;
import com.library.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LibraryService {
    // Map to keep track of locks per book ISBN, ensuring multiple librarians 
    // can process different books concurrently, but not the same book.
    private final ConcurrentHashMap<String, Lock> bookLocks = new ConcurrentHashMap<>();

    private Lock getLockForBook(String isbn) {
        return bookLocks.computeIfAbsent(isbn, k -> new ReentrantLock());
    }

    /**
     * Lists all available books from the database.
     */
    public List<Book> listAvailableBooks() {
        List<Book> availableBooks = new ArrayList<>();
        String query = "SELECT isbn, title, author, publication_year FROM books WHERE is_available = TRUE";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                availableBooks.add(new Book(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("publication_year")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching available books: " + e.getMessage());
        }
        return availableBooks;
    }

    /**
     * Borrows a book for a member. Includes thread safety and transaction management.
     */
    public boolean borrowBook(int memberId, String isbn) {
        Lock bookLock = getLockForBook(isbn);
        bookLock.lock(); // Prevent concurrent borrows for the same book
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Check if member exists and has borrowed less than 5 books
            String countQuery = "SELECT COUNT(*) FROM borrowing_records WHERE member_id = ? AND return_date IS NULL";
            try (PreparedStatement countStmt = conn.prepareStatement(countQuery)) {
                countStmt.setInt(1, memberId);
                ResultSet rs = countStmt.executeQuery();
                if (rs.next() && rs.getInt(1) >= 5) {
                    System.out.println("Librarian: Member " + memberId + " cannot borrow more than 5 books.");
                    conn.rollback();
                    return false;
                }
            }

            // 2. Check if book is available
            String checkBookQuery = "SELECT is_available FROM books WHERE isbn = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkBookQuery)) {
                checkStmt.setString(1, isbn);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next() || !rs.getBoolean("is_available")) {
                    System.out.println("Librarian: Book " + isbn + " is not available.");
                    conn.rollback();
                    return false;
                }
            }

            // 3. Record the borrowing
            String borrowQuery = "INSERT INTO borrowing_records (member_id, isbn, borrow_date) VALUES (?, ?, CURRENT_DATE)";
            try (PreparedStatement borrowStmt = conn.prepareStatement(borrowQuery)) {
                borrowStmt.setInt(1, memberId);
                borrowStmt.setString(2, isbn);
                borrowStmt.executeUpdate();
            }

            // 4. Mark book as unavailable
            String updateBookQuery = "UPDATE books SET is_available = FALSE WHERE isbn = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateBookQuery)) {
                updateStmt.setString(1, isbn);
                updateStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
            System.out.println("Librarian: Successfully processed borrow request. Member " + memberId + " borrowed Book " + isbn);
            return true;

        } catch (SQLException e) {
            System.err.println("Database error during borrow: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
            bookLock.unlock(); // Release lock
        }
    }

    /**
     * Returns a borrowed book.
     */
    public boolean returnBook(int memberId, String isbn) {
        Lock bookLock = getLockForBook(isbn);
        bookLock.lock();
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Update return date
            String updateRecordQuery = "UPDATE borrowing_records SET return_date = CURRENT_DATE WHERE member_id = ? AND isbn = ? AND return_date IS NULL";
            try (PreparedStatement updateRecStmt = conn.prepareStatement(updateRecordQuery)) {
                updateRecStmt.setInt(1, memberId);
                updateRecStmt.setString(2, isbn);
                int rowsAffected = updateRecStmt.executeUpdate();
                if (rowsAffected == 0) {
                    System.out.println("Librarian: No active borrowing record found for Member " + memberId + " and Book " + isbn);
                    conn.rollback();
                    return false;
                }
            }

            // 2. Mark book as available
            String updateBookQuery = "UPDATE books SET is_available = TRUE WHERE isbn = ?";
            try (PreparedStatement updateBookStmt = conn.prepareStatement(updateBookQuery)) {
                updateBookStmt.setString(1, isbn);
                updateBookStmt.executeUpdate();
            }

            conn.commit();
            System.out.println("Librarian: Successfully processed return request. Member " + memberId + " returned Book " + isbn);
            return true;

        } catch (SQLException e) {
            System.err.println("Database error during return: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("Error rolling back transaction: " + ex.getMessage());
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
            bookLock.unlock();
        }
    }

    /**
     * Registers a new book in the database.
     */
    public boolean registerBook(Book book) {
        String query = "INSERT INTO books (isbn, title, author, publication_year, is_available) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, book.getIsbn());
            stmt.setString(2, book.getTitle());
            stmt.setString(3, book.getAuthor());
            stmt.setInt(4, book.getPublicationYear());
            stmt.setBoolean(5, true);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registering book: " + e.getMessage());
            return false;
        }
    }

    /**
     * Registers a new member in the database.
     */
    public int registerMember(String name) {
        String query = "INSERT INTO members (name) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error registering member: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves all books from the database.
     */
    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        String query = "SELECT isbn, title, author, publication_year FROM books";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                books.add(new Book(
                        rs.getString("isbn"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getInt("publication_year")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all books: " + e.getMessage());
        }
        return books;
    }

    /**
     * Retrieves all members from the database.
     */
    public List<String> getAllMembers() {
        List<String> members = new ArrayList<>();
        String query = "SELECT member_id, name FROM members";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                members.add("ID: " + rs.getInt("member_id") + " | Name: " + rs.getString("name"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all members: " + e.getMessage());
        }
        return members;
    }

    /**
     * Verifies if a member ID exists.
     */
    public boolean memberExists(int memberId) {
        String query = "SELECT 1 FROM members WHERE member_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, memberId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error verifying member: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the history of borrowing transactions.
     */
    public List<String> getBorrowingRecords() {
        List<String> records = new ArrayList<>();
        String query = "SELECT record_id, member_id, isbn, borrow_date, return_date FROM borrowing_records";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
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
        } catch (SQLException e) {
            System.err.println("Error fetching borrowing records: " + e.getMessage());
        }
        return records;
    }

    /**
     * Retrieves books currently borrowed by a specific member.
     */
    public List<Book> getMemberBorrowedBooks(int memberId) {
        List<Book> books = new ArrayList<>();
        String query = "SELECT b.isbn, b.title, b.author, b.publication_year FROM books b " +
                       "JOIN borrowing_records r ON b.isbn = r.isbn " +
                       "WHERE r.member_id = ? AND r.return_date IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
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
        } catch (SQLException e) {
            System.err.println("Error fetching member borrowed books: " + e.getMessage());
        }
        return books;
    }
}
