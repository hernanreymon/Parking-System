
package com.scc.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void initialize() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    roles TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'PENDING'
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS parking_slots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    slot_number TEXT NOT NULL UNIQUE,
                    status TEXT NOT NULL DEFAULT 'AVAILABLE'
                )
            """);

            statement.execute("""
                CREATE TABLE IF NOT EXISTS parking_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    vehicle_number TEXT NOT NULL,
                    owner_name TEXT NOT NULL,
                    slot_id INTEGER,
                    time_in TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    time_out TEXT,
                    fee REAL NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'PARKED',
                    FOREIGN KEY (slot_id) REFERENCES parking_slots(id)
                )
            """);

            createDefaultSlots(connection);
            createDefaultAdmin(connection);
        }
    }

    private static void createDefaultSlots(Connection connection)
            throws Exception {

        String sql = """
            INSERT OR IGNORE INTO parking_slots (slot_number, status)
            VALUES (?, 'AVAILABLE')
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 1; i <= 10; i++) {
                ps.setString(1, String.format("P%02d", i));
                ps.executeUpdate();
            }
        }
    }

    private static void createDefaultAdmin(Connection connection)
            throws Exception {

        String checkSql = "SELECT COUNT(*) FROM users";

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(checkSql)) {

            if (rs.next() && rs.getInt(1) == 0) {
                String sql = """
                    INSERT INTO users (username, password, roles, status)
                    VALUES (?, ?, 'ADMIN', 'APPROVED')
                """;

                try (PreparedStatement ps =
                             connection.prepareStatement(sql)) {

                    ps.setString(1, "admin");
                    ps.setString(2, hashPassword("Admin123!"));
                    ps.executeUpdate();
                }

                System.out.println("Default administrator account created.");
            }
        }
    }

    public static String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(
                password.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder result = new StringBuilder();

        for (byte b : hash) {
            result.append(String.format("%02x", b & 0xff));
        }

        return result.toString();
    }
}
