
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner scanner;
    private final UserDao userDao;

    public AdminMenu(Scanner scanner, UserDao userDao) {
        this.scanner = scanner;
        this.userDao = userDao;
    }

    public void show() throws SQLException {
        boolean running = true;

        while (running) {
            System.out.println("\n================================");
            System.out.println("       ADMINISTRATOR MENU");
            System.out.println("================================");
            System.out.println("1. View All System Users");
            System.out.println("2. View Pending Registrations");
            System.out.println("3. Approve User");
            System.out.println("4. Logout");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> viewUsers(userDao.getAllUsers());
                case "2" -> viewUsers(userDao.getPendingUsers());
                case "3" -> approveUser();
                case "4" -> running = false;
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void viewUsers(List<User> users) {
        System.out.println("\n------------------------------------------------");
        System.out.printf("%-6s %-20s %-15s %-12s%n",
                "ID", "USERNAME", "ROLE", "STATUS");
        System.out.println("------------------------------------------------");

        if (users.isEmpty()) {
            System.out.println("No users found.");
            return;
        }

        for (User user : users) {
            System.out.printf("%-6d %-20s %-15s %-12s%n",
                    user.id(),
                    user.username(),
                    user.roles(),
                    user.status());
        }
    }

    private void approveUser() throws SQLException {
        viewUsers(userDao.getPendingUsers());

        System.out.print("\nEnter user ID to approve: ");
        String input = scanner.nextLine().trim();

        try {
            int userId = Integer.parseInt(input);

            if (userDao.approveUser(userId)) {
                System.out.println("User approved successfully.");
            } else {
                System.out.println(
                        "User not found or account is already approved."
                );
            }
        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid numeric ID.");
        }
    }
}
