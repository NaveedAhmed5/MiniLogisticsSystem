package com.minilogistics.model;

import java.time.LocalDateTime;

// Contains all Admin-specific data models (renamed to avoid conflicts)
public class AdminEntities {
    
    public static class AdminVehicle {
        public String model, plate;
        public double capacity;
        public AdminVehicle(String m, String p, double c) { model=m; plate=p; capacity=c; }
        public String getDetails() { return model + " (" + plate + ")"; }
    }

    public static class AdminDriverEntity {
        public int id;
        public String name, status;
        public double rating;
        public int activeJobs;
        public AdminVehicle vehicle;
        
        public AdminDriverEntity(int id, String name, String status, double rating, int jobs, AdminVehicle v) {
            this.id = id; this.name = name; this.status = status; 
            this.rating = rating; this.activeJobs = jobs; this.vehicle = v;
        }
    }

    public static class AdminDeliveryEntity {
        public int id, assignedDriverId;
        public String desc, route, status, priority, deadline;
        
        public AdminDeliveryEntity(int id, String desc, String route, String status, int driverId, String pri, String dead) {
            this.id = id; this.desc = desc; this.route = route; this.status = status;
            this.assignedDriverId = driverId; this.priority = pri; this.deadline = dead;
        }
        
        public Object[] toRow() {
            return new Object[]{id, desc, route, status, assignedDriverId == 0 ? "Unassigned" : assignedDriverId, priority == null ? "-" : priority, deadline == null ? "-" : deadline};
        }
    }

    public static class AdminAuditLog {
        public String time, cat, det;
        public AdminAuditLog(String t, String c, String d) { time=t; cat=c; det=d; }
    }
}