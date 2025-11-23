package com.minilogistics.controller;

import com.minilogistics.db.DriverDB;
import com.minilogistics.model.DriverEntities.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DriverAppController {
    private DriverSession current;

    public String login(String email, String pass) {
        try (Connection c = DriverDB.getConnection()) {
            String sql = "SELECT u.user_id, u.name, u.email, u.phone, d.status, d.earnings " +
                         "FROM users u JOIN drivers d ON u.user_id = d.user_id " +
                         "WHERE u.email=? AND u.password=? AND u.role='DRIVER'";
            
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, email); ps.setString(2, pass);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                if("Pending".equals(status)) return "Account Pending Approval";
                if("Suspended".equals(status)) return "Account Suspended";
                
                current = new DriverSession(rs.getInt("user_id"), rs.getString("name"), status, rs.getDouble("earnings"), rs.getString("email"), rs.getString("phone"));
                return "Success";
            }
            return "Invalid Credentials";
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "DB Error: " + e.getMessage(); 
        }
    }

    public String register(String name, String email, String pass, String phone, String model, String plate) {
        try (Connection c = DriverDB.getConnection()) {
            PreparedStatement check = c.prepareStatement("SELECT count(*) FROM users WHERE email = ?");
            check.setString(1, email);
            ResultSet rsCheck = check.executeQuery();
            if(rsCheck.next() && rsCheck.getInt(1) > 0) {
                return "Email address '" + email + "' is already registered.";
            }

            c.setAutoCommit(false); // Start Transaction

            String userSql = "INSERT INTO users (name, email, password, phone, role) VALUES (?,?,?,?,'DRIVER')";
            PreparedStatement p1 = c.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            p1.setString(1, name); p1.setString(2, email); p1.setString(3, pass); p1.setString(4, phone);
            p1.executeUpdate();
            
            ResultSet rs1 = p1.getGeneratedKeys(); rs1.next(); int uid = rs1.getInt(1); 

            String vehSql = "INSERT INTO vehicles (model, plate_no, capacity, insurance_info) VALUES (?,?,500,'Pending')";
            PreparedStatement p2 = c.prepareStatement(vehSql, Statement.RETURN_GENERATED_KEYS);
            p2.setString(1, model); p2.setString(2, plate);
            p2.executeUpdate();
            
            ResultSet rs2 = p2.getGeneratedKeys(); rs2.next(); int vid = rs2.getInt(1); 

            String driverSql = "INSERT INTO drivers (user_id, license_no, status, vehicle_id) VALUES (?, 'PENDING', 'Pending', ?)";
            PreparedStatement p3 = c.prepareStatement(driverSql);
            p3.setInt(1, uid); p3.setInt(2, vid);
            p3.executeUpdate();
            
            c.commit(); 
            return "Success";
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "Database Error: " + e.getMessage(); 
        }
    }

    public List<DriverJob> getMyJobs() {
        List<DriverJob> list = new ArrayList<>();
        try (Connection c = DriverDB.getConnection()) {
            String sql = "SELECT * FROM deliveries WHERE assigned_driver_id=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, current.id);
            
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                list.add(new DriverJob(rs.getInt("delivery_id"), rs.getString("description"), rs.getString("pickup")+" -> "+rs.getString("dropoff"), rs.getString("status"), rs.getDouble("fee"), rs.getString("customer_contact")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void updateStatus(int jobId, String status) {
        try (Connection c = DriverDB.getConnection()) {
            String sql = "UPDATE deliveries SET status=? WHERE delivery_id=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, status); ps.setInt(2, jobId);
            ps.executeUpdate();
            
            if("COMPLETED".equalsIgnoreCase(status)) {
                String earnSql = "UPDATE drivers SET earnings = earnings + (SELECT fee FROM deliveries WHERE delivery_id=?) WHERE user_id=?";
                PreparedStatement p2 = c.prepareStatement(earnSql);
                p2.setInt(1, jobId); p2.setInt(2, current.id);
                p2.executeUpdate();
                
                String refreshSql = "SELECT earnings FROM drivers WHERE user_id=?";
                PreparedStatement p3 = c.prepareStatement(refreshSql);
                p3.setInt(1, current.id);
                ResultSet rs = p3.executeQuery();
                if(rs.next()) current.earnings = rs.getDouble(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    public DriverVehicleInfo getVehicleInfo() {
        try (Connection c = DriverDB.getConnection()) {
            String sql = "SELECT v.model, v.plate_no FROM vehicles v JOIN drivers d ON d.vehicle_id = v.vehicle_id WHERE d.user_id=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, current.id);
            ResultSet rs = ps.executeQuery();
            if(rs.next()) return new DriverVehicleInfo(rs.getString(1), rs.getString(2));
        } catch(SQLException e) {}
        return new DriverVehicleInfo("Unknown", "Unknown");
    }

    public boolean updateProfile(String name, String phone, String email) {
        try (Connection c = DriverDB.getConnection()) {
            String sql = "UPDATE users SET name=?, phone=?, email=? WHERE user_id=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, name); ps.setString(2, phone); ps.setString(3, email); ps.setInt(4, current.id);
            ps.executeUpdate();
            
            current.name = name; current.phone = phone; current.email = email;
            return true;
        } catch(SQLException e) { return false; }
    }
    
    public DriverSession getSession() { return current; }
    public void logout() { current = null; }
}