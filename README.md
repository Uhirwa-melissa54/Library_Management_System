#  Library Management System - Rwanda National Digital Library

##  Project Overview

This project is a **console-based Library Management System** developed using **Java (OOP, JDBC, Collections, and Multithreading)**.

It simulates a real-world library where:
- Librarians manage books, members, and borrowing records
- Members can borrow and return books
- All data is stored in a **PostgreSQL relational database**

---

##  System Features

###  Librarian Role
Librarians are system administrators with predefined access.

- Librarian login is based on **hardcoded IDs**
    - Valid IDs: `101, 102, 103, 999`
- No database table is used for librarians (as per requirement constraints)

Librarian can:
- Register new books
- Register new members
- View all books
- View all members
- View borrowing records

---

###  Member Role

Members must be registered in the system (stored in database).

Member can:
- View available books
- Borrow books
- Return books
- View their borrowed books

---

## Database (PostgreSQL)

The system uses the following tables:

### Books
- isbn (Primary Key)
- title
- author
- publication_year
- is_available

### Members
- member_id (Primary Key, auto-generated)
- name

### Borrowing Records
- record_id (Primary Key)
- member_id (Foreign Key)
- isbn (Foreign Key)
- borrow_date
- return_date

---

## ⚙ Technical Concepts Used

- Object-Oriented Programming (OOP)
- Encapsulation
- Java Collections (List, Map)
- Multithreading (ExecutorService, Runnable)
- Thread Safety (Locks / Synchronization)
- JDBC (PostgreSQL connection)
- Transaction Management
- DAO + Service Architecture

---

##  Authentication Design

- **Librarians:** authenticated using hardcoded Java list of valid IDs
- **Members:** authenticated using database verification via JDBC

This design is used due to system constraint that database schema must not be modified.

---

##  Concurrency Design

- Multiple librarians can process requests simultaneously
- Book-level locking is used to prevent two users borrowing the same book
- ExecutorService is used to simulate multiple librarian threads

---

##  How to Run the Project

1. Install PostgreSQL and create a database (e.g., `librarydb`)
2. Run the SQL schema from `schema.sql`
3. Update database credentials in `DatabaseConnection.java`
4. Open project in IntelliJ / Eclipse
5. Run:Application.java
##  Important Notes

- Librarians are not stored in database (hardcoded IDs used)
- Members must be registered before borrowing books
- A member can borrow a maximum of **5 books**
- All borrowing transactions are permanently stored in database
- System ensures thread safety during concurrent borrowing

---