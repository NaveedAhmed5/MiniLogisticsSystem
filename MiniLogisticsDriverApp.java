import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MINI LOGISTICS - DRIVER PORTAL
 * * Based on Deliverable #3 (Use Cases), #4 (Sequence), and Class Diagram.
 * * FEATURES:
 * - Registration (Sequence Pg 7)
 * - Login (Sequence Pg 8)
 * - Delivery List & Details (Sequence Pg 15)
 * - Update Status / Complete / Cancel (Sequence Pg 11-13)
 * - Profile Management (Sequence Pg 14)
 */

public class MiniLogisticsDriverApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } 
            catch (Exception e) {}
            new MainDriverFrame().setVisible(true);
        });
    }
}

// ===========================
// 1. ENTITY LAYER (Class Diagram)
// ===========================

class User {
    protected int userID;
    protected String name;
    protected String email;
    protected String password;
    protected String phone;

    public User(int id, String name, String email, String password, String phone) {
        this.userID = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }
    public String getName() { return name; }
    public boolean validateLogin(String email, String pass) {
        return this.email.equalsIgnoreCase(email) && this.password.equals(pass);
    }
}

class Vehicle {
    private String model;
    private String plateNo;
    private double capacity;
    private String insuranceInfo;

    public Vehicle(String model, String plateNo, double capacity, String insuranceInfo) {
        this.model = model;
        this.plateNo = plateNo;
        this.capacity = capacity;
        this.insuranceInfo = insuranceInfo;
    }
    public String getDetails() { return model + " [" + plateNo + "]"; }
    // Setters for Profile Update Use Case
    public void setModel(String m) { this.model = m; }
    public void setPlate(String p) { this.plateNo = p; }
}

class Driver extends User {
    private String licenseNo;
    private String status; // Active, Pending, Suspended
    private double rating;
    private Vehicle vehicle; // Composition (Class Diagram)
    private double earnings;

    public Driver(int id, String name, String email, String password, String phone, String license, String status, Vehicle vehicle) {
        super(id, name, email, password, phone);
        this.licenseNo = license;
        this.status = status;
        this.rating = 5.0; // Default
        this.vehicle = vehicle;
        this.earnings = 0.0;
    }

    public String getStatus() { return status; }
    public Vehicle getVehicle() { return vehicle; }
    public double getEarnings() { return earnings; }
    public void addEarnings(double amount) { this.earnings += amount; }
    
    // Profile Update Methods
    public void updateProfile(String name, String phone, String email) {
        this.name = name; this.phone = phone; this.email = email;
    }
}

class Delivery {
    private int deliveryID;
    private String pickupLoc;
    private String dropoffLoc;
    private String status; // Pending, Assigned, In Transit, Completed, Cancelled
    private String description;
    private int driverID;
    private double fee;
    private String customerContact;

    public Delivery(int id, String desc, String pickup, String dropoff, String status, int driverID, double fee, String contact) {
        this.deliveryID = id;
        this.description = desc;
        this.pickupLoc = pickup;
        this.dropoffLoc = dropoff;
        this.status = status;
        this.driverID = driverID;
        this.fee = fee;
        this.customerContact = contact;
    }

    public int getID() { return deliveryID; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public int getDriverID() { return driverID; }
    public String getRoute() { return pickupLoc + " -> " + dropoffLoc; }
    public String getDescription() { return description; }
    public double getFee() { return fee; }
    public String getContact() { return customerContact; }
}

// ===========================
// 2. CONTROLLER (Mock Logic)
// ===========================

class PortalController {
    private static PortalController instance;
    private List<Driver> drivers = new ArrayList<>();
    private List<Delivery> deliveries = new ArrayList<>();
    private Driver currentSession;

    private PortalController() {
        // Mock Data
        Vehicle v1 = new Vehicle("Toyota Prius", "XYZ-123", 100.0, "INS-999");
        drivers.add(new Driver(101, "Bob Driver", "bob", "pass", "555-0101", "LIC-001", "Active", v1));
        
        Vehicle v2 = new Vehicle("Honda Civic", "ABC-789", 80.0, "INS-888");
        drivers.add(new Driver(102, "Alice New", "alice", "pass", "555-0102", "LIC-002", "Pending", v2));

        deliveries.add(new Delivery(501, "Medical Kit", "City Hospital", "Clinic A", "Assigned", 101, 25.0, "Dr. Smith (555-9999)"));
        deliveries.add(new Delivery(502, "Office Chairs", "Warehouse", "Tech Corp", "In Transit", 101, 40.0, "Manager John (555-8888)"));
        deliveries.add(new Delivery(503, "Flowers", "Florist", "Home 22B", "Completed", 101, 15.0, "Mrs. Doe"));
    }

    public static PortalController getInstance() {
        if (instance == null) instance = new PortalController();
        return instance;
    }

    // Login Use Case (Page 7/8)
    public String login(String email, String pass) {
        for (Driver d : drivers) {
            if (d.validateLogin(email, pass)) {
                if ("Pending".equals(d.getStatus())) return "Account Pending Approval";
                if ("Suspended".equals(d.getStatus())) return "Account Suspended";
                currentSession = d;
                return "Success";
            }
        }
        return "Invalid Credentials";
    }

    // Registration Use Case (Page 6/7)
    public boolean register(String name, String email, String pass, String phone, String carModel, String plate) {
        // Check duplicate
        if (drivers.stream().anyMatch(d -> d.email.equals(email))) return false;
        
        int newId = drivers.size() + 101;
        Vehicle v = new Vehicle(carModel, plate, 50.0, "Pending Verification");
        Driver d = new Driver(newId, name, email, pass, phone, "Pending", "Pending", v);
        drivers.add(d);
        return true;
    }

    public Driver getCurrentDriver() { return currentSession; }
    public void logout() { currentSession = null; }

    public List<Delivery> getMyDeliveries() {
        return deliveries.stream()
                .filter(d -> d.getDriverID() == currentSession.userID)
                .collect(Collectors.toList());
    }

    // Update Status (Page 12)
    public void updateDeliveryStatus(int id, String newStatus) {
        for (Delivery d : deliveries) {
            if (d.getID() == id) d.setStatus(newStatus);
        }
    }

    // Complete Delivery (Page 13)
    public void completeDelivery(int id) {
        for (Delivery d : deliveries) {
            if (d.getID() == id) {
                d.setStatus("Completed");
                currentSession.addEarnings(d.getFee());
            }
        }
    }
}

// ===========================
// 3. VIEW LAYER (Swing)
// ===========================

class MainDriverFrame extends JFrame {
    CardLayout cardLayout = new CardLayout();
    JPanel mainPanel = new JPanel(cardLayout);
    PortalController controller = PortalController.getInstance();

    public MainDriverFrame() {
        setTitle("Driver Portal - Mini Logistics");
        setSize(400, 700); // Mobile-like aspect ratio
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel.add(new LoginPanel(this), "LOGIN");
        mainPanel.add(new RegisterPanel(this), "REGISTER");
        // Dashboard added dynamically after login

        add(mainPanel);
    }

    public void showDashboard() {
        mainPanel.add(new DriverDashboard(this), "DASHBOARD");
        cardLayout.show(mainPanel, "DASHBOARD");
    }

    public void showRegister() { cardLayout.show(mainPanel, "REGISTER"); }
    public void showLogin() { cardLayout.show(mainPanel, "LOGIN"); }
}

class LoginPanel extends JPanel {
    public LoginPanel(MainDriverFrame frame) {
        setLayout(new GridBagLayout());
        setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10,10,10,10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Driver Login");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        
        JTextField emailField = new JTextField(15);
        JPasswordField passField = new JPasswordField(15);
        JButton loginBtn = new JButton("Login");
        JButton regBtn = new JButton("Register as Driver");

        gbc.gridx=0; gbc.gridy=0; gbc.gridwidth=2; add(title, gbc);
        gbc.gridy=1; gbc.gridwidth=1; add(new JLabel("Email:"), gbc);
        gbc.gridx=1; add(emailField, gbc);
        gbc.gridx=0; gbc.gridy=2; add(new JLabel("Password:"), gbc);
        gbc.gridx=1; add(passField, gbc);
        gbc.gridx=0; gbc.gridy=3; gbc.gridwidth=2; add(loginBtn, gbc);
        gbc.gridy=4; add(regBtn, gbc);

        // Pre-fill for demo
        emailField.setText("bob"); passField.setText("pass");

        loginBtn.addActionListener(e -> {
            String res = frame.controller.login(emailField.getText(), new String(passField.getPassword()));
            if ("Success".equals(res)) frame.showDashboard();
            else JOptionPane.showMessageDialog(this, res);
        });

        regBtn.addActionListener(e -> frame.showRegister());
    }
}

class RegisterPanel extends JPanel {
    public RegisterPanel(MainDriverFrame frame) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(20,20,20,20));

        add(new JLabel("Driver Registration"));
        
        JTextField name = new JTextField();
        JTextField email = new JTextField();
        JPasswordField pass = new JPasswordField();
        JTextField phone = new JTextField();
        JTextField carModel = new JTextField();
        JTextField plate = new JTextField();

        add(new JLabel("Full Name:")); add(name);
        add(new JLabel("Email:")); add(email);
        add(new JLabel("Password:")); add(pass);
        add(new JLabel("Phone:")); add(phone);
        add(Box.createVerticalStrut(10));
        add(new JLabel("Vehicle Model:")); add(carModel);
        add(new JLabel("License Plate:")); add(plate);
        
        JButton uploadDocs = new JButton("Upload Documents (License/Ins)");
        uploadDocs.addActionListener(e -> JOptionPane.showMessageDialog(this, "Documents Uploaded Successfully!"));
        add(Box.createVerticalStrut(10));
        add(uploadDocs);

        JButton submit = new JButton("Submit Application");
        submit.addActionListener(e -> {
            if(frame.controller.register(name.getText(), email.getText(), new String(pass.getPassword()), phone.getText(), carModel.getText(), plate.getText())) {
                JOptionPane.showMessageDialog(this, "Registration Successful!\nAccount is Pending Approval.");
                frame.showLogin();
            } else {
                JOptionPane.showMessageDialog(this, "Email already exists.");
            }
        });

        JButton back = new JButton("Back to Login");
        back.addActionListener(e -> frame.showLogin());

        add(Box.createVerticalStrut(20));
        add(submit);
        add(back);
    }
}

class DriverDashboard extends JPanel {
    private MainDriverFrame frame;
    private JTabbedPane tabs;

    public DriverDashboard(MainDriverFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());

        tabs = new JTabbedPane();
        tabs.addTab("My Deliveries", new ActiveDeliveriesPanel(frame));
        tabs.addTab("History", new HistoryPanel(frame)); // Reuses logic filtered by 'Completed'
        tabs.addTab("Profile", new ProfilePanel(frame));

        add(tabs, BorderLayout.CENTER);
        
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            frame.controller.logout();
            frame.showLogin();
        });
        add(logout, BorderLayout.NORTH);
    }
}

class ActiveDeliveriesPanel extends JPanel {
    private MainDriverFrame frame;
    private DefaultTableModel model;
    private JTable table;

    public ActiveDeliveriesPanel(MainDriverFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        
        String[] cols = {"ID", "Route", "Status", "Fee"};
        model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton viewBtn = new JButton("View Details & Actions");
        JButton refreshBtn = new JButton("Refresh");
        btnPanel.add(viewBtn);
        btnPanel.add(refreshBtn);
        add(btnPanel, BorderLayout.SOUTH);

        viewBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int id = (Integer) model.getValueAt(row, 0);
                showDeliveryDetails(id);
            }
        });

        refreshBtn.addActionListener(e -> refresh());
        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        for (Delivery d : frame.controller.getMyDeliveries()) {
            // Only show active jobs
            if (!"Completed".equals(d.getStatus()) && !"Cancelled".equals(d.getStatus())) {
                model.addRow(new Object[]{d.getID(), d.getRoute(), d.getStatus(), "$" + d.getFee()});
            }
        }
    }

    // Delivery Detail Use Case (Page 15)
    private void showDeliveryDetails(int id) {
        Delivery d = frame.controller.getMyDeliveries().stream().filter(x -> x.getID() == id).findFirst().orElse(null);
        if (d == null) return;

        JDialog dlg = new JDialog(frame, "Delivery #" + id, true);
        dlg.setSize(400, 500);
        dlg.setLayout(new BoxLayout(dlg.getContentPane(), BoxLayout.Y_AXIS));
        JPanel p = (JPanel) dlg.getContentPane();
        p.setBorder(new EmptyBorder(10,10,10,10));

        p.add(new JLabel("<html><h2>" + d.getDescription() + "</h2></html>"));
        p.add(new JLabel("Route: " + d.getRoute()));
        p.add(new JLabel("Customer: " + d.getContact()));
        p.add(new JLabel("Fee: $" + d.getFee()));
        p.add(new JLabel("Current Status: " + d.getStatus()));
        
        // Simulated Map View
        JPanel map = new JPanel();
        map.setBackground(Color.LIGHT_GRAY);
        map.setPreferredSize(new Dimension(350, 150));
        map.add(new JLabel("[ MAP VIEW: GPS Tracking ]"));
        p.add(Box.createVerticalStrut(10));
        p.add(map);

        // Action Buttons
        JButton navBtn = new JButton("Start Navigation"); // Mock
        p.add(navBtn);

        // Update Status (Page 12)
        JPanel statusPanel = new JPanel();
        String[] states = {"Picked Up", "In Transit", "Out for Delivery"};
        JComboBox<String> statusCombo = new JComboBox<>(states);
        JButton updateBtn = new JButton("Update Status");
        statusPanel.add(statusCombo);
        statusPanel.add(updateBtn);
        p.add(statusPanel);

        updateBtn.addActionListener(e -> {
            frame.controller.updateDeliveryStatus(id, (String) statusCombo.getSelectedItem());
            dlg.dispose();
            refresh();
            JOptionPane.showMessageDialog(frame, "Status Updated & Customer Notified.");
        });

        // Complete / Cancel
        JPanel finalPanel = new JPanel();
        JButton completeBtn = new JButton("Complete");
        JButton cancelBtn = new JButton("Cancel");
        finalPanel.add(completeBtn);
        finalPanel.add(cancelBtn);
        p.add(finalPanel);

        // Complete Delivery (Page 13)
        completeBtn.addActionListener(e -> {
            String proof = JOptionPane.showInputDialog(dlg, "Enter Proof of Delivery (Sign/Code):");
            if (proof != null && !proof.trim().isEmpty()) {
                frame.controller.completeDelivery(id);
                dlg.dispose();
                refresh();
                JOptionPane.showMessageDialog(frame, "Delivery Completed! Earnings Updated.");
            }
        });

        // Cancel Delivery (Page 11)
        cancelBtn.addActionListener(e -> {
            String reason = JOptionPane.showInputDialog(dlg, "Enter Cancellation Reason (Min 10 chars):");
            if (reason != null && reason.length() >= 10) {
                frame.controller.updateDeliveryStatus(id, "Cancelled");
                dlg.dispose();
                refresh();
                JOptionPane.showMessageDialog(frame, "Delivery Cancelled. Admin Notified.");
            } else if (reason != null) {
                JOptionPane.showMessageDialog(dlg, "Reason too short.");
            }
        });

        dlg.setVisible(true);
    }
}

class HistoryPanel extends JPanel {
    public HistoryPanel(MainDriverFrame frame) {
        setLayout(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Desc", "Status", "Earned"}, 0);
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton ref = new JButton("Refresh History");
        add(ref, BorderLayout.SOUTH);
        
        ref.addActionListener(e -> {
            model.setRowCount(0);
            for (Delivery d : frame.controller.getMyDeliveries()) {
                if ("Completed".equals(d.getStatus()) || "Cancelled".equals(d.getStatus())) {
                    model.addRow(new Object[]{d.getID(), d.getDescription(), d.getStatus(), "$" + d.getFee()});
                }
            }
        });
    }
}

class ProfilePanel extends JPanel {
    public ProfilePanel(MainDriverFrame frame) {
        setLayout(new GridLayout(6, 2, 10, 10));
        setBorder(new EmptyBorder(20,20,20,20));

        JTextField name = new JTextField();
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        JTextField car = new JTextField();
        JTextField plate = new JTextField();
        
        add(new JLabel("Name:")); add(name);
        add(new JLabel("Phone:")); add(phone);
        add(new JLabel("Email:")); add(email);
        add(new JLabel("Vehicle:")); add(car);
        add(new JLabel("Plate:")); add(plate);

        JButton load = new JButton("Load My Info");
        JButton save = new JButton("Save Changes");
        add(load); add(save);

        load.addActionListener(e -> {
            Driver d = frame.controller.getCurrentDriver();
            name.setText(d.getName());
            email.setText("bob"); // mock accessor
            phone.setText("555-0101");
            car.setText(d.getVehicle().getDetails());
        });

        save.addActionListener(e -> {
            Driver d = frame.controller.getCurrentDriver();
            d.updateProfile(name.getText(), phone.getText(), email.getText());
            d.getVehicle().setModel(car.getText());
            d.getVehicle().setPlate(plate.getText());
            JOptionPane.showMessageDialog(this, "Profile Updated Successfully!");
        });
    }
}