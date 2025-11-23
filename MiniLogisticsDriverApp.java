import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MINI LOGISTICS - DRIVER PORTAL
 * * Database Connected Version
 * * Connects to 'logistics_db' via XAMPP (MySQL)
 * * Fixed: Renamed internal classes to avoid conflicts with Admin App
 * * Fixed: Robust Registration check for existing emails
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

// ==========================================
// 1. DATABASE CONFIGURATION
// ==========================================
class DriverDB {
    static final String URL = "jdbc:mysql://localhost:3306/logistics_db";
    static final String USER = "root";
    static final String PASS = ""; // Default XAMPP password
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}

// ==========================================
// 2. ENTITIES (Renamed for Driver App Scope)
// ==========================================
class DriverSession {
    int id;
    String name, status, email, phone;
    double earnings;
    
    public DriverSession(int id, String name, String status, double earnings, String email, String phone) {
        this.id = id; 
        this.name = name; 
        this.status = status; 
        this.earnings = earnings;
        this.email = email;
        this.phone = phone;
    }
}

class DriverJob {
    int id;
    String desc, route, status, contact;
    double fee;
    
    public DriverJob(int id, String desc, String route, String status, double fee, String contact) {
        this.id = id; 
        this.desc = desc; 
        this.route = route; 
        this.status = status; 
        this.fee = fee; 
        this.contact = contact;
    }
}

class DriverVehicleInfo {
    String model, plate;
    public DriverVehicleInfo(String m, String p) { model = m; plate = p; }
}

// ==========================================
// 3. CONTROLLER (Business Logic)
// ==========================================
class DriverAppController {
    private DriverSession current;

    // --- LOGIN LOGIC ---
    public String login(String email, String pass) {
        try (Connection c = DriverDB.getConnection()) {
            // Join Users and Drivers tables to get full info
            String sql = "SELECT u.user_id, u.name, u.email, u.phone, d.status, d.earnings " +
                         "FROM users u JOIN drivers d ON u.user_id = d.user_id " +
                         "WHERE u.email=? AND u.password=? AND u.role='DRIVER'";
            
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, email); 
            ps.setString(2, pass);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                if("Pending".equals(status)) return "Account Pending Approval";
                if("Suspended".equals(status)) return "Account Suspended";
                
                current = new DriverSession(
                    rs.getInt("user_id"), 
                    rs.getString("name"), 
                    status, 
                    rs.getDouble("earnings"),
                    rs.getString("email"),
                    rs.getString("phone")
                );
                return "Success";
            }
            return "Invalid Credentials";
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return "DB Error: " + e.getMessage(); 
        }
    }

    // --- REGISTRATION LOGIC (Transactional) ---
    public boolean register(String name, String email, String pass, String phone, String model, String plate) {
        try (Connection c = DriverDB.getConnection()) {
            // 0. Pre-check: Does email exist?
            PreparedStatement check = c.prepareStatement("SELECT count(*) FROM users WHERE email = ?");
            check.setString(1, email);
            ResultSet rsCheck = check.executeQuery();
            if(rsCheck.next() && rsCheck.getInt(1) > 0) {
                System.out.println("DEBUG: Email " + email + " already exists.");
                return false; // Email taken, return false to show error
            }

            c.setAutoCommit(false); // Start Transaction

            // 1. Insert into Users Table
            String userSql = "INSERT INTO users (name, email, password, phone, role) VALUES (?,?,?,?,'DRIVER')";
            PreparedStatement p1 = c.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
            p1.setString(1, name); 
            p1.setString(2, email); 
            p1.setString(3, pass); 
            p1.setString(4, phone);
            p1.executeUpdate();
            
            ResultSet rs1 = p1.getGeneratedKeys(); 
            rs1.next(); 
            int uid = rs1.getInt(1); // Get the new User ID

            // 2. Insert into Vehicles Table
            String vehSql = "INSERT INTO vehicles (model, plate_no, capacity, insurance_info) VALUES (?,?,500,'Pending')";
            PreparedStatement p2 = c.prepareStatement(vehSql, Statement.RETURN_GENERATED_KEYS);
            p2.setString(1, model); 
            p2.setString(2, plate);
            p2.executeUpdate();
            
            ResultSet rs2 = p2.getGeneratedKeys(); 
            rs2.next(); 
            int vid = rs2.getInt(1); // Get the new Vehicle ID

            // 3. Insert into Drivers Table (Linking User and Vehicle)
            String driverSql = "INSERT INTO drivers (user_id, license_no, status, vehicle_id) VALUES (?, 'PENDING', 'Pending', ?)";
            PreparedStatement p3 = c.prepareStatement(driverSql);
            p3.setInt(1, uid); 
            p3.setInt(2, vid);
            p3.executeUpdate();
            
            c.commit(); // Commit Transaction
            System.out.println("DEBUG: Registration Successful for " + email);
            return true;
        } catch (SQLException e) { 
            e.printStackTrace(); 
            return false; 
        }
    }

    // --- FETCH JOBS ---
    public List<DriverJob> getMyJobs() {
        List<DriverJob> list = new ArrayList<>();
        try (Connection c = DriverDB.getConnection()) {
            String sql = "SELECT * FROM deliveries WHERE assigned_driver_id=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, current.id);
            
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                list.add(new DriverJob(
                    rs.getInt("delivery_id"), 
                    rs.getString("description"), 
                    rs.getString("pickup")+" -> "+rs.getString("dropoff"), 
                    rs.getString("status"), 
                    rs.getDouble("fee"), 
                    rs.getString("customer_contact")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // --- UPDATE STATUS ---
    public void updateStatus(int jobId, String status) {
        try (Connection c = DriverDB.getConnection()) {
            String sql = "UPDATE deliveries SET status=? WHERE delivery_id=?";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setString(1, status); 
            ps.setInt(2, jobId);
            ps.executeUpdate();
            
            // If Completed, update earnings
            if("COMPLETED".equalsIgnoreCase(status)) {
                String earnSql = "UPDATE drivers SET earnings = earnings + (SELECT fee FROM deliveries WHERE delivery_id=?) WHERE user_id=?";
                PreparedStatement p2 = c.prepareStatement(earnSql);
                p2.setInt(1, jobId); 
                p2.setInt(2, current.id);
                p2.executeUpdate();
                
                // Refresh local session earnings
                String refreshSql = "SELECT earnings FROM drivers WHERE user_id=?";
                PreparedStatement p3 = c.prepareStatement(refreshSql);
                p3.setInt(1, current.id);
                ResultSet rs = p3.executeQuery();
                if(rs.next()) current.earnings = rs.getDouble(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    // --- PROFILE INFO ---
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
            
            // Update local session
            current.name = name; current.phone = phone; current.email = email;
            return true;
        } catch(SQLException e) { return false; }
    }
    
    public DriverSession getSession() { return current; }
    public void logout() { current = null; }
}

// ==========================================
// 4. VIEW LAYER (Swing UI)
// ==========================================
class MainDriverFrame extends JFrame {
    CardLayout cards = new CardLayout();
    JPanel main = new JPanel(cards);
    DriverAppController ctrl = new DriverAppController();

    public MainDriverFrame() {
        setTitle("Driver App - DB Connected"); 
        setSize(400, 700); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        main.add(new LoginP(this), "LOGIN");
        main.add(new RegP(this), "REG");
        // Dashboard added dynamically
        
        add(main);
    }
    
    void goDash() { 
        main.add(new DashP(this), "DASH"); // Re-create to refresh data
        cards.show(main, "DASH"); 
    }
    void goReg() { cards.show(main, "REG"); }
    void goLog() { cards.show(main, "LOGIN"); }
}

class LoginP extends JPanel {
    public LoginP(MainDriverFrame f) {
        setLayout(new GridLayout(5,1,10,10)); 
        setBorder(new EmptyBorder(50,20,50,20));
        
        JTextField em = new JTextField("bob"); // Default for quick test
        JPasswordField pw = new JPasswordField("pass");
        JButton log = new JButton("Login");
        JButton reg = new JButton("Create Account");
        
        add(new JLabel("Email:")); add(em); 
        add(new JLabel("Password:")); add(pw);
        add(log); add(reg);
        
        log.addActionListener(e -> {
            String res = f.ctrl.login(em.getText(), new String(pw.getPassword()));
            if("Success".equals(res)) f.goDash();
            else JOptionPane.showMessageDialog(this, res);
        });
        
        reg.addActionListener(e -> f.goReg());
    }
}

class RegP extends JPanel {
    public RegP(MainDriverFrame f) {
        setLayout(new GridLayout(8,2, 5, 5)); 
        setBorder(new EmptyBorder(20,20,20,20));
        
        JTextField nm=new JTextField(), em=new JTextField(), ph=new JTextField(), md=new JTextField(), pl=new JTextField();
        JPasswordField pw=new JPasswordField();
        
        add(new JLabel("Name:")); add(nm); 
        add(new JLabel("Email:")); add(em);
        add(new JLabel("Password:")); add(pw); 
        add(new JLabel("Phone:")); add(ph);
        add(new JLabel("Car Model:")); add(md); 
        add(new JLabel("Plate No:")); add(pl);
        
        JButton sub = new JButton("Register");
        JButton back = new JButton("Back");
        add(sub); add(back);
        
        sub.addActionListener(e -> {
            if(f.ctrl.register(nm.getText(), em.getText(), new String(pw.getPassword()), ph.getText(), md.getText(), pl.getText())) {
                JOptionPane.showMessageDialog(this, "Registration Successful!\nWait for Admin Approval.");
                f.goLog();
            } else {
                JOptionPane.showMessageDialog(this, "Registration Failed.\nEmail might be already taken.");
            }
        });
        
        back.addActionListener(e -> f.goLog());
    }
}

class DashP extends JPanel {
    private MainDriverFrame frame;
    private JTabbedPane tabs;

    public DashP(MainDriverFrame f) {
        this.frame = f;
        setLayout(new BorderLayout());
        
        tabs = new JTabbedPane();
        tabs.addTab("My Jobs", new JobsTab(frame));
        tabs.addTab("Profile", new ProfileTab(frame));
        
        add(tabs, BorderLayout.CENTER);
        
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            frame.ctrl.logout();
            frame.goLog();
        });
        add(logout, BorderLayout.NORTH);
    }
}

class JobsTab extends JPanel {
    public JobsTab(MainDriverFrame f) {
        setLayout(new BorderLayout());
        
        String[] cols = {"ID","Route","Status","Fee"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);
        
        JPanel bot = new JPanel();
        JButton ref = new JButton("Refresh");
        JButton upd = new JButton("Update Status");
        bot.add(ref); bot.add(upd);
        add(bot, BorderLayout.SOUTH);
        
        // Top Info
        JLabel info = new JLabel();
        info.setHorizontalAlignment(SwingConstants.CENTER);
        info.setBorder(new EmptyBorder(10,0,10,0));
        add(info, BorderLayout.NORTH);

        // Refresh Action
        ref.addActionListener(e -> {
            model.setRowCount(0);
            DriverSession s = f.ctrl.getSession();
            info.setText("<html>Driver: <b>" + s.name + "</b> | Earnings: <b>$" + s.earnings + "</b></html>");
            
            for(DriverJob j : f.ctrl.getMyJobs()) {
                model.addRow(new Object[]{j.id, j.route, j.status, j.fee});
            }
        });

        // Update Action
        upd.addActionListener(e -> {
            int r = table.getSelectedRow();
            if(r < 0) {
                JOptionPane.showMessageDialog(this, "Select a job first.");
                return;
            }
            int id = (int)model.getValueAt(r, 0);
            String[] ops = {"Picked Up", "In Transit", "COMPLETED", "CANCELLED"};
            String s = (String) JOptionPane.showInputDialog(this, "Set Status:", "Update", JOptionPane.QUESTION_MESSAGE, null, ops, ops[0]);
            
            if(s != null) {
                f.ctrl.updateStatus(id, s);
                ref.doClick();
            }
        });
        
        // Auto Load
        ref.doClick();
    }
}

class ProfileTab extends JPanel {
    public ProfileTab(MainDriverFrame f) {
        setLayout(new GridLayout(6,2, 10, 10));
        setBorder(new EmptyBorder(20,20,20,20));
        
        JTextField nm=new JTextField(), ph=new JTextField(), em=new JTextField();
        JTextField car=new JTextField(), pl=new JTextField();
        car.setEditable(false); pl.setEditable(false); // Vehicle info read-only for simplicity
        
        add(new JLabel("Name:")); add(nm);
        add(new JLabel("Phone:")); add(ph);
        add(new JLabel("Email:")); add(em);
        add(new JLabel("Vehicle:")); add(car);
        add(new JLabel("Plate:")); add(pl);
        
        JButton load = new JButton("Load Info");
        JButton save = new JButton("Save");
        add(load); add(save);
        
        load.addActionListener(e -> {
            DriverSession s = f.ctrl.getSession();
            nm.setText(s.name);
            ph.setText(s.phone);
            em.setText(s.email);
            
            DriverVehicleInfo v = f.ctrl.getVehicleInfo();
            car.setText(v.model);
            pl.setText(v.plate);
        });
        
        save.addActionListener(e -> {
            if(f.ctrl.updateProfile(nm.getText(), ph.getText(), em.getText())) {
                JOptionPane.showMessageDialog(this, "Profile Updated!");
            } else {
                JOptionPane.showMessageDialog(this, "Update Failed (Email might be duplicate).");
            }
        });
        
        load.doClick();
    }
}