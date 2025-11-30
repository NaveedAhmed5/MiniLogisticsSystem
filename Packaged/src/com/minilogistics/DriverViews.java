package com.minilogistics.view;

import com.minilogistics.controller.DriverAppController;
import com.minilogistics.model.DriverEntities.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class DriverViews {

    public static class MainDriverFrame extends JFrame {
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
            add(main);
        }

        void goDash() {
            main.add(new DashP(this), "DASH");
            cards.show(main, "DASH");
        }

        void goReg() {
            cards.show(main, "REG");
        }

        void goLog() {
            cards.show(main, "LOGIN");
        }
    }

    public static class LoginP extends JPanel {
        public LoginP(MainDriverFrame f) {
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("Driver Login");
            title.setFont(new Font("Arial", Font.BOLD, 24));
            title.setHorizontalAlignment(SwingConstants.CENTER);

            JTextField em = new JTextField("bob", 15);
            JPasswordField pw = new JPasswordField("pass", 15);
            JButton log = new JButton("Login"), reg = new JButton("Create Account");

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            add(title, gbc);

            gbc.gridwidth = 1;
            gbc.gridy = 1;
            add(new JLabel("Email:"), gbc);
            gbc.gridx = 1;
            add(em, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            add(pw, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 2;
            add(log, gbc);
            gbc.gridy = 4;
            add(reg, gbc);

            log.addActionListener(e -> {
                String res = f.ctrl.login(em.getText(), new String(pw.getPassword()));
                if ("Success".equals(res))
                    f.goDash();
                else
                    JOptionPane.showMessageDialog(this, res);
            });
            reg.addActionListener(e -> f.goReg());
        }
    }

    public static class RegP extends JPanel {
        public RegP(MainDriverFrame f) {
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel title = new JLabel("Driver Registration");
            title.setFont(new Font("Arial", Font.BOLD, 20));
            title.setHorizontalAlignment(SwingConstants.CENTER);

            JTextField nm = new JTextField(15), em = new JTextField(15), ph = new JTextField(15),
                    md = new JTextField(15), pl = new JTextField(15);
            JPasswordField pw = new JPasswordField(15);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            add(title, gbc);

            gbc.gridwidth = 1;
            int y = 1;
            addLabelField(this, gbc, "Name:", nm, y++);
            addLabelField(this, gbc, "Email:", em, y++);
            addLabelField(this, gbc, "Password:", pw, y++);
            addLabelField(this, gbc, "Phone:", ph, y++);
            addLabelField(this, gbc, "Car Model:", md, y++);
            addLabelField(this, gbc, "Plate No:", pl, y++);

            JButton sub = new JButton("Register");
            JButton back = new JButton("Back");

            gbc.gridx = 0;
            gbc.gridy = y;
            gbc.gridwidth = 2;
            add(sub, gbc);
            gbc.gridy = y + 1;
            add(back, gbc);

            sub.addActionListener(e -> {
                String result = f.ctrl.register(nm.getText(), em.getText(), new String(pw.getPassword()), ph.getText(),
                        md.getText(), pl.getText());
                if ("Success".equals(result)) {
                    // No success popup, just redirect
                    f.goLog();
                } else {
                    JOptionPane.showMessageDialog(this, "Registration Failed:\n" + result);
                }
            });
            back.addActionListener(e -> f.goLog());
        }

        private void addLabelField(JPanel p, GridBagConstraints gbc, String lbl, Component cmp, int y) {
            gbc.gridx = 0;
            gbc.gridy = y;
            p.add(new JLabel(lbl), gbc);
            gbc.gridx = 1;
            p.add(cmp, gbc);
        }
    }

    public static class DashP extends JPanel {
        public DashP(MainDriverFrame f) {
            setLayout(new BorderLayout());
            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("My Jobs", new JobsTab(f));
            tabs.addTab("Profile", new ProfileTab(f));
            add(tabs, BorderLayout.CENTER);

            JButton logout = new JButton("Logout");
            logout.addActionListener(e -> {
                f.ctrl.logout();
                f.goLog();
            });
            add(logout, BorderLayout.NORTH);
        }
    }

    public static class JobsTab extends JPanel {
        public JobsTab(MainDriverFrame f) {
            setLayout(new BorderLayout());
            String[] cols = { "ID", "Route", "Status", "Fee" };
            DefaultTableModel model = new DefaultTableModel(cols, 0) {
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };
            JTable table = new JTable(model);
            add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bot = new JPanel();
            JButton ref = new JButton("Refresh"), upd = new JButton("Update Status");
            bot.add(ref);
            bot.add(upd);
            add(bot, BorderLayout.SOUTH);

            JLabel info = new JLabel();
            info.setHorizontalAlignment(SwingConstants.CENTER);
            info.setBorder(new EmptyBorder(10, 0, 10, 0));
            add(info, BorderLayout.NORTH);

            ref.addActionListener(e -> {
                model.setRowCount(0);
                DriverSession s = f.ctrl.getSession();
                info.setText("<html>Driver: <b>" + s.name + "</b> | Earnings: <b>$" + String.format("%.2f", s.earnings)
                        + "</b></html>");
                for (DriverJob j : f.ctrl.getMyJobs())
                    model.addRow(new Object[] { j.id, j.route, j.status, j.fee });
            });

            upd.addActionListener(e -> {
                int r = table.getSelectedRow();
                if (r < 0) {
                    JOptionPane.showMessageDialog(this, "Select a job first.");
                    return;
                }
                int id = (int) model.getValueAt(r, 0);
                String[] ops = { "Picked Up", "In Transit", "COMPLETED", "CANCELLED" };
                String s = (String) JOptionPane.showInputDialog(this, "Set Status:", "Update",
                        JOptionPane.QUESTION_MESSAGE, null, ops, ops[0]);
                if (s != null) {
                    f.ctrl.updateStatus(id, s);
                    ref.doClick();
                }
            });
            ref.doClick();

            // Auto-refresh every 5 seconds
            new Timer(5000, e -> ref.doClick()).start();
        }
    }

    public static class ProfileTab extends JPanel {
        public ProfileTab(MainDriverFrame f) {
            setLayout(new GridLayout(6, 2, 10, 10));
            setBorder(new EmptyBorder(20, 20, 20, 20));

            JTextField nm = new JTextField(), ph = new JTextField(), em = new JTextField();
            JTextField car = new JTextField(), pl = new JTextField();
            car.setEditable(false);
            pl.setEditable(false);

            add(new JLabel("Name:"));
            add(nm);
            add(new JLabel("Phone:"));
            add(ph);
            add(new JLabel("Email:"));
            add(em);
            add(new JLabel("Vehicle:"));
            add(car);
            add(new JLabel("Plate:"));
            add(pl);

            JButton load = new JButton("Load Info"), save = new JButton("Save");
            add(load);
            add(save);

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
                if (f.ctrl.updateProfile(nm.getText(), ph.getText(), em.getText())) {
                    // Success - no popup
                } else
                    JOptionPane.showMessageDialog(this, "Update Failed (Email might be duplicate).");
            });
            load.doClick();
        }
    }
}