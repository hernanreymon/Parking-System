
package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import com.scc.model.User;

import java.sql.*;
import java.util.Scanner;

public class AttendantMenu {

    private final Scanner scanner;
    private final User user;

    public AttendantMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
    }

    public void show() {
        boolean running = true;

        while (running) {
            System.out.println("\n================================");
            System.out.println("     PARKING ATTENDANT MENU");
            System.out.println("================================");
            System.out.println("Welcome, " + user.username());
            System.out.println("1. Record Vehicle Entry");
            System.out.println("2. View Parked Vehicles");
            System.out.println("3. Record Vehicle Exit");
            System.out.println("4. Logout");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> recordEntry();
                case "2" -> viewParkedVehicles();
                case "3" -> recordExit();
                case "4" -> running = false;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void recordEntry() {
        System.out.print("Vehicle plate number: ");
        String plate = scanner.nextLine().trim().toUpperCase();

        System.out.print("Vehicle owner's name: ");
        String owner = scanner.nextLine().trim();

        if (plate.isEmpty() || owner.isEmpty()) {
            System.out.println("Please fill in all fields.");
            return;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                String findSlot = """
                    SELECT id, slot_number
                    FROM parking_slots
                    WHERE status = 'AVAILABLE'
                    ORDER BY id
                    LIMIT 1
                """;

                int slotId;
                String slotNumber;

                try (PreparedStatement ps =
                             connection.prepareStatement(findSlot);
                     ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        System.out.println("No available parking slots.");
                        connection.rollback();
                        return;
                    }

                    slotId = rs.getInt("id");
                    slotNumber = rs.getString("slot_number");
                }

                String insertRecord = """
                    INSERT INTO parking_records
                    (vehicle_number, owner_name, slot_id, time_in, status)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'PARKED')
                """;

                try (PreparedStatement ps =
                             connection.prepareStatement(insertRecord)) {
                    ps.setString(1, plate);
                    ps.setString(2, owner);
                    ps.setInt(3, slotId);
                    ps.executeUpdate();
                }

                String updateSlot = """
                    UPDATE parking_slots
                    SET status = 'OCCUPIED'
                    WHERE id = ?
                """;

                try (PreparedStatement ps =
                             connection.prepareStatement(updateSlot)) {
                    ps.setInt(1, slotId);
                    ps.executeUpdate();
                }

                connection.commit();

                System.out.println("Vehicle entry recorded.");
                System.out.println("Assigned parking slot: " + slotNumber);

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Could not record entry: " + e.getMessage());
        }
    }

    private void viewParkedVehicles() {
        String sql = """
            SELECT pr.id, pr.vehicle_number, pr.owner_name,
                   ps.slot_number, pr.time_in
            FROM parking_records pr
            LEFT JOIN parking_slots ps ON pr.slot_id = ps.id
            WHERE pr.status = 'PARKED'
            ORDER BY pr.id
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n---------------------------------------------------------------");
            System.out.printf("%-5s %-15s %-20s %-10s %-20s%n",
                    "ID", "PLATE", "OWNER", "SLOT", "TIME IN");
            System.out.println("---------------------------------------------------------------");

            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-15s %-20s %-10s %-20s%n",
                        rs.getInt("id"),
                        rs.getString("vehicle_number"),
                        rs.getString("owner_name"),
                        rs.getString("slot_number"),
                        rs.getString("time_in"));
            }

            if (!found) {
                System.out.println("No parked vehicles found.");
            }

        } catch (SQLException e) {
            System.out.println("Could not load vehicles: " + e.getMessage());
        }
    }

    private void recordExit() {
        viewParkedVehicles();

        System.out.print("\nEnter parking record ID: ");
        String input = scanner.nextLine().trim();

        int recordId;

        try {
            recordId = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric ID.");
            return;
        }

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                String findRecord = """
                    SELECT slot_id, time_in
                    FROM parking_records
                    WHERE id = ? AND status = 'PARKED'
                """;

                int slotId;
                Timestamp timeIn;

                try (PreparedStatement ps =
                             connection.prepareStatement(findRecord)) {
                    ps.setInt(1, recordId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("Parked vehicle not found.");
                            connection.rollback();
                            return;
                        }

                        slotId = rs.getInt("slot_id");
                        timeIn = rs.getTimestamp("time_in");
                    }
                }

                long elapsedMillis =
                        System.currentTimeMillis() - timeIn.getTime();

                long hours = Math.max(
                        1, (long) Math.ceil(elapsedMillis / 3600000.0)
                );

                double fee = hours * 20.0;

                String updateRecord = """
                    UPDATE parking_records
                    SET time_out = CURRENT_TIMESTAMP,
                        fee = ?, status = 'COMPLETED'
                    WHERE id = ? AND status = 'PARKED'
                """;

                try (PreparedStatement ps =
                             connection.prepareStatement(updateRecord)) {
                    ps.setDouble(1, fee);
                    ps.setInt(2, recordId);
                    ps.executeUpdate();
                }

                String updateSlot = """
                    UPDATE parking_slots
                    SET status = 'AVAILABLE'
                    WHERE id = ?
                """;

                try (PreparedStatement ps =
                             connection.prepareStatement(updateSlot)) {
                    ps.setInt(1, slotId);
                    ps.executeUpdate();
                }

                connection.commit();

                System.out.println("Vehicle exit recorded.");
                System.out.println("Parking duration: " + hours + " hour(s)");
                System.out.printf("Parking fee: ₱%.2f%n", fee);

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println("Could not record exit: " + e.getMessage());
        }
    }
}
