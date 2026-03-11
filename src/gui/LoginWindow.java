package gui;

import client.ChatClient;
import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * LoginWindow — the first screen users see.
 * Allows login to an existing account or registration of a new one.
 */
public class LoginWindow extends JFrame {

    private final JTextField     hostField     = new JTextField("localhost", 12);
    private final JTextField     portField     = new JTextField("5000", 6);
    private final JTextField     usernameField = new JTextField(15);
    private final JPasswordField passwordField = new JPasswordField(15);
    private final JButton        loginBtn      = new JButton("Login");
    private final JButton        registerBtn   = new JButton("Register");
    private final JLabel         statusLabel   = new JLabel(" ");

    public LoginWindow() {
        super("Chat App – Login");
        buildUI();
        attachListeners();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(20, 30, 20, 30));
        root.setBackground(new Color(245, 247, 250));

        // Header
        JLabel header = new JLabel("💬 ChatApp", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.setForeground(new Color(63, 81, 181));
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(root.getBackground());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(6, 6, 6, 6);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        int row = 0;
        addRow(form, gbc, row++, "Server Host:", hostField);
        addRow(form, gbc, row++, "Port:",        portField);
        addRow(form, gbc, row++, "Username:",    usernameField);
        addRow(form, gbc, row++, "Password:",    passwordField);

        root.add(form, BorderLayout.CENTER);

        // Buttons + status
        JPanel south = new JPanel(new GridLayout(2, 1, 5, 5));
        south.setBackground(root.getBackground());

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setBackground(root.getBackground());
        styleButton(loginBtn,   new Color(63, 81, 181), Color.WHITE);
        styleButton(registerBtn, new Color(76, 175, 80), Color.WHITE);
        btnRow.add(loginBtn);
        btnRow.add(registerBtn);

        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(Color.RED);

        south.add(btnRow);
        south.add(statusLabel);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row,
                        String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        panel.add(field, gbc);
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 36));
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void attachListeners() {
        loginBtn.addActionListener(e -> attempt(false));
        registerBtn.addActionListener(e -> attempt(true));

        // Enter key submits
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attempt(false);
            }
        };
        usernameField.addKeyListener(enter);
        passwordField.addKeyListener(enter);
    }

    private void attempt(boolean register) {
        String host     = hostField.getText().trim();
        String portText = portField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (host.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("All fields are required.");
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            statusLabel.setText("Invalid port number.");
            return;
        }

        loginBtn.setEnabled(false);
        registerBtn.setEnabled(false);
        statusLabel.setForeground(new Color(33, 150, 243));
        statusLabel.setText("Connecting...");

        ChatClient client = new ChatClient(host, port);

        // One-shot handler: wait for server response to login/register
        client.setMessageHandler(msg -> {
            SwingUtilities.invokeLater(() -> {
                if (msg.getType() == Message.Type.SUCCESS) {
                    statusLabel.setForeground(new Color(56, 142, 60));
                    statusLabel.setText("Connected!");
                    // Open main chat window
                    new ChatWindow(client, username);
                    dispose();
                } else if (msg.getType() == Message.Type.ERROR) {
                    statusLabel.setForeground(Color.RED);
                    statusLabel.setText(msg.getContent());
                    loginBtn.setEnabled(true);
                    registerBtn.setEnabled(true);
                    client.disconnect();
                } else {
                    // Could be USER_LIST arriving right after SUCCESS — forward to new window
                    // (handled by ChatWindow's own handler; ignore here)
                }
            });
        });

        if (!client.connect()) {
            statusLabel.setForeground(Color.RED);
            statusLabel.setText("Cannot reach server at " + host + ":" + port);
            loginBtn.setEnabled(true);
            registerBtn.setEnabled(true);
            return;
        }

        // Send credentials
        if (register) {
            // Build register message (Type.SUCCESS from client = register signal)
            Message regMsg = new Message(Message.Type.SUCCESS, username, null, password);
            client.send(regMsg);
        } else {
            client.sendLogin(username, password);
        }
    }

    // ── Entry point (for testing LoginWindow alone) ───────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(LoginWindow::new);
    }
}
