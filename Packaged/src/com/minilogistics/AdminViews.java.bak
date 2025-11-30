package com.minilogistics.view;

import com.minilogistics.controller.AdminPortalController;
import com.minilogistics.model.AdminEntities.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

public class AdminViews {

    public static class MainFrame extends JFrame {
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

    public static class DriverPanel extends JPanel {
        public DriverPanel(AdminPortalController ctrl) {
            setLayout(new BorderLayout());
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Name", "Status", "Jobs", "Rating", "Vehicle"}, 0) {
                 public boolean isCellEditable(int r, int c) { return false; }
            };
            JTable table = new JTable(model);
            add(new JScrollPane(table), BorderLayout.CENTER);
            JPanel bot = new JPanel();
            JButton ref = new JButton("Refresh"), act = new JButton("Activate"), susp = new JButton("Suspend");
            bot.add(ref); bot.add(act); bot.add(susp);
            add(bot, BorderLayout.SOUTH);

            ref.addActionListener(e -> {
                model.setRowCount(0);
                for(AdminDriverEntity d : ctrl.getDrivers()) model.addRow(new Object[]{d.id, d.name, d.status, d.activeJobs, d.rating, d.vehicle.getDetails()});
            });
            
            JTable finalTable = table;
            act.addActionListener(e -> update(finalTable, ctrl, "Active"));
            susp.addActionListener(e -> update(finalTable, ctrl, "Suspended"));
            ref.doClick();
        }
        void update(JTable t, AdminPortalController c, String s) {
            int r = t.getSelectedRow();
            if(r >= 0 && c.updateDriverStatus((int)t.getValueAt(r, 0), s, "Manual")) JOptionPane.showMessageDialog(this, "Updated!");
            else JOptionPane.showMessageDialog(this, "Failed (Active jobs?)");
            ((JButton)((JPanel)getComponent(1)).getComponent(0)).doClick(); 
        }
    }

    public static class AssignPanel extends JPanel {
        public AssignPanel(AdminPortalController ctrl) {
            setLayout(new BorderLayout());
            DefaultTableModel model = new DefaultTableModel(new String[]{"ID","Desc","Route","Status","Driver","Pri","Deadline"},0) {
                 public boolean isCellEditable(int r, int c) { return false; }
            };
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
            
            JTable finalTable = table;
            ref.doClick();

            assign.addActionListener(e -> {
                int r = finalTable.getSelectedRow();
                if(r >= 0) {
                    try {
                        JOptionPane.showMessageDialog(this, ctrl.assignDriver((int)model.getValueAt(r, 0), Integer.parseInt(drvId.getText()), "High", 24));
                        ref.doClick();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Invalid Driver ID format.");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Select a delivery first.");
                }
            });
        }
    }

    public static class LogPanel extends JPanel {
        public LogPanel(AdminPortalController ctrl) {
            setLayout(new BorderLayout());
            DefaultTableModel model = new DefaultTableModel(new String[]{"Time","Cat","Details"},0) {
                 public boolean isCellEditable(int r, int c) { return false; }
            };
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

    public static class ReportPanel extends JPanel {
        public ReportPanel(AdminPortalController ctrl) {
            setLayout(new BorderLayout());
            JTextArea area = new JTextArea(); 
            add(new JScrollPane(area));
            JPanel top = new JPanel();
            JButton gen = new JButton("Generate Driver Report");
            JButton exp = new JButton("Export Report");

            top.add(gen);
            top.add(exp);
            add(top, BorderLayout.NORTH);
            
            gen.addActionListener(e -> area.setText(ctrl.generateReport("Drivers")));

            exp.addActionListener(e -> {
                try(PrintWriter w = new PrintWriter(new FileWriter("admin_report.txt"))) {
                    w.print(area.getText());
                    JOptionPane.showMessageDialog(this, "Report saved successfully to 'admin_report.txt'");
                } catch(IOException ex) {
                    JOptionPane.showMessageDialog(this, "Error saving file.");
                }
            });
        }
    }
}