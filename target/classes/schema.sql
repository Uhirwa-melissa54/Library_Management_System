-- Database Schema for Library Management System
-- Drop tables if they exist to allow clean re-runs (optional)
DROP TABLE IF EXISTS borrowing_records;
DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS members;

CREATE TABLE books (
    isbn VARCHAR(20) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    publication_year INT,
    is_available BOOLEAN DEFAULT TRUE
);

CREATE TABLE members (
    member_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE borrowing_records (
    record_id SERIAL PRIMARY KEY,
    member_id INT REFERENCES members(member_id),
    isbn VARCHAR(20) REFERENCES books(isbn),
    borrow_date DATE NOT NULL,
    return_date DATE
);

-- Insert some sample data
INSERT INTO books (isbn, title, author, publication_year, is_available) VALUES
('978-0134685991', 'Effective Java', 'Joshua Bloch', 2017, TRUE),
('978-0596009205', 'Head First Java', 'Kathy Sierra', 2005, TRUE),
('978-0201633610', 'Design Patterns', 'Erich Gamma', 1994, TRUE),
('978-0132350884', 'Clean Code', 'Robert C. Martin', 2008, TRUE),
('978-0321356680', 'Effective C++', 'Scott Meyers', 2005, TRUE),
('978-0137081073', 'Clean Coder', 'Robert C. Martin', 2011, TRUE);

INSERT INTO members (name) VALUES
('Alice Smith'),
('Bob Jones'),
('Charlie Brown');
