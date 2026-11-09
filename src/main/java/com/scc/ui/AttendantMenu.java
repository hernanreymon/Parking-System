
package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import com.scc.dao.ParkingRecordDao;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class AttendantMenu {

    private final Scanner scanner;
    private final User user;
    private final ParkingRecordDao parkingRecordDao;

    public AttendantMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
        this.parkingRecordDao = new ParkingRecordDao();
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

                case "4" -> {
                    running = false;
                    System.out.println("Attendant logged out.");
                }

                default ->
                        System.out.println("Invalid choice.");
            }
        }
    }

    // RECORD VEHICLE ENTRY
    private void recordEntry() {

        System.out.println("\n========== VEHICLE ENTRY ==========");

        System.out.print("Vehicle plate number: ");
        String plate = scanner.nextLine().trim().toUpperCase();

        System.out.print("Vehicle owner's name: ");
        String owner = scanner.nextLine().trim();

        if (plate.isEmpty() || owner.isEmpty()) {
            System.out.println("Please fill in all fields.");
            return;
        }

        // DISPLAY AVAILABLE SLOTS
        parkingRecordDao.displayAvailableSlots();

        System.out.print(
                "\nEnter customer's preferred parking slot: "
        );

        String preferredSlot =
                scanner.nextLine().trim().toUpperCase();

        if (preferredSlot.isEmpty()) {
            System.out.println("Please select a parking slot.");
            return;
        }

        // ASSIGN SELECTED SLOT
        parkingRecordDao.parkVehicle(
                user.id(),
                plate,
                owner,
                preferredSlot
        );
    }

    // VIEW PARKED VEHICLES
    private void viewParkedVehicles() {

        String sql = """
            SELECT pr.id, pr.vehicle_number, pr.owner_name,
                   ps.slot_number, pr.time_in
            FROM parking_records pr
            LEFT JOIN parking_slots ps ON pr.slot_id = ps.id
            WHERE pr.status = 'PARKED'
            ORDER BY pr.id
        """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement ps =
                     connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n==============================================================");
            System.out.printf(
                    "%-5s %-15s %-20s %-10s %-20s%n",
                    "ID", "PLATE", "OWNER", "SLOT", "TIME IN"
            );
            System.out.println("==============================================================");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.printf(
                        "%-5d %-15s %-20s %-10s %-20s%n",
                        rs.getInt("id"),
                        rs.getString("vehicle_number"),
                        rs.getString("owner_name"),
                        rs.getString("slot_number"),
                        rs.getString("time_in")
                );
            }

            if (!found) {
                System.out.println("No parked vehicles found.");
            }

            System.out.println("==============================================================");

        } catch (SQLException e) {

            System.out.println(
                    "Could not load vehicles: " + e.getMessage()
            );
        }
    }

    // RECORD VEHICLE EXIT
    private void recordExit() {

        viewParkedVehicles();

        System.out.print("\nEnter parking record ID: ");
        String input = scanner.nextLine().trim();

        int recordId;

        try {

            recordId = Integer.parseInt(input);

            if (recordId <= 0) {
                System.out.println(
                        "Please enter a valid record ID."
                );
                return;
            }

        } catch (NumberFormatException e) {

            System.out.println(
                    "Please enter a valid numeric ID."
            );
            return;
        }

        System.out.println("\nParking rate: ₱20.00 per hour.");

        System.out.print(
                "Enter calculated parking fee: ₱"
        );

        String feeInput = scanner.nextLine().trim();

        double fee;

        try {

            fee = Double.parseDouble(feeInput);

            if (!Double.isFinite(fee) || fee < 0) {
                System.out.println("Invalid parking fee.");
                return;
            }

        } catch (NumberFormatException e) {

            System.out.println(
                    "Please enter a valid parking fee."
            );
            return;
        }

        parkingRecordDao.checkoutVehicle(
                user.id(),
                recordId,
                fee
        );
    }
}
