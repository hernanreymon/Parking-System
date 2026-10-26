
package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseInitializer.initialize();

            UserDao userDao = new UserDao();
            Authentication authentication = new Authentication(userDao);

            authentication.start();

        } catch (Exception e) {
            System.out.println("Unable to start Parking System.");
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
