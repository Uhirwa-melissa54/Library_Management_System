package com.library.task;

import com.library.service.LibraryService;

public class BorrowTask implements Runnable {
    private final LibraryService libraryService;
    private final int memberId;
    private final String isbn;
    private final String librarianName;

    public BorrowTask(LibraryService libraryService, int memberId, String isbn, String librarianName) {
        this.libraryService = libraryService;
        this.memberId = memberId;
        this.isbn = isbn;
        this.librarianName = librarianName;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " (" + librarianName + ") is attempting to process borrow for Member " + memberId + " and Book " + isbn);
        boolean success = libraryService.borrowBook(memberId, isbn);
        if (success) {
            System.out.println(Thread.currentThread().getName() + " (" + librarianName + "): SUCCESS");
        } else {
            System.out.println(Thread.currentThread().getName() + " (" + librarianName + "): FAILED");
        }
    }
}
