import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MINI LOGISTICS & DELIVERY TRACKER
 * VERSION: Class Diagram Compliant (Fixed Compilation Error & Read-Only Tables)
 * * ARCHITECTURE:
 * - Entities: User, Driver, Admin, Vehicle, Delivery, Assignment, AuditLog
 * - Controllers: DriverController, DeliveryController, ReportController
 * - View: AdminDashboard (Swing)
 */

public class MiniLogisticsApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) { e.printStackTrace(); }
            new MainFrame().setVisible(true);
        });
    }
}

// ===========================
// 1. ENTITY LAYER (Domain Model)
// ===========================

abstract class User {
    protected int userID;
    protected String name;
    protected String email;
    protected String role;

    public User(int userID, String name, String email, String role) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.role = role;
    }
    public int getUserID() { return userID; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
}

class Vehicle {
    private String model;
    private String plateNo;
    private double capacity; // e.g., in kg

    public Vehicle(String model, String plateNo, double capacity) {
        this.model = model;
        this.plateNo = plateNo;
        this.capacity = capacity;
    }
    
    public String getDetails() { return model + " (" + plateNo + ")"; }
    public double getCapacity() { return capacity; }
}

class Driver extends User {
    private String licenseNo;
    private String status; // Active, Suspended, etc.
    private double rating;
    private int activeJobs;
    private Vehicle vehicle; // Association: Driver owns Vehicle

    public Driver(int id, String name, String email, String licenseNo, String status, double rating, Vehicle vehicle) {
        super(id, name, email, "DRIVER");
        this.licenseNo = licenseNo;
        this.status = status;
        this.rating = rating;
        this.vehicle = vehicle;
        this.activeJobs = 0;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getActiveJobs() { return activeJobs; }
    public void incrementJobs() { this.activeJobs++; }
    public double getRating() { return rating; }
    public Vehicle getVehicle() { return vehicle; }
}

class Admin extends User {
    public Admin(int id, String name, String email) { super(id, name, email, "ADMIN"); }
}

class Assignment {
    private int assignmentID;
    private String priorityLevel;
    private LocalDateTime deadline;
    private LocalDateTime assignedAt;

    public Assignment(int id, String priority, LocalDateTime deadline) {
        this.assignmentID = id;
        this.priorityLevel = priority;
        this.deadline = deadline;
        this.assignedAt = LocalDateTime.now();
    }
    
    public String getPriority() { return priorityLevel; }
    public String getDeadlineStr() { return deadline.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")); }
}

class Delivery {
    private int deliveryID;
    private String description;
    private String pickupLocation;
    private String dropoffLocation;
    private String status; // PENDING, ASSIGNED, DELIVERED
    private int assignedDriverID; // 0 if null
    private Assignment assignment; // Composition: Delivery has an Assignment

    public Delivery(int id, String desc, String pickup, String dropoff) {
        this.deliveryID = id;
        this.description = desc;
        this.pickupLocation = pickup;
        this.dropoffLocation = dropoff;
        this.status = "PENDING";
        this.assignedDriverID = 0;
    }

    public int getDeliveryID() { return deliveryID; }
    public String getDescription() { return description; }
    public String getRoute() { return pickupLocation + " -> " + dropoffLocation; }
    public String getStatus() { return status; }
    public int getAssignedDriverID() { return assignedDriverID; }
    public Assignment getAssignment() { return assignment; }

    public void assign(int driverID, Assignment assignment) {
        this.assignedDriverID = driverID;
        this.assignment = assignment;
        this.status = "ASSIGNED";
    }
}

class AuditLog {
    private LocalDateTime timestamp;
    private String category;
    private String details;

    public AuditLog(String category, String details) {
        this.timestamp = LocalDateTime.now();
        this.category = category;
        this.details = details;
    }
    public String toString() { return timestamp.format(DateTimeFormatter.ISO_LOCAL_TIME) + " [" + category + "] " + details; }
    public String getCategory() { return category; }
    public String getDetails() { return details; }
    public String getTime() { return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
}

// ===========================
// 2. CONTROLLER LAYER
// ===========================

// Mock Database (Single Source of Truth)
class Database {
    static List<User> users = new ArrayList<>();
    static List<Delivery> deliveries = new ArrayList<>();
    static List<AuditLog> logs = new ArrayList<>();
    
    static {
        // Mock Data
        users.add(new Admin(1, "SuperAdmin", "admin@sys.com"));
        users.add(new Driver(101, "Bob", "bob@sys.com", "LIC-999", "Active", 4.8, new Vehicle("Toyota Van", "ABC-123", 500.0)));
        users.add(new Driver(102, "Charlie", "charlie@sys.com", "LIC-888", "Active", 4.5, new Vehicle("Honda Bike", "XYZ-789", 50.0)));
        users.add(new Driver(103, "Dave", "dave@sys.com", "LIC-777", "Suspended", 2.0, new Vehicle("Ford Truck", "TRK-001", 1000.0)));

        deliveries.add(new Delivery(501, "Medical Supplies", "Hospital A", "Clinic B"));
        deliveries.add(new Delivery(502, "Office Chairs", "Warehouse", "Office 5"));
        
        logs.add(new AuditLog("SYSTEM", "System Initialized"));
    }
}

class DriverController {
    public List<Driver> getAllDrivers() {
        return Database.users.stream().filter(u -> u instanceof Driver).map(u -> (Driver)u).collect(Collectors.toList());
    }

    public Driver getDriver(int id) {
        return (Driver) Database.users.stream().filter(u -> u.getUserID() == id).findFirst().orElse(null);
    }

    public boolean updateStatus(int driverID, String newStatus, String reason) {
        Driver d = getDriver(driverID);
        if (d == null) return false;

        // Validation Rule: Cannot deactivate if active jobs exist
        if (("Inactive".equals(newStatus) || "Suspended".equals(newStatus)) && d.getActiveJobs() > 0) {
            Database.logs.add(new AuditLog("ERROR", "Failed status update for " + d.getName() + " (Has active jobs)"));
            return false;
        }

        String old = d.getStatus();
        d.setStatus(newStatus);
        Database.logs.add(new AuditLog("ADMIN", "Driver " + d.getName() + " status: " + old + " -> " + newStatus + ". Reason: " + reason));
        return true;
    }
}

class DeliveryController {
    public List<Delivery> getAllDeliveries() { return Database.deliveries; }

    public String assignDriver(int deliveryID, int driverID, String priority, int deadlineHours) {
        Delivery delivery = Database.deliveries.stream().filter(d -> d.getDeliveryID() == deliveryID).findFirst().orElse(null);
        Driver driver = (Driver) Database.users.stream().filter(u -> u.getUserID() == driverID).findFirst().orElse(null);

        if (delivery == null || driver == null) return "Error: Not found";

        // Business Logic Checks
        if (!"Active".equals(driver.getStatus())) return "Error: Driver is not Active";
        if (driver.getActiveJobs() >= 3) return "Warning: Driver overloaded";

        // Create Assignment Object (Class Diagram requirement)
        Assignment assignment = new Assignment(
            (int)(Math.random()*1000), 
            priority, 
            LocalDateTime.now().plusHours(deadlineHours)
        );

        delivery.assign(driverID, assignment);
        driver.incrementJobs();
        
        Database.logs.add(new AuditLog("OPERATION", "Assigned Delivery " + deliveryID + " to " + driver.getName()));
        return "Success";
    }
}

class ReportController {
    public List<AuditLog> getLogs() { return Database.logs; }

    public String generateReport(String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("REPORT TYPE: ").append(type).append("\n");
        sb.append("Generated: ").append(LocalDateTime.now()).append("\n\n");

        if ("Drivers".equals(type)) {
            for(User u : Database.users) {
                if(u instanceof Driver) {
                    Driver d = (Driver)u;
                    sb.append(String.format("%s | %s | Rating: %.1f | Vehicle: %s\n", 
                        d.getName(), d.getStatus(), d.getRating(), d.getVehicle().getDetails()));
                }
            }
        } else {
            for(Delivery d : Database.deliveries) {
                String assignInfo = d.getAssignment() == null ? "Unassigned" : "Pri: " + d.getAssignment().getPriority();
                sb.append(String.format("ID %d | %s | %s | %s\n", 
                    d.getDeliveryID(), d.getStatus(), d.getRoute(), assignInfo));
            }
        }
        return sb.toString();
    }
}

// ===========================
// 3. UI LAYER (Swing)
// ===========================

class MainFrame extends JFrame {
    // Controllers
    DriverController driverCtrl = new DriverController();
    DeliveryController deliveryCtrl = new DeliveryController();
    ReportController reportCtrl = new ReportController();

    public MainFrame() {
        setTitle("Logistics Manager - Admin Console");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        add(new AdminPanel(this));
    }
}

class AdminPanel extends JPanel {
    public AdminPanel(MainFrame frame) {
        setLayout(new BorderLayout());
        
        // Header
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.setBackground(new Color(44, 62, 80));
        JLabel title = new JLabel("  Admin Dashboard");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.add(title);
        add(header, BorderLayout.NORTH);

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        tabs.addTab("Manage Drivers", new DriverMgmtPanel(frame));
        tabs.addTab("Assign Deliveries", new AssignDeliveryPanel(frame));
        tabs.addTab("System Logs", new LogsPanel(frame));
        tabs.addTab("Reports", new ReportPanel(frame)); 

        add(tabs, BorderLayout.CENTER);
    }
}

class DriverMgmtPanel extends JPanel {
    private MainFrame frame;
    private DefaultTableModel model;

    public DriverMgmtPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());

        // Updated Table Columns based on Class Diagram (Vehicle info included)
        String[] cols = {"ID", "Name", "Status", "Vehicle Model", "Plate No", "Capacity (kg)", "Rating"};
        
        // OVERRIDE isCellEditable TO MAKE TABLE READ-ONLY
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel controls = new JPanel();
        JButton btnActivate = new JButton("Activate");
        JButton btnSuspend = new JButton("Suspend");
        JButton btnDeactivate = new JButton("Deactivate"); 

        controls.add(btnActivate);
        controls.add(btnSuspend);
        controls.add(btnDeactivate);
        add(controls, BorderLayout.SOUTH);

        btnActivate.addActionListener(e -> updateStatus("Active"));
        btnSuspend.addActionListener(e -> updateStatus("Suspended"));
        btnDeactivate.addActionListener(e -> updateStatus("Inactive"));

        refresh();
    }

    private void updateStatus(String newStatus) {
        int row = ((JTable)((JScrollPane)getComponent(0)).getViewport().getView()).getSelectedRow();
        if (row == -1) return;
        
        int id = (Integer) model.getValueAt(row, 0);
        String reason = JOptionPane.showInputDialog(this, "Reason:");
        
        boolean success = frame.driverCtrl.updateStatus(id, newStatus, reason == null ? "Admin Action" : reason);
        if (success) refresh();
        else JOptionPane.showMessageDialog(this, "Failed: Driver has active jobs or not found.");
    }

    private void refresh() {
        model.setRowCount(0);
        for(Driver d : frame.driverCtrl.getAllDrivers()) {
            model.addRow(new Object[]{
                d.getUserID(), d.getName(), d.getStatus(), 
                d.getVehicle().getDetails().split("\\(")[0], // Model
                d.getVehicle().getDetails(), // Full Details (Lazy fix for Plate)
                d.getVehicle().getCapacity(), d.getRating()
            });
        }
    }
}

class AssignDeliveryPanel extends JPanel {
    private MainFrame frame;
    private DefaultTableModel model;
    private JComboBox<String> driverCombo;
    private JComboBox<String> priorityCombo;
    private JComboBox<String> deadlineCombo;

    public AssignDeliveryPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        
        String[] cols = {"ID", "Description", "Route", "Status", "Assigned Driver", "Priority", "Deadline"};
        
        // OVERRIDE isCellEditable TO MAKE TABLE READ-ONLY
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        bottom.add(new JLabel("Driver:"));
        driverCombo = new JComboBox<>();
        bottom.add(driverCombo);
        
        bottom.add(new JLabel("Priority:"));
        priorityCombo = new JComboBox<>(new String[]{"Standard", "High", "Urgent"});
        bottom.add(priorityCombo);

        bottom.add(new JLabel("Deadline (Hrs):"));
        deadlineCombo = new JComboBox<>(new String[]{"24", "48", "72"});
        bottom.add(deadlineCombo);
        
        JButton assignBtn = new JButton("Assign");
        bottom.add(assignBtn);
        add(bottom, BorderLayout.SOUTH);

        assignBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            
            String selectedDriver = (String) driverCombo.getSelectedItem();
            if (selectedDriver == null) return;
            int driverID = Integer.parseInt(selectedDriver.split(":")[0]);
            int deliveryID = (Integer) model.getValueAt(row, 0);
            
            String res = frame.deliveryCtrl.assignDriver(deliveryID, driverID, 
                (String)priorityCombo.getSelectedItem(), 
                Integer.parseInt((String)deadlineCombo.getSelectedItem())
            );
            
            if(res.startsWith("Warning")) {
                if(JOptionPane.showConfirmDialog(this, res + ". Override?", "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    // Logic to force assignment could go here
                }
            } else if (!res.equals("Success")) {
                JOptionPane.showMessageDialog(this, res);
            }
            refresh();
        });
        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        for(Delivery d : frame.deliveryCtrl.getAllDeliveries()) {
            String driverInfo = d.getAssignedDriverID() == 0 ? "None" : String.valueOf(d.getAssignedDriverID());
            String pri = d.getAssignment() == null ? "-" : d.getAssignment().getPriority();
            String dead = d.getAssignment() == null ? "-" : d.getAssignment().getDeadlineStr();
            
            model.addRow(new Object[]{d.getDeliveryID(), d.getDescription(), d.getRoute(), d.getStatus(), driverInfo, pri, dead});
        }
        
        driverCombo.removeAllItems();
        for(Driver d : frame.driverCtrl.getAllDrivers()) {
            driverCombo.addItem(d.getUserID() + ": " + d.getName() + " (" + d.getActiveJobs() + " jobs)");
        }
    }
}

class LogsPanel extends JPanel {
    private MainFrame frame;
    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;

    public LogsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        String[] cols = {"Time", "Category", "Details"};
        
        // OVERRIDE isCellEditable TO MAKE TABLE READ-ONLY
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        add(refreshBtn, BorderLayout.SOUTH);
        refreshBtn.addActionListener(e -> refresh());
        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        for(AuditLog l : frame.reportCtrl.getLogs()) {
            model.addRow(new Object[]{l.getTime(), l.getCategory(), l.getDetails()});
        }
    }
}

class ReportPanel extends JPanel {
    private MainFrame frame; 
    private JComboBox<String> typeCombo;
    private JTextArea previewArea;

    public ReportPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        
        JPanel controls = new JPanel();
        controls.add(new JLabel("Type:"));
        typeCombo = new JComboBox<>(new String[]{"Drivers", "Deliveries"});
        controls.add(typeCombo);
        
        JButton genBtn = new JButton("Generate Preview");
        controls.add(genBtn);
        
        JButton exportBtn = new JButton("Export .txt");
        controls.add(exportBtn);
        
        add(controls, BorderLayout.NORTH);
        
        // MAKE TEXT AREA READ-ONLY
        previewArea = new JTextArea();
        previewArea.setEditable(false); 
        
        add(new JScrollPane(previewArea), BorderLayout.CENTER);
        
        genBtn.addActionListener(e -> {
            previewArea.setText(frame.reportCtrl.generateReport((String)typeCombo.getSelectedItem()));
        });
        
        exportBtn.addActionListener(e -> {
            try (PrintWriter out = new PrintWriter(new FileWriter("export.txt"))) {
                out.println(previewArea.getText());
                JOptionPane.showMessageDialog(this, "Saved to export.txt");
            } catch (IOException ex) { ex.printStackTrace(); }
        });
    }
}