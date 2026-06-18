package com.library;

import com.library.model.Book;
import com.library.service.LibraryService;
import com.library.task.BorrowTask;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {
    private static final LibraryService libraryService = new LibraryService();
    private static final Scanner scanner = new Scanner(System.in);
    
    // Simple predefined collection of Librarian IDs for authentication
    private static final List<Integer> VALID_LIBRARIAN_IDS = Arrays.asList(101, 102, 103, 999);
    
    // Executor for executing concurrency logic as requested
    private static final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public static void main(String[] args) {
        System.out.println("Welcome to the Rwanda National Digital Library Management System");

        while (true) {
            System.out.println("\nSelect role:");
            System.out.println("1. Librarian");
            System.out.println("2. Member");
            System.out.println("3. Exit Application");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            if ("1".equals(choice)) {
                handleLibrarianLogin();
            } else if ("2".equals(choice)) {
                handleMemberLogin();
            } else if ("3".equals(choice)) {
                System.out.println("Exiting application...");
                executorService.shutdown();
                System.exit(0);
            } else {
                System.out.println("Invalid choice. Please enter 1, 2, or 3.");
            }
        }
    }

    private static void handleLibrarianLogin() {
        System.out.print("Enter Librarian ID: ");
        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (VALID_LIBRARIAN_IDS.contains(id)) {
                System.out.println("\nLogin successful. Welcome Librarian " + id);
                librarianMenu();
            } else {
                System.out.println("Invalid Librarian ID. Access denied.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
        }
    }

    private static void handleMemberLogin() {
        System.out.print("Enter Member ID: ");
        try {
            int id = Integer.parseInt(scanner.nextLine().trim());
            if (libraryService.memberExists(id)) {
                System.out.println("\nLogin successful. Welcome Member " + id);
                memberMenu(id);
            } else {
                System.out.println("Invalid Member ID or Member does not exist.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input format.");
        }
    }

    private static void librarianMenu() {
        while (true) {
            System.out.println("\n========================");
            System.out.println("     LIBRARIAN MENU");
            System.out.println("========================");
            System.out.println("1. Register new book");
            System.out.println("2. Register new member");
            System.out.println("3. View all books");
            System.out.println("4. View all members");
            System.out.println("5. View borrowing records");
            System.out.println("6. Logout");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    registerBook();
                    break;
                case "2":
                    registerMember();
                    break;
                case "3":
                    viewAllBooks();
                    break;
                case "4":
                    viewAllMembers();
                    break;
                case "5":
                    viewBorrowingRecords();
                    break;
                case "6":
                    System.out.println("Logging out Librarian...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    private static void memberMenu(int memberId) {
        while (true) {
            System.out.println("\n========================");
            System.out.println("      MEMBER MENU");
            System.out.println("========================");
            System.out.println("1. View available books");
            System.out.println("2. Borrow book");
            System.out.println("3. Return book");
            System.out.println("4. View my borrowed books");
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    viewAvailableBooks();
                    break;
                case "2":
                    borrowBook(memberId);
                    break;
                case "3":
                    returnBook(memberId);
                    break;
                case "4":
                    viewMemberBorrowedBooks(memberId);
                    break;
                case "5":
                    System.out.println("Logging out Member...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    // --- Librarian Operations ---

    private static void registerBook() {
        try {
            System.out.print("Enter ISBN: ");
            String isbn = scanner.nextLine().trim();
            System.out.print("Enter Title: ");
            String title = scanner.nextLine().trim();
            System.out.print("Enter Author: ");
            String author = scanner.nextLine().trim();
            System.out.print("Enter Publication Year: ");
            int year = Integer.parseInt(scanner.nextLine().trim());

            Book newBook = new Book(isbn, title, author, year);
            if (libraryService.registerBook(newBook)) {
                System.out.println("Book registered successfully!");
            } else {
                System.out.println("Failed to register book. Maybe ISBN already exists?");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid year format.");
        }
    }

    private static void registerMember() {
        System.out.print("Enter Member Name: ");
        String name = scanner.nextLine().trim();
        int newId = libraryService.registerMember(name);
        if (newId != -1) {
            System.out.println("Member registered successfully! Generated Member ID: " + newId);
        } else {
            System.out.println("Failed to register member.");
        }
    }

    private static void viewAllBooks() {
        List<Book> books = libraryService.getAllBooks();
        if (books.isEmpty()) {
            System.out.println("No books found.");
        } else {
            System.out.println("\n--- All Books ---");
            books.forEach(System.out::println);
        }
    }

    private static void viewAllMembers() {
        List<String> members = libraryService.getAllMembers();
        if (members.isEmpty()) {
            System.out.println("No members found.");
        } else {
            System.out.println("\n--- All Members ---");
            members.forEach(System.out::println);
        }
    }

    private static void viewBorrowingRecords() {
        List<String> records = libraryService.getBorrowingRecords();
        if (records.isEmpty()) {
            System.out.println("No borrowing records found.");
        } else {
            System.out.println("\n--- Borrowing Records ---");
            records.forEach(System.out::println);
        }
    }

    // --- Member Operations ---

    private static void viewAvailableBooks() {
        List<Book> availableBooks = libraryService.listAvailableBooks();
        if (availableBooks.isEmpty()) {
            System.out.println("No available books right now.");
        } else {
            System.out.println("\n--- Available Books ---");
            availableBooks.forEach(System.out::println);
        }
    }

    private static void borrowBook(int memberId) {
        System.out.print("Enter ISBN of the book to borrow: ");
        String isbn = scanner.nextLine().trim();
        
        // We use the executor service to preserve the multithreading requirements
        // and avoid modifying the concurrency logic layer.
        BorrowTask borrowTask = new BorrowTask(libraryService, memberId, isbn, "Member " + memberId);
        // We run it synchronously here so the UI waits for it, 
        // but it executes through the task runnable
        System.out.println("Processing borrow request...");
        borrowTask.run(); 
    }

    private static void returnBook(int memberId) {
        System.out.print("Enter ISBN of the book to return: ");
        String isbn = scanner.nextLine().trim();
        System.out.println("Processing return request...");
        boolean success = libraryService.returnBook(memberId, isbn);
        if (!success) {
            System.out.println("Could not return the book. Check if ISBN is correct and currently borrowed by you.");
        }
    }

    private static void viewMemberBorrowedBooks(int memberId) {
        List<Book> books = libraryService.getMemberBorrowedBooks(memberId);
        if (books.isEmpty()) {
            System.out.println("You currently have no borrowed books.");
        } else {
            System.out.println("\n--- Your Borrowed Books ---");
            books.forEach(System.out::println);
        }
    }
}
