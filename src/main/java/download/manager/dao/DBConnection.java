package download.manager.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String URL = "jdbc:mysql://localhost:3308/download_manager";
    private static final String USER = "root";
    private static final String PASSWORD = "secret";

    private static Connection connection = null;

    // Private constructor so nobody can do "new DBConnection()"
    private DBConnection() {}

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✓ Connected to MySQL successfully!");
                createTableIfNotExists();
            } catch (SQLException e) {
                System.out.println("✗ DB Connection failed: " + e.getMessage());
            }
        }
        return connection;
    }

    private static void createTableIfNotExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS downloads (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    url TEXT NOT NULL,
                    file_name VARCHAR(255),
                    file_size BIGINT,
                    status VARCHAR(20) DEFAULT 'PENDING',
                    progress DOUBLE DEFAULT 0.0,
                    chunks INT DEFAULT 8,
                    save_path VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("✓ Table ready!");
        } catch (SQLException e) {
            System.out.println("✗ Table creation failed: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("✓ DB Connection closed.");
            } catch (SQLException e) {
                System.out.println("✗ Failed to close connection: " + e.getMessage());
            }
        }
    }
}