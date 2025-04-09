CREATE DATABASE ATMINTERFACE;
USE ATMINTERFACE;

-- USERS TABLE
CREATE TABLE users (
    user_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    pin VARCHAR(10) NOT NULL,
    balance DECIMAL(10,2) DEFAULT 0.00
);

select * from users;

-- Make sure user names are unique too
ALTER TABLE users ADD CONSTRAINT unique_name UNIQUE(name);

-- TRANSACTIONS TABLE
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
    users r ON t.recipient = r.user_id   -- join for recipient name
LEFT JOIN 
    users s ON t.user_id = s.user_id     -- join for sender name
ORDER BY 
    t.timestamp DESC;