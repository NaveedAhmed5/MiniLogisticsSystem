package com.minilogistics.model;

// Contains all Driver-specific data models (renamed to avoid conflicts)
public class DriverEntities {

    public static class DriverSession {
        public int id;
        public String name, status, email, phone;
        public double earnings;
        
        public DriverSession(int id, String name, String status, double earnings, String email, String phone) {
            this.id = id; this.name = name; this.status = status; 
            this.earnings = earnings; this.email = email; this.phone = phone;
        }
    }

    public static class DriverJob {
        public int id;
        public String desc, route, status, contact;
        public double fee;
        
        public DriverJob(int id, String desc, String route, String status, double fee, String contact) {
            this.id = id; this.desc = desc; this.route = route; 
            this.status = status; this.fee = fee; this.contact = contact;
        }
    }

    public static class DriverVehicleInfo {
        public String model, plate;
        public DriverVehicleInfo(String m, String p) { model = m; plate = p; }
    }
}