
package com.scc.ui;

import com.scc.dao.DatabaseConnection;
import com.scc.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class CustomerMenu {

    private final Scanner scanner;
    private final User user;

    public CustomerMenu(Scanner scanner, User user) {
        this.scanner = scanner;
        this.user = user;
    }

    public void show() {
        boolean running = true;

        while (running) {
            System.out.println("\n================================");
            System.out.println("          CUSTOMER MENU");
            System.out.println("================================");
            System.out.println("Welcome, " + user.username());
            System.out.println("1. View Parking Slots");
            System.out.println("2. Logout");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewParkingSlots();
                case "2" -> running = false;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewParkingSlots() {
        String sql = """
            SELECT slot_number, status
            FROM parking_slots
            ORDER BY slot_number
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n-----------------------------");
            System.out.printf("%-15s %-15s%n", "PARKING SLOT", "STATUS");
            System.out.println("-----------------------------");

            while (rs.next()) {
                System.out.printf("%-15s %-15s%n",
                        rs.getString("slot_number"),
                        rs.getString("status"));
            }

        } catch (SQLException e) {
            System.out.println("Could not load parking slots: "
                    + e.getMessage());
        }
    }
}
