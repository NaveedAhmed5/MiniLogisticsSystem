package com.minilogistics.main;

import com.minilogistics.view.DriverViews;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class DriverApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { 
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); 
            } catch (Exception e) {}
            new DriverViews.MainDriverFrame().setVisible(true);
        });
    }
}