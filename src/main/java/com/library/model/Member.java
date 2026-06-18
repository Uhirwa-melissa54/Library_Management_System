package com.library.model;

import java.util.ArrayList;
import java.util.List;

public class Member {
    private int id;
    private String name;
    private List<Book> borrowedBooks;

    public Member(int id, String name) {
        this.id = id;
        this.name = name;
        this.borrowedBooks = new ArrayList<>();
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public List<Book> getBorrowedBooks() { return borrowedBooks; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    
    public void addBorrowedBook(Book book) {
        this.borrowedBooks.add(book);
    }

    public void removeBorrowedBook(Book book) {
        this.borrowedBooks.removeIf(b -> b.getIsbn().equals(book.getIsbn()));
    }

    @Override
    public String toString() {
        return "Member{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", borrowedBooksCount=" + borrowedBooks.size() +
                '}';
    }
}
