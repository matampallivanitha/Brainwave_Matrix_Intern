import java.sql.*;
import java.util.*;

class GetConnection {
    public static Connection getconnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/ATMINTERFACE", "root", "Vanitha@4829"
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

class BankAccount {
    String name;
    String userId;
    String userPin;
    Scanner sc = new Scanner(System.in);

    public void register() {
        try (Connection conn = GetConnection.getconnection()) {

            // Check for valid and unique name
            while (true) {
                System.out.print("Enter your name: ");
                name = sc.nextLine().trim();

                if (!name.matches("[a-zA-Z ]{2,}")) {
                    System.out.println("Invalid name. Only alphabets allowed, min 2 characters.");
                    continue;
                }

                PreparedStatement nameCheck = conn.prepareStatement("SELECT * FROM users WHERE name = ?");
                nameCheck.setString(1, name);
                ResultSet nameRS = nameCheck.executeQuery();

                if (nameRS.next()) {
                    System.out.println("Name already exists. Please use a different name.");
                } else {
                    break;
                }
            }

            // Check for valid and unique user ID
            while (true) {
                System.out.print("Create User ID: ");
                userId = sc.nextLine().trim();

                if (!userId.matches("[a-zA-Z0-9]+") || userId.length() <4) {
                    System.out.println("Invalid User ID. Must be at least 4 characters, alphanumeric only.");
                    continue;
                }

                PreparedStatement idCheck = conn.prepareStatement("SELECT * FROM users WHERE user_id = ?");
                idCheck.setString(1, userId);
                ResultSet idRS = idCheck.executeQuery();

                if (idRS.next()) {
                    System.out.println("User ID already exists. Try another.");
                } else {
                    break;
                }
            }

            // Set a 4-digit PIN
            while (true) {
                System.out.print("Set 4-digit PIN: ");
                userPin = sc.nextLine().trim();
                if (userPin.matches("\\d{4}")) break;
                System.out.println("Invalid PIN. Must be 4 digits.");
            }

            // Insert into DB
            PreparedStatement ps = conn.prepareStatement("INSERT INTO users (user_id, name, pin) VALUES (?, ?, ?)");
            ps.setString(1, userId);
            ps.setString(2, name);
            ps.setString(3, userPin);
            ps.executeUpdate();

            System.out.println("Registration successful!");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public boolean login() {
        System.out.print("Enter User ID: ");
        userId = sc.nextLine();
        System.out.print("Enter PIN: ");
        userPin = sc.nextLine();

        try (Connection conn = GetConnection.getconnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE user_id = ? AND pin = ?");
            ps.setString(1, userId);
            ps.setString(2, userPin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                name = rs.getString("name");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Login error: " + e.getMessage());
        }

        System.out.println("Invalid login.");
        return false;
    }

    public void deposit() {
        System.out.print("Enter amount to deposit: ");
        float amount = sc.nextFloat();

        try (Connection conn = GetConnection.getconnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE user_id = ?");
            ps.setFloat(1, amount);
            ps.setString(2, userId);
            ps.executeUpdate();

            logTransaction("Deposit", amount, null);
            System.out.println("₹" + amount + " deposited.");
        } catch (Exception e) {
            System.out.println("Deposit failed: " + e.getMessage());
        }
    }

    public void withdraw() {
        System.out.print("Enter amount to withdraw: ");
        float amount = sc.nextFloat();

        try (Connection conn = GetConnection.getconnection()) {
            PreparedStatement check = conn.prepareStatement("SELECT balance FROM users WHERE user_id = ?");
            check.setString(1, userId);
            ResultSet rs = check.executeQuery();
            if (rs.next() && rs.getFloat("balance") >= amount) {
                PreparedStatement ps = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE user_id = ?");
                ps.setFloat(1, amount);
                ps.setString(2, userId);
                ps.executeUpdate();

                logTransaction("Withdraw", amount, null);
                System.out.println("₹" + amount + " withdrawn.");
            } else {
                System.out.println("Insufficient balance.");
            }
        } catch (Exception e) {
            System.out.println("Withdraw failed: " + e.getMessage());
        }
    }

    public void transfer() {
        System.out.print("Enter recipient User ID: ");
        String toUser = sc.next();
        sc.nextLine();
        System.out.print("Enter amount to transfer: ");
        float amount = sc.nextFloat();
        sc.nextLine(); // clear the newline left after reading float

        try (Connection conn = GetConnection.getconnection()) {
            conn.setAutoCommit(false);

            // 1. Check sender's balance
            PreparedStatement checkBalance = conn.prepareStatement(
                    "SELECT balance FROM users WHERE user_id = ?"
            );
            checkBalance.setString(1, userId);
            ResultSet rs = checkBalance.executeQuery();

            if (rs.next() && rs.getFloat("balance") >= amount) {
                // 2. Deduct from sender
                PreparedStatement deduct = conn.prepareStatement(
                        "UPDATE users SET balance = balance - ? WHERE user_id = ?"
                );
                deduct.setFloat(1, amount);
                deduct.setString(2, userId);
                deduct.executeUpdate();

                // 3. Add to recipient
                PreparedStatement add = conn.prepareStatement(
                        "UPDATE users SET balance = balance + ? WHERE user_id = ?"
                );
                add.setFloat(1, amount);
                add.setString(2, toUser);
                int rows = add.executeUpdate();

                if (rows == 0) {
                    conn.rollback();
                    System.out.println("Transfer failed: Recipient not found.");
                    return;
                }

                // 4. Log the transaction
                PreparedStatement log = conn.prepareStatement(
                        "INSERT INTO transactions (user_id, type, amount, recipient) VALUES (?, ?, ?, ?)"
                );
                log.setString(1, userId);
                log.setString(2, "Transfer");
                log.setFloat(3, amount);
                log.setString(4, toUser);
                log.executeUpdate();

                // 5. Get recipient's name
                String recipientName = toUser;
                PreparedStatement getName = conn.prepareStatement(
                        "SELECT name FROM users WHERE user_id = ?"
                );
                getName.setString(1, toUser);
                ResultSet nameRs = getName.executeQuery();
                if (nameRs.next()) {
                    recipientName = nameRs.getString("name");
                }

                conn.commit();
                System.out.println("Transferred ₹" + amount + " to " + recipientName);

            } else {
                conn.rollback();
                System.out.println("Insufficient balance.");
            }

            conn.setAutoCommit(true);

        } catch (Exception e) {
            System.out.println("Transfer failed: " + e.getMessage());
        }
    }

    public void checkBalance() {
        try (Connection conn = GetConnection.getconnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT balance FROM users WHERE user_id = ?");
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Current Balance: ₹" + rs.getFloat("balance"));
            }
        } catch (Exception e) {
            System.out.println("Failed to fetch balance.");
        }
    }

    public void transactionHistory() {
        System.out.println("\nTransaction History:");

        try (Connection conn = GetConnection.getconnection()) {
            String sql = """
            SELECT t.timestamp, t.type, t.amount, t.recipient, u.name AS recipient_name
            FROM transactions t
            LEFT JOIN users u ON t.recipient = u.user_id
            WHERE t.user_id = ?
            ORDER BY t.timestamp DESC
            """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, this.userId);
            ResultSet rs = ps.executeQuery();

            boolean hasData = false;

            while (rs.next()) {
                hasData = true;
                String type = rs.getString("type");
                float amount = rs.getFloat("amount");
                String recipient = rs.getString("recipient");
                String recipientName = rs.getString("recipient_name");
                Timestamp ts = rs.getTimestamp("timestamp");

                if ("Transfer".equalsIgnoreCase(type) && recipient != null) {
                    System.out.println(ts + " | " + type + " | ₹" + amount +
                            " | To: Name: " + (recipientName != null ? recipientName : "Unknown") +
                            ", UserID: " + recipient);
                } else {
                    System.out.println(ts + " | " + type + " | ₹" + amount);
                }
            }

            if (!hasData) {
                System.out.println("No transactions yet.");
            }

        } catch (Exception e) {
            System.out.println("Error retrieving transaction history: " + e.getMessage());
        }
    }

    private void logTransaction(String type, float amount, String recipient) throws SQLException {
        try (Connection conn = GetConnection.getconnection()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO transactions (user_id, type, amount, recipient) VALUES (?, ?, ?, ?)");
            ps.setString(1, userId);
            ps.setString(2, type);
            ps.setFloat(3, amount);
            ps.setString(4, recipient);
            ps.executeUpdate();
        }
    }
}

public class ATMInterface {
    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;

        while (running) {
            System.out.println("\n*** WELCOME TO VANITHA ATM INTERFACE ***");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            int choice = getInput(3);

            switch (choice) {
                case 1 -> {
                    BankAccount acc = new BankAccount();
                    acc.register();
                }
                case 2 -> {
                    BankAccount acc = new BankAccount();
                    if (acc.login()) {
                        boolean session = true;
                        while (session) {
                            System.out.println("\nWelcome, " + acc.name);
                            System.out.println("1. Withdraw");
                            System.out.println("2. Deposit");
                            System.out.println("3. Transfer");
                            System.out.println("4. Check Balance");
                            System.out.println("5. Transaction History");
                            System.out.println("6. Logout");
                            System.out.print("Choose an option: ");
                            int op = getInput(6);
                            switch (op) {
                                case 1 -> acc.withdraw();
                                case 2 -> acc.deposit();
                                case 3 -> acc.transfer();
                                case 4 -> acc.checkBalance();
                                case 5 -> acc.transactionHistory();
                                case 6 -> session = false;
                            }
                        }
                    }
                }
                case 3 -> {
                    System.out.println("Thank you for using VANITHA ATM!");
                    running = false;
                }
            }
        }
    }

    public static int getInput(int limit) {
        int input = 0;
        while (true) {
            try {
                input = sc.nextInt();
                sc.nextLine(); // clear newline
                if (input >= 1 && input <= limit) return input;
                System.out.println("Choose a number between 1 and " + limit);
            } catch (InputMismatchException e) {
                System.out.println("Enter a valid number.");
                sc.next();
            }
        }
    }
}
