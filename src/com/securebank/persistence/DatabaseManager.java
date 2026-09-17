package com.securebank.persistence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:data/securebank.db";

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found on classpath: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initializeSchema() {
        String createTransactions =
                "CREATE TABLE IF NOT EXISTS transactions (" +
                "  transaction_id TEXT PRIMARY KEY," +
                "  source_account TEXT NOT NULL," +
                "  destination_account TEXT NOT NULL," +
                "  amount REAL NOT NULL," +
                "  status TEXT NOT NULL," +
                "  origin_device TEXT," +
                "  created_at TEXT NOT NULL" +
                ")";

        String createAlerts =
                "CREATE TABLE IF NOT EXISTS fraud_alerts (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  transaction_id TEXT NOT NULL," +
                "  rule_triggered TEXT NOT NULL," +
                "  raised_at TEXT NOT NULL" +
                ")";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTransactions);
            stmt.execute(createAlerts);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema: " + e.getMessage());
        }
    }
}
