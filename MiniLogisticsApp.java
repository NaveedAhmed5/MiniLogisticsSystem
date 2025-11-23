import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * MINI LOGISTICS - ADMIN PORTAL
 * * Database Connected Version
 * * Connects to 'logistics_db' via XAMPP (MySQL)
 * * FIXED: Explicitly loading MySQL Driver to prevent "No suitable driver" error.
 */
public class MiniLogisticsApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new MainFrame().setVisible(true);
        });
    }
}

// ==========================================
// 1. DATABASE CONFIGURATION
// ==========================================
class AdminDBConfig {
    static final String URL = "jdbc:mysql://localhost:3306/logistics_db";
    static final String USER = "root";
    static final String PASS = ""; // Default XAMPP password
    
    public static Connection getConnection() throws SQLException {
        try {
            // FORCE LOAD DRIVER
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new SQLException("MySQL Driver not found in classpath");
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

// ==========================================
// 2. ENTITIES (Renamed to avoid conflicts)
// ==========================================
class AdminVehicle {
    String model, plate;
    double capacity;
    public AdminVehicle(String m, String p, double c) { model=m; plate=p; capacity=c; }
    public String getDetails() { return model + " (" + plate + ")"; }
}

class AdminDriverEntity {
    int id;
    String name, status;
    double rating;
    int activeJobs;
    AdminVehicle vehicle;
    
    public AdminDriverEntity(int id, String name, String status, double rating, int jobs, AdminVehicle v) {
        this.id = id; this.name = name; this.status = status; 
        this.rating = rating; this.activeJobs = jobs; this.vehicle = v;
    }
}

class AdminDeliveryEntity {
    int id, assignedDriverId;
    String desc, route, status, priority, deadline;
    
    public AdminDeliveryEntity(int id, String desc, String route, String status, int driverId, String pri, String dead) {
        this.id = id; this.desc = desc; this.route = route; this.status = status;
        this.assignedDriverId = driverId; this.priority = pri; this.deadline = dead;
    }
    
    public Object[] toRow() {
        return new Object[]{id, desc, route, status, assignedDriverId == 0 ? "Unassigned" : assignedDriverId, priority == null ? "-" : priority, deadline == null ? "-" : deadline};
    }
}

class AdminAuditLog {
    String time, cat, det;
    public AdminAuditLog(String t, String c, String d) { time=t; cat=c; det=d; }
}

// ==========================================
// 3. CONTROLLER
// ==========================================
class AdminPortalController {
    
    public List<AdminDriverEntity> getDrivers() {
        List<AdminDriverEntity> list = new ArrayList<>();
        // Note: We calculate active_jobs dynamically now as it was removed from drivers table
        String sql = "SELECT u.user_id, u.name, d.status, d.rating, " +
                     "(SELECT COUNT(*) FROM deliveries del WHERE del.assigned_driver_id = d.user_id AND del.status != 'COMPLETED' AND del.status != 'CANCELLED') as active_jobs, " +
                     "v.model, v.plate_no, v.capacity " +
                     "FROM drivers d JOIN users u ON d.user_id = u.user_id " +
                     "JOIN vehicles v ON d.vehicle_id = v.vehicle_id";

        try (Connection conn = AdminDBConfig.getConnection(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                AdminVehicle v = new AdminVehicle(rs.getString("model"), rs.getString("plate_no"), rs.getDouble("capacity"));
                list.add(new AdminDriverEntity(rs.getInt("user_id"), rs.getString("name"), rs.getString("status"), rs.getDouble("rating"), rs.getInt("active_jobs"), v));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean updateDriverStatus(int id, String status, String reason) {
        try (Connection conn = AdminDBConfig.getConnection()) {
            // Check active jobs using dynamic query
            String checkSql = "SELECT COUNT(*) FROM deliveries WHERE assigned_driver_id=? AND status != 'COMPLETED' AND status != 'CANCELLED'";
            PreparedStatement check = conn.prepareStatement(checkSql);
            check.setInt(1, id);
            ResultSet rs = check.executeQuery();
            
            if(rs.next() && rs.getInt(1) > 0 && ("Inactive".equals(status) || "Suspended".equals(status))) return false;

            PreparedStatement ps = conn.prepareStatement("UPDATE drivers SET status=? WHERE user_id=?");
            ps.setString(1, status); ps.setInt(2, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public List<AdminDeliveryEntity> getDeliveries() {
        List<AdminDeliveryEntity> list = new ArrayList<>();
        String sql = "SELECT d.*, a.priority, a.deadline FROM deliveries d LEFT JOIN assignments a ON d.delivery_id = a.delivery_id";
        try (Connection conn = AdminDBConfig.getConnection(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {
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
            conn.commit();
            return "Success";
        } catch (SQLException e) { return "DB Error: " + e.getMessage(); }
    }

    public List<AdminAuditLog> getLogs() {
        List<AdminAuditLog> logs = new ArrayList<>();
        try(Connection c = AdminDBConfig.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT * FROM audit_logs ORDER BY log_id DESC")) {
            while(rs.next()) logs.add(new AdminAuditLog(rs.getString("timestamp"), rs.getString("category"), rs.getString("details")));
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

// ==========================================
// 4. VIEW LAYER
// ==========================================
class MainFrame extends JFrame {
    AdminPortalController ctrl = new AdminPortalController();
    public MainFrame() {
        setTitle("Admin Console - DB Connected"); setSize(1000,700); setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Drivers", new DriverPanel(ctrl));
        tabs.addTab("Assignments", new AssignPanel(ctrl));
        tabs.addTab("Logs", new LogPanel(ctrl));
        tabs.addTab("Reports", new ReportPanel(ctrl));
        add(tabs);
    }
}

class DriverPanel extends JPanel {
    public DriverPanel(AdminPortalController ctrl) {
        setLayout(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Status", "Jobs", "Rating", "Vehicle"}, 0);
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        JPanel bot = new JPanel();
        JButton ref = new JButton("Refresh"), act = new JButton("Activate"), susp = new JButton("Suspend");
        bot.add(ref); bot.add(act); bot.add(susp);
        add(bot, BorderLayout.SOUTH);

        ref.addActionListener(e -> {
            model.setRowCount(0);
            for(AdminDriverEntity d : ctrl.getDrivers()) model.addRow(new Object[]{d.id, d.name, d.status, d.activeJobs, d.rating, d.vehicle.getDetails()});
        });
        act.addActionListener(e -> update((JTable)((JScrollPane)getComponent(0)).getViewport().getView(), ctrl, "Active"));
        susp.addActionListener(e -> update((JTable)((JScrollPane)getComponent(0)).getViewport().getView(), ctrl, "Suspended"));
        ref.doClick();
    }
    void update(JTable t, AdminPortalController c, String s) {
        int r = t.getSelectedRow();
        if(r >= 0 && c.updateDriverStatus((int)t.getValueAt(r, 0), s, "Manual")) JOptionPane.showMessageDialog(this, "Updated!");
        else JOptionPane.showMessageDialog(this, "Failed (Active jobs?)");
    }
}

class AssignPanel extends JPanel {
    public AssignPanel(AdminPortalController ctrl) {
        setLayout(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID","Desc","Route","Status","Driver","Pri","Deadline"},0);
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel bot = new JPanel();
        JTextField drvId = new JTextField(5);
        JButton assign = new JButton("Assign Driver ID:"), ref = new JButton("Refresh");
        bot.add(ref); bot.add(assign); bot.add(drvId);
        add(bot, BorderLayout.SOUTH);

        ref.addActionListener(e -> {
            model.setRowCount(0);
            for(AdminDeliveryEntity d : ctrl.getDeliveries()) model.addRow(d.toRow());
        });
        assign.addActionListener(e -> {
            int r = table.getSelectedRow();
            if(r >= 0) {
                JOptionPane.showMessageDialog(this, ctrl.assignDriver((int)model.getValueAt(r, 0), Integer.parseInt(drvId.getText()), "High", 24));
                ref.doClick();
            }
        });
        ref.doClick();
    }
}

class LogPanel extends JPanel {
    public LogPanel(AdminPortalController ctrl) {
        setLayout(new BorderLayout());
        DefaultTableModel model = new DefaultTableModel(new String[]{"Time","Cat","Details"},0);
        add(new JScrollPane(new JTable(model)));
        JButton ref = new JButton("Refresh");
        add(ref, BorderLayout.SOUTH);
        ref.addActionListener(e -> {
            model.setRowCount(0);
            for(AdminAuditLog l : ctrl.getLogs()) model.addRow(new Object[]{l.time, l.cat, l.det});
        });
        ref.doClick();
    }
}

class ReportPanel extends JPanel {
    public ReportPanel(AdminPortalController ctrl) {
        setLayout(new BorderLayout());
        JTextArea area = new JTextArea(); 
        add(new JScrollPane(area));
        JPanel top = new JPanel();
        JButton gen = new JButton("Generate Driver Report");
        top.add(gen);
        add(top, BorderLayout.NORTH);
        gen.addActionListener(e -> area.setText(ctrl.generateReport("Drivers")));
    }
}