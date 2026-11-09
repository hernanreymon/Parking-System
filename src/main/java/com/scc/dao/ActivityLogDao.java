
package com.scc.dao;

import com.scc.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {

    public boolean logActivity(int userId, String actionType,
                               String description) {

        String sql = """
                INSERT INTO activity_logs
                (user_id, action_type, description)
                VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, actionType);
            pstmt.setString(3, description);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("Audit Log Error: " + e.getMessage());
            return false;
        }
    }

    public List<ActivityLog> getAllLogs() {

        List<ActivityLog> logs = new ArrayList<>();

        String sql = """
                SELECT id, user_id, action_type, description, timestamp
                FROM activity_logs
                ORDER BY id DESC
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                logs.add(new ActivityLog(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("action_type"),
                        rs.getString("description"),
                        rs.getString("timestamp")
                ));
            }

        } catch (SQLException e) {
            System.out.println("Retrieve Log Error: " + e.getMessage());
        }

        return logs;
    }

    public void displayLogs() {

        List<ActivityLog> logs = getAllLogs();

        System.out.println(
                "\n========== PARKING SYSTEM AUDIT LOGS =========="
        );

        System.out.printf("%-5s %-8s %-22s %-45s %-20s%n",
                "ID", "USER ID", "ACTION", "DESCRIPTION", "TIMESTAMP");

        System.out.println(
                "--------------------------------------------------------------------------------"
        );

        for (ActivityLog log : logs) {
            System.out.printf("%-5d %-8d %-22s %-45s %-20s%n",
                    log.id(),
                    log.userId(),
                    log.actionType(),
                    log.description(),
                    log.timestamp());
        }

        System.out.println("================================================");
        System.out.println("Total Logs: " + logs.size());
    }
}
