package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    // XAMPP default MySQL configuration
    private static final String URL = "jdbc:mysql://localhost:3306/packpal_db";
    private static final String USER = "root";  // XAMPP default username
    private static final String PASSWORD = "";   // XAMPP default password is EMPTY

    private static Connection conn = null;

    private DatabaseConfig() {}

    public static Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Database connected successfully!");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found", e);
            }
        }
        return conn;
    }

    // In DatabaseConfig.java - ensure this method exists
public static boolean testConnection() {
    try (Connection conn = getConnection()) {
        // Test with a simple query
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
        }
        System.out.println("✅ Database connection test successful");
        return true;
    } catch (SQLException e) {
        System.err.println("❌ Database connection test failed: " + e.getMessage());
        return false;
    }
}

