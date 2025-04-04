# Brainwave_Matrix_Intern
# Java ATM Interface

A fully functional ATM interface built using Java and MySQL. This command-line application allows users to register, log in, and perform basic banking operations like deposit, withdrawal, fund transfer, checking balance, and viewing transaction history.

# Features
-  User Registration with validations
-  Secure Login with 4-digit PIN
-  Deposit & Withdraw
-  Transfer funds to other users
-  Check Account Balance
-  View detailed Transaction History
-  JDBC + MySQL-based data persistence

# Technologies Used
- Java (JDK 17 or above)
- JDBC (Java Database Connectivity)
- MySQL
- IntelliJ IDEA (for development, optional)

# Create the MySQL Database
Login to MySQL and run:

# Create databse
CREATE DATABASE ATMINTERFACE;

# Use database
USE ATMINTERFACE;

# USERS TABLE

CREATE TABLE users (
    user_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    pin VARCHAR(10) NOT NULL,
    balance DECIMAL(10,2) DEFAULT 0.00
);

ALTER TABLE users ADD CONSTRAINT unique_name UNIQUE(name);

# TRANSACTIONS TABLE

CREATE TABLE transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50),
    type VARCHAR(20),
    amount DECIMAL(10,2),
    recipient VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

SELECT 
    t.timestamp,
    t.type,
    t.amount,
    t.user_id AS sender_id,
    s.name AS sender_name,
    t.recipient AS recipient_id,
    r.name AS recipient_name
FROM 
    transactions t
LEFT JOIN 
    users r ON t.recipient = r.user_id   
LEFT JOIN 
    users s ON t.user_id = s.user_id    
ORDER BY 
    t.timestamp DESC;

# Output Screens

![Screenshot 2025-04-04 191015](https://github.com/user-attachments/assets/677df0e2-3beb-45c8-9279-3a5a71d91028)
![Screenshot 2025-04-04 191026](https://github.com/user-attachments/assets/8dca6ae9-2f1c-4fe6-b880-1d180eb80ac8)
![Screenshot 2025-04-04 191046](https://github.com/user-attachments/assets/29e07967-fa76-47ad-b9ef-c8440dfa567b)


