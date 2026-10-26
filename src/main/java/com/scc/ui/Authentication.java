
package com.scc.ui;

import com.scc.dao.UserDao;
import com.scc.model.User;

import java.util.Scanner;

public class Authentication {

    private final UserDao userDao;
    private final Scanner scanner = new Scanner(System.in);

    public Authentication(UserDao userDao) {
        this.userDao = userDao;
    }

    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("\n================================");
            System.out.println("       PARKING SYSTEM");
            System.out.println("================================");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> login();
                    case "2" -> register();
                    case "3" -> {
                        running = false;
                        System.out.println("Thank you for using the system.");
                    }
                    default -> System.out.println("Invalid choice.");
                }
            } catch (Exception e) {
                System.out.println("Operation failed: " + e.getMessage());
            }
        }
    }

    private void login() throws Exception {
        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        User user = userDao.login(username, password);

        if (user == null) {
            System.out.println(
                    "Login failed. Check your credentials and account status."
            );
            return;
        }

        System.out.println("Welcome, " + user.username() + "!");

        switch (user.roles().toUpperCase()) {
            case "ADMIN" ->
                    new AdminMenu(scanner, userDao).show();
            case "CUSTOMER" ->
                    new CustomerMenu(scanner, user).show();
            case "ATTENDANT" ->
                    new AttendantMenu(scanner, user).show();
            default ->
                    System.out.println("Unknown user role.");
        }
    }

    private void register() throws Exception {
        System.out.println("\n--- REGISTRATION ---");

        System.out.print("Create username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Create password: ");
        String password = scanner.nextLine();

        System.out.println("Choose role:");
        System.out.println("1. Customer");
        System.out.println("2. Parking Attendant");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim();
        String role;

        if (choice.equals("1")) {
            role = "CUSTOMER";
        } else if (choice.equals("2")) {
            role = "ATTENDANT";
        } else {
            System.out.println("Invalid role.");
            return;
        }

        boolean success = userDao.register(username, password, role);

        if (success) {
            System.out.println(
                    "Registration successful. Wait for administrator approval."
            );
        } else {
            System.out.println(
                    "Registration failed. Username may already exist."
            );
        }
    }
}
