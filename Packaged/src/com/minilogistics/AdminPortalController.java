package com.minilogistics.controller;

import com.minilogistics.db.AdminDBConfig;
import com.minilogistics.model.AdminEntities.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdminPortalController {
    
    // Private helper method to log actions to the database
    private void log(Connection c, String cat, String det) throws SQLException {
        // Use CURRENT_TIMESTAMP in SQL for precise server time recording
        String sql = "INSERT INTO audit_logs (category, details, timestamp) VALUES (?, ?, CURRENT_TIMESTAMP)";
        PreparedStatement ps = c.prepareStatement(sql);
        ps.setString(1, cat); 
        ps.setString(2, det); 
        ps.executeUpdate();
    }
    
    public List<AdminDriverEntity> getDrivers() {
        List<AdminDriverEntity> list = new ArrayList<>();
        // Note: We calculate active_jobs dynamically now as it was removed from drivers table
        String sql = "SELECT u.user_id, u.name, d.status, d.rating, " +
                     "(SELECT COUNT(*) FROM deliveries del WHERE del.assigned_driver_id = d.user_id AND del.status != 'COMPLETED' AND del.status != 'CANCELLED') as active_jobs, " +
                     "v.model, v.plate_no, v.capacity " +
                     "FROM drivers d JOIN users u ON d.user_id = u.user_id " +
                     "JOIN vehicles v ON d.vehicle_id = v.vehicle_id";

        try (Connection conn = AdminDBConfig.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                AdminVehicle v = new AdminVehicle(rs.getString("model"), rs.getString("plate_no"), rs.getDouble("capacity"));
                list.add(new AdminDriverEntity(rs.getInt("user_id"), rs.getString("name"), rs.getString("status"), rs.getDouble("rating"), rs.getInt("active_jobs"), v));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean updateDriverStatus(int id, String status, String reason) {
        try (Connection conn = AdminDBConfig.getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM deliveries WHERE assigned_driver_id=? AND status != 'COMPLETED' AND status != 'CANCELLED'";
            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, id);
            ResultSet rs = check.executeQuery();
            
            if(rs.next() && rs.getInt(1) > 0 && ("Inactive".equals(status) || "Suspended".equals(status))) return false;

            PreparedStatement ps = conn.prepareStatement("UPDATE drivers SET status=? WHERE user_id=?");
            ps.setString(1, status); ps.setInt(2, id);
            ps.executeUpdate();
            
            // LOGGING CALL ADDED HERE
            log(conn, "DRIVER_STATUS", "Admin changed Driver ID " + id + " status to " + status);
            
            return true;
        } catch (SQLException e) { 
             e.printStackTrace(); // Always print the stack trace for debugging connectivity/SQL issues
             return false; 
        }
    }

    public List<AdminDeliveryEntity> getDeliveries() {
        List<AdminDeliveryEntity> list = new ArrayList<>();
        String sql = "SELECT d.*, a.priority, a.deadline FROM deliveries d LEFT JOIN assignments a ON d.delivery_id = a.delivery_id";
        try (Connection conn = AdminDBConfig.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                list.add(new AdminDeliveryEntity(rs.getInt("delivery_id"), rs.getString("description"), rs.getString("pickup")+" -> "+rs.getString("dropoff"), rs.getString("status"), rs.getInt("assigned_driver_id"), rs.getString("priority"), rs.getString("deadline")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public String assignDriver(int delId, int drvId, String pri, int hours) {
        try (Connection conn = AdminDBConfig.getConnection()) {
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement("SELECT status FROM drivers WHERE user_id=?");
            ps.setInt(1, drvId); ResultSet rs = ps.executeQuery();
            if(!rs.next() || !"Active".equals(rs.getString("status"))) return "Error: Driver not active";

            PreparedStatement ps2 = conn.prepareStatement("INSERT INTO assignments (delivery_id, priority, deadline) VALUES (?,?,?)");
            ps2.setInt(1, delId); ps2.setString(2, pri); 
            ps2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now().plusHours(hours)));
            ps2.executeUpdate();

            conn.createStatement().executeUpdate("UPDATE deliveries SET status='ASSIGNED', assigned_driver_id="+drvId+" WHERE delivery_id="+delId);
            
            // LOGGING CALL ADDED HERE
            log(conn, "DELIVERY_ASSIGN", "Delivery ID " + delId + " assigned to Driver ID " + drvId + " with priority " + pri);
            
            conn.commit();
            return "Success";
        } catch (SQLException e) { return "DB Error: " + e.getMessage(); }
    }

    public List<AdminAuditLog> getLogs() {
        List<AdminAuditLog> logs = new ArrayList<>();
        // Logs the access to the log panel itself (Use Case Pg 3, Step 14 equivalent)
        try (Connection c = AdminDBConfig.getConnection()) {
            log(c, "SECURITY", "Admin accessed Audit Log panel."); 
            
            // Retrieve logs
            try(ResultSet rs = c.createStatement().executeQuery("SELECT * FROM audit_logs ORDER BY log_id DESC LIMIT 100")) {
                while(rs.next()) logs.add(new AdminAuditLog(rs.getString("timestamp"), rs.getString("category"), rs.getString("details")));
            }
        } catch (SQLException e) {}
        return logs;
    }

    public String generateReport(String type) {
        StringBuilder sb = new StringBuilder("=== ADMIN REPORT ===\nGenerated: " + LocalDateTime.now() + "\n\n");
        if ("Drivers".equals(type)) {
            for(AdminDriverEntity d : getDrivers()) sb.append(String.format("%s | %s | Jobs: %d\n", d.name, d.status, d.activeJobs));
        } else {
            for(AdminDeliveryEntity d : getDeliveries()) sb.append(String.format("ID %d | %s | %s\n", d.id, d.status, d.desc));
        }
        return sb.toString();
    }
}