package gui;

import javax.swing.*;

/**
 * Main entry point for the Chat Client application.
 * Run this class to start the client GUI.
 */
public class Main {
    public static void main(String[] args) {
        // Use system look and feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default look and feel
        }

        SwingUtilities.invokeLater(LoginWindow::new);
    }
}
