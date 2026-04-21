package server;

import database.DatabaseManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class ServerAdminWindow extends JFrame {

    private final DatabaseManager db;
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userList = new JList<>(userListModel);
    private final JTextField newPasswordField = new JTextField(15);
    
    public ServerAdminWindow(DatabaseManager db) {
        super("Server Admin - User Management");
        this.db = db;
        
        buildUI();
        refreshUserList();
        
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(450, 350);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JLabel header = new JLabel("Registered Users", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        root.add(header, BorderLayout.NORTH);

        // List of users
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(userList);
        root.add(scrollPane, BorderLayout.CENTER);

        // Controls at the bottom
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel passLabel = new JLabel("New Password:");
        gbc.gridx = 0; gbc.gridy = 0;
        controlPanel.add(passLabel, gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        controlPanel.add(newPasswordField, gbc);

        JButton updateBtn = new JButton("Update Password");
        updateBtn.addActionListener(e -> changePassword());
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        controlPanel.add(updateBtn, gbc);
        
        JButton deleteBtn = new JButton("Delete User");
        deleteBtn.setForeground(Color.RED);
        deleteBtn.addActionListener(e -> deleteSelectedUser());
        gbc.gridy = 2;
        controlPanel.add(deleteBtn, gbc);

        root.add(controlPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void refreshUserList() {
        userListModel.clear();
        List<String> users = db.getAllUsernames();
        for (String u : users) {
            userListModel.addElement(u);
        }
    }

    private void changePassword() {
        String selectedUser = userList.getSelectedValue();
        String newPass = newPasswordField.getText().trim();

        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user first.");
            return;
        }
        if (newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.");
            return;
        }

        if (db.updatePassword(selectedUser, newPass)) {
            JOptionPane.showMessageDialog(this, "Password updated for " + selectedUser);
            newPasswordField.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update password.");
        }
    }

    private void deleteSelectedUser() {
        String selectedUser = userList.getSelectedValue();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete user: " + selectedUser + "?", 
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                
        if (confirm == JOptionPane.YES_OPTION) {
            if (db.deleteUser(selectedUser)) {
                refreshUserList();
                JOptionPane.showMessageDialog(this, "User deleted successfully.");
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete user.");
            }
        }
    }
}