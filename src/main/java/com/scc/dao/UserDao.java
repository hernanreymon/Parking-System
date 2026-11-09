
package com.scc.dao;

import com.scc.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private final ActivityLogDao activityLogDao =
            new ActivityLogDao();

    // REGISTER CUSTOMER OR ATTENDANT
    public boolean register(String username, String password, String role)
            throws Exception {

        username = username.trim();
        role = role.toUpperCase().trim();

        if (username.isEmpty() || password.isEmpty()) {
            return false;
        }

        if (!role.equals("CUSTOMER")
                && !role.equals("ATTENDANT")) {
            return false;
        }

        String sql = """
            INSERT INTO users (username, password, roles, status)
            VALUES (?, ?, ?, 'PENDING')
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2,
                    DatabaseInitializer.hashPassword(password));
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

    // CHECK IF ADMIN EXISTS
    public boolean adminExists() throws SQLException {

        String sql = """
            SELECT COUNT(*)
            FROM users
            WHERE roles = 'ADMIN'
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() && rs.getInt(1) > 0;
        }
    }

    // CREATE FIRST ADMIN MANUALLY
    public boolean createAdmin(String username, String password)
            throws Exception {

        username = username.trim();

        if (username.isEmpty() || password.trim().isEmpty()) {
            System.out.println("Username and password are required.");
            return false;
        }

        // MINIMUM PASSWORD: 4 CHARACTERS
        if (password.length() < 4) {
            System.out.println(
                    "Password must contain at least 4 characters."
            );
            return false;
        }

        String sql = """
            INSERT INTO users (username, password, roles, status)
            SELECT ?, ?, 'ADMIN', 'APPROVED'
            WHERE NOT EXISTS (
                SELECT 1 FROM users WHERE roles = 'ADMIN'
            )
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(
                    2,
                    DatabaseInitializer.hashPassword(password)
            );

            boolean created = ps.executeUpdate() > 0;

            if (created) {
                System.out.println(
                        "Admin account created successfully."
                );
                return true;
            }

            System.out.println("An Admin account already exists.");
            return false;

        } catch (SQLException e) {

            if (e.getMessage() != null
                    && e.getMessage().toLowerCase().contains("unique")) {
                System.out.println("Username already exists.");
                return false;
            }

            throw e;
        }
    }

    // LOGIN USER
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

                int userId = rs.getInt("id");
                String storedPassword = rs.getString("password");

                String hashedPassword =
                        DatabaseInitializer.hashPassword(password);

                if (!storedPassword.equals(hashedPassword)
                        && !storedPassword.equals(password)) {
                    return null;
                }

                if (storedPassword.equals(password)
                        && !storedPassword.equals(hashedPassword)) {

                    updatePassword(
                            connection,
                            userId,
                            hashedPassword
                    );
                }

                String status = rs.getString("status");

                if (!"APPROVED".equalsIgnoreCase(status)) {
                    System.out.println(
                            "Your account is not approved yet."
                    );
                    return null;
                }

                return new User(
                        userId,
                        rs.getString("username"),
                        hashedPassword,
                        rs.getString("roles"),
                        status
                );
            }
        }
    }

    // UPDATE PASSWORD
    private void updatePassword(
            Connection connection,
            int id,
            String hash
    ) throws SQLException {

        String sql = """
                UPDATE users
                SET password = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // GET ALL USERS
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

    // GET PENDING USERS
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

    // APPROVE USER WITH AUDIT LOG
    public boolean approveUser(int adminId, int userId)
            throws SQLException {

        String sql = """
            UPDATE users
            SET status = 'APPROVED'
            WHERE id = ? AND status = 'PENDING'
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, userId);

            if (ps.executeUpdate() > 0) {

                activityLogDao.logActivity(
                        adminId,
                        "APPROVE_USER",
                        "Admin (ID: " + adminId
                                + ") approved User (ID: " + userId + ")."
                );

                return true;
            }

            return false;
        }
    }

    // UPDATE USER ROLE AND STATUS
    public boolean updateUserRoleAndStatus(
            int adminId,
            int userId,
            String newRole,
            String newStatus
    ) throws SQLException {

        newRole = newRole.toUpperCase().trim();
        newStatus = newStatus.toUpperCase().trim();

        if (!newRole.equals("ADMIN")
                && !newRole.equals("CUSTOMER")
                && !newRole.equals("ATTENDANT")) {
            System.out.println("Invalid role.");
            return false;
        }

        if (!newStatus.equals("APPROVED")
                && !newStatus.equals("PENDING")) {
            System.out.println("Invalid status.");
            return false;
        }

        String sql = """
            UPDATE users
            SET roles = ?, status = ?
            WHERE id = ?
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, newRole);
            ps.setString(2, newStatus);
            ps.setInt(3, userId);

            if (ps.executeUpdate() > 0) {

                activityLogDao.logActivity(
                        adminId,
                        "UPDATE_USER",
                        "Admin (ID: " + adminId
                                + ") updated User (ID: " + userId
                                + ") role to " + newRole
                                + " and status to " + newStatus + "."
                );

                return true;
            }

            return false;
        }
    }
}
