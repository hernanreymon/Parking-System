
package com.scc.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ParkingRecordDao {

    private final ActivityLogDao activityLogDao =
            new ActivityLogDao();

    // DISPLAY AVAILABLE PARKING SLOTS
    public void displayAvailableSlots() {

        String sql = """
                SELECT slot_number, status
                FROM parking_slots
                WHERE status = 'AVAILABLE'
                ORDER BY id
                """;

        System.out.println("\n===== AVAILABLE PARKING SLOTS =====");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.println(
                        rs.getString("slot_number")
                                + " - "
                                + rs.getString("status")
                );
            }

            if (!found) {
                System.out.println("No available parking slots.");
            }

            System.out.println("===================================");

        } catch (SQLException e) {
            System.out.println(
                    "Unable to retrieve parking slots: "
                            + e.getMessage()
            );
        }
    }

    // PARK VEHICLE IN CUSTOMER'S PREFERRED SLOT
    public boolean parkVehicle(
            int userId,
            String vehicleNumber,
            String ownerName,
            String preferredSlot
    ) {

        vehicleNumber = vehicleNumber.trim().toUpperCase();
        ownerName = ownerName.trim();
        preferredSlot = preferredSlot.trim().toUpperCase();

        if (vehicleNumber.isEmpty()
                || ownerName.isEmpty()
                || preferredSlot.isEmpty()) {

            System.out.println("All fields are required.");
            return false;
        }

        String findSlotSql = """
                SELECT id, slot_number
                FROM parking_slots
                WHERE slot_number = ?
                AND status = 'AVAILABLE'
                """;

        String updateSlotSql = """
                UPDATE parking_slots
                SET status = 'OCCUPIED'
                WHERE id = ?
                AND status = 'AVAILABLE'
                """;

        String insertSql = """
                INSERT INTO parking_records
                (vehicle_number, owner_name, slot_id, status)
                VALUES (?, ?, ?, 'PARKED')
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                int slotId;
                String slotNumber;

                // CHECK CUSTOMER'S PREFERRED SLOT
                try (PreparedStatement ps =
                             conn.prepareStatement(findSlotSql)) {

                    ps.setString(1, preferredSlot);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            System.out.println(
                                    "Selected slot is unavailable "
                                            + "or does not exist."
                            );

                            conn.rollback();
                            return false;
                        }

                        slotId = rs.getInt("id");
                        slotNumber = rs.getString("slot_number");
                    }
                }

                // UPDATE SLOT STATUS
                try (PreparedStatement ps =
                             conn.prepareStatement(updateSlotSql)) {

                    ps.setInt(1, slotId);

                    if (ps.executeUpdate() == 0) {
                        System.out.println(
                                "Slot is no longer available. "
                                        + "Please select another slot."
                        );

                        conn.rollback();
                        return false;
                    }
                }

                // INSERT PARKING RECORD
                try (PreparedStatement ps =
                             conn.prepareStatement(insertSql)) {

                    ps.setString(1, vehicleNumber);
                    ps.setString(2, ownerName);
                    ps.setInt(3, slotId);

                    ps.executeUpdate();
                }

                conn.commit();

                // AUDIT LOG
                boolean logged = activityLogDao.logActivity(
                        userId,
                        "VEHICLE_PARKED",
                        "Vehicle " + vehicleNumber
                                + " parked in Slot " + slotNumber + "."
                );

                if (!logged) {
                    System.out.println(
                            "Warning: Parking saved but audit log failed."
                    );
                }

                System.out.println(
                        "Vehicle parked successfully in Slot "
                                + slotNumber + "."
                );

                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println(
                    "Parking Error: " + e.getMessage()
            );
            return false;
        }
    }

    // VEHICLE CHECKOUT
    public boolean checkoutVehicle(
            int userId,
            int recordId,
            double fee
    ) {

        if (recordId <= 0 || !Double.isFinite(fee) || fee < 0) {
            System.out.println("Invalid record ID or fee.");
            return false;
        }

        String findRecordSql = """
                SELECT vehicle_number, slot_id, status
                FROM parking_records
                WHERE id = ?
                """;

        String checkoutSql = """
                UPDATE parking_records
                SET time_out = CURRENT_TIMESTAMP,
                    fee = ?,
                    status = 'COMPLETED'
                WHERE id = ?
                AND status = 'PARKED'
                """;

        String updateSlotSql = """
                UPDATE parking_slots
                SET status = 'AVAILABLE'
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {

            conn.setAutoCommit(false);

            try {

                String vehicleNumber;
                int slotId;
                String oldStatus;

                // FIND PARKING RECORD
                try (PreparedStatement ps =
                             conn.prepareStatement(findRecordSql)) {

                    ps.setInt(1, recordId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            System.out.println(
                                    "Parking record not found."
                            );

                            conn.rollback();
                            return false;
                        }

                        vehicleNumber =
                                rs.getString("vehicle_number");

                        slotId = rs.getInt("slot_id");

                        oldStatus = rs.getString("status");
                    }
                }

                if (!"PARKED".equalsIgnoreCase(oldStatus)) {
                    System.out.println(
                            "This vehicle is not currently parked."
                    );

                    conn.rollback();
                    return false;
                }

                // UPDATE PARKING RECORD
                try (PreparedStatement ps =
                             conn.prepareStatement(checkoutSql)) {

                    ps.setDouble(1, fee);
                    ps.setInt(2, recordId);

                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }

                // RELEASE PARKING SLOT
                try (PreparedStatement ps =
                             conn.prepareStatement(updateSlotSql)) {

                    ps.setInt(1, slotId);

                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        System.out.println(
                                "Unable to release parking slot."
                        );
                        return false;
                    }
                }

                conn.commit();

                // AUDIT LOG
                boolean logged = activityLogDao.logActivity(
                        userId,
                        "VEHICLE_CHECKOUT",
                        "Parking Record #" + recordId
                                + " for Vehicle " + vehicleNumber
                                + " status changed from PARKED "
                                + "to COMPLETED."
                );

                if (!logged) {
                    System.out.println(
                            "Warning: Checkout saved but audit log failed."
                    );
                }

                System.out.println(
                        "Vehicle checkout completed successfully."
                );

                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;

            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.out.println(
                    "Checkout Error: " + e.getMessage()
            );
            return false;
        }
    }
}
