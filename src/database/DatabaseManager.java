package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager handles all JDBC interactions with SQLite.
 * Tables:
 *   users(id, username, password, created_at)
 *   messages(id, sender, receiver, content, timestamp)
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:chatapp.db";
    private Connection connection;

    public DatabaseManager() {
        connect();
        createTables();
    }

    // ── Connection ──────────────────────────────────────────────────────────

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DB] Connected to SQLite database.");
        } catch (ClassNotFoundException e) {
            System.err.println("[DB] SQLite JDBC driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DB] Connection error: " + e.getMessage());
        }
    }

    private void createTables() {
        String usersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id       INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT    NOT NULL UNIQUE,
                password TEXT    NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )""";

        String messagesTable = """
            CREATE TABLE IF NOT EXISTS messages (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                sender    TEXT NOT NULL,
                receiver  TEXT,
                content   TEXT NOT NULL,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
            )""";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(messagesTable);
            System.out.println("[DB] Tables ready.");
        } catch (SQLException e) {
            System.err.println("[DB] Table creation error: " + e.getMessage());
        }
    }

    // ── User Authentication ──────────────────────────────────────────────────

    /** Register a new user. Returns true on success, false if username taken. */
    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            System.out.println("[DB] Registered user: " + username);
            return true;
        } catch (SQLException e) {
            System.err.println("[DB] Register failed (duplicate?): " + e.getMessage());
            return false;
        }
    }

    /** Validate login credentials. Returns true if username+password match. */
    public boolean validateLogin(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("[DB] Login validation error: " + e.getMessage());
            return false;
        }
    }

    /** Check if username already exists. */
    public boolean userExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    // ── Message Storage ──────────────────────────────────────────────────────

    /** Save a chat message to the database. receiver=null means broadcast. */
    public void saveMessage(String sender, String receiver, String content) {
        String sql = "INSERT INTO messages (sender, receiver, content) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, content);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DB] Save message error: " + e.getMessage());
        }
    }

    /**
     * Retrieve broadcast chat history (last N messages).
     * Format: "HH:MM:SS | sender: content"
     */
    public List<String> getBroadcastHistory(int limit) {
        String sql = """
            SELECT sender, content, strftime('%H:%M:%S', timestamp) as ts
            FROM messages
            WHERE receiver IS NULL
            ORDER BY id DESC LIMIT ?""";
        List<String> history = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(0, "[" + rs.getString("ts") + "] " +
                                rs.getString("sender") + ": " +
                                rs.getString("content"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] History fetch error: " + e.getMessage());
        }
        return history;
    }

    /**
     * Retrieve private message history between two users (last N messages).
     */
    public List<String> getPrivateHistory(String user1, String user2, int limit) {
        String sql = """
            SELECT sender, receiver, content, strftime('%H:%M:%S', timestamp) as ts
            FROM messages
            WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)
            ORDER BY id DESC LIMIT ?""";
        List<String> history = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user1); ps.setString(2, user2);
            ps.setString(3, user2); ps.setString(4, user1);
            ps.setInt(5, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                history.add(0, "[" + rs.getString("ts") + "] " +
                                rs.getString("sender") + " → " +
                                rs.getString("receiver") + ": " +
                                rs.getString("content"));
            }
        } catch (SQLException e) {
            System.err.println("[DB] Private history error: " + e.getMessage());
        }
        return history;
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("[DB] Close error: " + e.getMessage());
        }
    }
}
