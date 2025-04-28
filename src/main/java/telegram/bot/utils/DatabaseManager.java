package telegram.bot.utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !isValid(connection)) {
            // Try loading from environment first
            String url = System.getenv("DATABASE_URL");
            System.out.println("Connecting to " + url);
            String user = System.getenv("PGUSER");
            System.out.println("PGUSER: " + user);
            String password = System.getenv("PGPASSWORD");
            System.out.println("PGPASSWORD: " + password);
            // If missing, load from .env file (for local dev)
            if (url == null || user == null || password == null) {
                Dotenv dotenv = Dotenv.load();
                url = dotenv.get("DATABASE_URL");
                user = dotenv.get("PGUSER");
                password = dotenv.get("PGPASSWORD");
            }

            connection = DriverManager.getConnection(url, user, password);
            System.out.println("🔗 Connected to PostgreSQL successfully: " + url);
        }
        return connection;
    }

    private static boolean isValid(Connection conn) {
        try {
            return conn.isValid(2); // timeout in seconds
        } catch (SQLException e) {
            return false;
        }
    }
}

