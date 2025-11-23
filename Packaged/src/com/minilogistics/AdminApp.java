package com.minilogistics.main;

import com.minilogistics.view.AdminViews;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class AdminApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new AdminViews.MainFrame().setVisible(true);
        });
    }
}