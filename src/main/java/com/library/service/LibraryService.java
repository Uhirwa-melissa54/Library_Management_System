package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.BorrowDAO;
import com.library.dao.MemberDAO;
import com.library.model.Book;
import com.library.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LibraryService {
    private final ConcurrentHashMap<String, Lock> bookLocks = new ConcurrentHashMap<>();
    
    private final BookDAO bookDAO = new BookDAO();
    private final MemberDAO memberDAO = new MemberDAO();
    private final BorrowDAO borrowDAO = new BorrowDAO();

    private Lock getLockForBook(String isbn) {
        return bookLocks.computeIfAbsent(isbn, k -> new ReentrantLock());
    }

    /**
     * Lists all available books from the database.
     */
    public List<Book> listAvailableBooks() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookDAO.selectAvailable(conn);
        } catch (SQLException e) {
            System.err.println("Error fetching available books: " + e.getMessage());
        }
        return new ArrayList<>();
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
            int activeBorrows = borrowDAO.countActiveBorrows(conn, memberId);
            if (activeBorrows >= 5) {
                System.out.println("Librarian: Member " + memberId + " cannot borrow more than 5 books.");
                conn.rollback();
                return false;
            }

            // 2. Check if book is available
            boolean isAvailable = bookDAO.isAvailable(conn, isbn);
            if (!isAvailable) {
                System.out.println("Librarian: Book " + isbn + " is not available.");
                conn.rollback();
                return false;
            }

            // 3. Record the borrowing
            borrowDAO.insertRecord(conn, memberId, isbn);

            // 4. Mark book as unavailable
            bookDAO.updateAvailability(conn, isbn, false);

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
            int rowsAffected = borrowDAO.updateReturnDate(conn, memberId, isbn);
            if (rowsAffected == 0) {
                System.out.println("Librarian: No active borrowing record found for Member " + memberId + " and Book " + isbn);
                conn.rollback();
                return false;
            }

            // 2. Mark book as available
            bookDAO.updateAvailability(conn, isbn, true);

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
        try (Connection conn = DatabaseConnection.getConnection()) {
            bookDAO.insert(conn, book);
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
        try (Connection conn = DatabaseConnection.getConnection()) {
            return memberDAO.insert(conn, name);
        } catch (SQLException e) {
            System.err.println("Error registering member: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Retrieves all books from the database.
     */
    public List<Book> getAllBooks() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookDAO.selectAll(conn);
        } catch (SQLException e) {
            System.err.println("Error fetching all books: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Retrieves all members from the database.
     */
    public List<String> getAllMembers() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return memberDAO.selectAll(conn);
        } catch (SQLException e) {
            System.err.println("Error fetching all members: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Verifies if a member ID exists.
     */
    public boolean memberExists(int memberId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return memberDAO.exists(conn, memberId);
        } catch (SQLException e) {
            System.err.println("Error verifying member: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the history of borrowing transactions.
     */
    public List<String> getBorrowingRecords() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return borrowDAO.selectAllRecords(conn);
        } catch (SQLException e) {
            System.err.println("Error fetching borrowing records: " + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Retrieves books currently borrowed by a specific member.
     */
    public List<Book> getMemberBorrowedBooks(int memberId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return bookDAO.selectMemberBorrowedBooks(conn, memberId);
        } catch (SQLException e) {
            System.err.println("Error fetching member borrowed books: " + e.getMessage());
        }
        return new ArrayList<>();
    }
}
