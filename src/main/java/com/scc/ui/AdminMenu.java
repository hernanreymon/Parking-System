
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.dao.ActivityLogDao;
import com.scc.model.User;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminMenu {

    private final Scanner scanner;
    private final UserDao userDao;
    private final User admin;
    private final ActivityLogDao activityLogDao;

    public AdminMenu(
            Scanner scanner,
            UserDao userDao,
            User admin
    ) {
        this.scanner = scanner;
        this.userDao = userDao;
        this.admin = admin;
        this.activityLogDao = new ActivityLogDao();
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
            System.out.println("4. Update User Role/Status");
            System.out.println("5. View Audit Logs");
            System.out.println("6. Logout");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {

                case "1" ->
                        viewUsers(userDao.getAllUsers());

                case "2" ->
                        viewUsers(userDao.getPendingUsers());

                case "3" ->
                        approveUser();

                case "4" ->
                        updateUser();

                case "5" ->
                        activityLogDao.displayLogs();

                case "6" -> {
                    running = false;
                    System.out.println("Admin logged out.");
                }

                default ->
                        System.out.println("Invalid choice.");
            }
        }
    }

    // DISPLAY USERS
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

        System.out.println("------------------------------------------------");
    }

    // APPROVE USER
    private void approveUser() throws SQLException {

        viewUsers(userDao.getPendingUsers());

        System.out.print("\nEnter user ID to approve: ");

        String input = scanner.nextLine().trim();

        try {

            int userId = Integer.parseInt(input);

            if (userId == admin.id()) {
                System.out.println(
                        "You cannot approve your own account."
                );
                return;
            }

            if (userDao.approveUser(admin.id(), userId)) {

                System.out.println(
                        "User approved successfully."
                );

            } else {

                System.out.println(
                        "User not found or account is already approved."
                );
            }

        } catch (NumberFormatException e) {

            System.out.println(
                    "Please enter a valid numeric ID."
            );
        }
    }

    // UPDATE USER ROLE AND STATUS
    private void updateUser() throws SQLException {

        viewUsers(userDao.getAllUsers());

        System.out.print("\nEnter User ID to update: ");

        String input = scanner.nextLine().trim();

        try {

            int userId = Integer.parseInt(input);

            if (userId == admin.id()) {
                System.out.println(
                        "Modifying the currently active admin account is restricted."
                );
                return;
            }

            System.out.println("\nChoose New Role:");
            System.out.println("1. CUSTOMER");
            System.out.println("2. ATTENDANT");
            System.out.println("3. ADMIN");
            System.out.print("Choice: ");

            String roleChoice = scanner.nextLine().trim();

            String newRole;

            switch (roleChoice) {
                case "1" -> newRole = "CUSTOMER";
                case "2" -> newRole = "ATTENDANT";
                case "3" -> newRole = "ADMIN";
                default -> {
                    System.out.println("Invalid role.");
                    return;
                }
            }

            System.out.println("\nChoose New Status:");
            System.out.println("1. APPROVED");
            System.out.println("2. PENDING");
            System.out.print("Choice: ");

            String statusChoice = scanner.nextLine().trim();

            String newStatus;

            switch (statusChoice) {
                case "1" -> newStatus = "APPROVED";
                case "2" -> newStatus = "PENDING";
                default -> {
                    System.out.println("Invalid status.");
                    return;
                }
            }

            boolean success =
                    userDao.updateUserRoleAndStatus(
                            admin.id(),
                            userId,
                            newRole,
                            newStatus
                    );

            if (success) {
                System.out.println(
                        "User role and status updated successfully."
                );
            } else {
                System.out.println("User not found.");
            }

        } catch (NumberFormatException e) {

            System.out.println(
                    "Please enter a valid numeric ID."
            );
        }
    }
}
