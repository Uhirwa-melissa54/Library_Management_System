package com.library;

import com.library.model.Book;
import com.library.service.LibraryService;
import com.library.task.BorrowTask;
import com.library.util.DatabaseConnection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DMain {
    public static void main(String[] args) {
        System.out.println("Initializing Library Management System...");

        try {
            // 1. Initialize Database Schema
            initializeDatabase();

            LibraryService libraryService = new LibraryService();

            // 2. Display available books initially
            System.out.println("\n--- Available Books ---");
            List<Book> availableBooks = libraryService.listAvailableBooks();
            availableBooks.forEach(System.out::println);
            System.out.println("-----------------------\n");

            // 3. Setup ExecutorService for multithreading (simulate 3 librarians)
            ExecutorService executor = Executors.newFixedThreadPool(3);

            // Member 1 (Alice) tries to borrow "Effective Java" (978-0134685991)
            // Two librarians try to process this concurrently to test thread safety.
            executor.submit(new BorrowTask(libraryService, 1, "978-0134685991", "Librarian A"));
            executor.submit(new BorrowTask(libraryService, 2, "978-0134685991", "Librarian B")); // Should fail, Book already borrowed by Librarian A

            // Member 1 tries to borrow multiple books concurrently to test limit (but we only seed 6 books)
            executor.submit(new BorrowTask(libraryService, 1, "978-0596009205", "Librarian C"));
            executor.submit(new BorrowTask(libraryService, 1, "978-0201633610", "Librarian A"));
            executor.submit(new BorrowTask(libraryService, 1, "978-0132350884", "Librarian B"));
            executor.submit(new BorrowTask(libraryService, 1, "978-0321356680", "Librarian C"));
            
            // By now Alice has 5 books. Next attempt should fail.
            executor.submit(new BorrowTask(libraryService, 1, "978-0137081073", "Librarian A"));

            // Shut down executor and wait for tasks to complete
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            // 4. Display available books after operations
            System.out.println("\n--- Available Books After Operations ---");
            availableBooks = libraryService.listAvailableBooks();
            availableBooks.forEach(System.out::println);
            System.out.println("----------------------------------------\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void initializeDatabase() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Read schema.sql from resources
            InputStream is = Main.class.getResourceAsStream("/schema.sql");
            if (is == null) {
                System.out.println("Could not find schema.sql, assuming tables already exist.");
                return;
            }
            
            String sql = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));
            
            stmt.execute(sql);
            System.out.println("Database tables and initial data populated.");
        }
    }
}
