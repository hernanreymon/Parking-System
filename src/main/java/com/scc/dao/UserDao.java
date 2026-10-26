
package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public boolean register(String username, String password, String role)
            throws Exception {

        username = username.trim();

        if (username.isEmpty() || password.isEmpty()) {
            return false;
        }

        if (!role.equals("CUSTOMER") && !role.equals("ATTENDANT")) {
            return false;
        }

        String sql = """
            INSERT INTO users (username, password, roles, status)
            VALUES (?, ?, ?, 'PENDING')
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, DatabaseInitializer.hashPassword(password));
            ps.setString(3, role);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("unique")) {
                return false;
            }
            throw e;
        }
    }

    public User login(String username, String password) throws Exception {
        String sql = """
            SELECT id, username, password, roles, status
            FROM users
            WHERE username = ?
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                String storedPassword = rs.getString("password");
                String hashedPassword =
                        DatabaseInitializer.hashPassword(password);

                // Supports SHA-256 passwords and older plain-text records.
                if (!storedPassword.equals(hashedPassword)
                        && !storedPassword.equals(password)) {
                    return null;
                }

                // Upgrade a matching plain-text password to SHA-256.
                if (storedPassword.equals(password)
                        && !storedPassword.equals(hashedPassword)) {
                    updatePassword(connection, rs.getInt("id"),
                            hashedPassword);
                }

                String status = rs.getString("status");

                if (!"APPROVED".equalsIgnoreCase(status)) {
                    System.out.println(
                            "Your account is not approved yet."
                    );
                    return null;
                }

                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        hashedPassword,
                        rs.getString("roles"),
                        status
                );
            }
        }
    }

    private void updatePassword(Connection connection, int id, String hash)
            throws SQLException {

        String sql = "UPDATE users SET password = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = """
            SELECT id, username, password, roles, status
            FROM users
            ORDER BY id
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }
        }

        return users;
    }

    public List<User> getPendingUsers() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = """
            SELECT id, username, password, roles, status
            FROM users
            WHERE status = 'PENDING'
            ORDER BY id
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("roles"),
                        rs.getString("status")
                ));
            }
        }

        return users;
    }

    public boolean approveUser(int userId) throws SQLException {
        String sql = """
            UPDATE users
            SET status = 'APPROVED'
            WHERE id = ? AND status = 'PENDING'
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
