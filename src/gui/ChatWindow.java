package gui;

import client.ChatClient;
import common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;

/**
 * ChatWindow — the main application window after a successful login.
 *
 * Layout:
 * ┌─────────────────────────────────────────┐
 * │  Header bar  (username + status)        │
 * ├─────────────────┬───────────────────────┤
 * │  Online Users   │   Chat Area           │
 * │  (left panel)   │                       │
 * │                 ├───────────────────────┤
 * │                 │  Input row            │
 * └─────────────────┴───────────────────────┘
 */
public class ChatWindow extends JFrame {

    // ── State ─────────────────────────────────────────────────────────────────
    private final ChatClient client;
    private final String     myUsername;
    private String           selectedUser = null; // null = broadcast

    // ── Swing components ──────────────────────────────────────────────────────
    private final JTextArea   chatArea      = new JTextArea();
    private final JTextField  inputField    = new JTextField();
    private final JButton     sendBtn       = new JButton("Send");
    private final JButton     fileBtn       = new JButton("📎 File");
    private final JButton     historyBtn    = new JButton("🕑 History");
    private final JList<String> userList    = new JList<>();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JLabel      chatTargetLbl = new JLabel("💬 Global Chat");
    private final JLabel      statusBar     = new JLabel(" ");

    // ── Constructor ───────────────────────────────────────────────────────────

    public ChatWindow(ChatClient client, String username) {
        super("ChatApp — " + username);
        this.client     = client;
        this.myUsername = username;

        buildUI();
        attachListeners();

        // Hand off message handling from LoginWindow → this window
        client.setMessageHandler(this::handleMessage);

        // Ask for broadcast history on open
        client.requestBroadcastHistory(myUsername);

        // Ask for online users once window is ready
        client.requestUserList();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });

        setPreferredSize(new Dimension(900, 620));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── UI Construction ───────────────────────────────────────────────────────

    private void buildUI() {
        Color BG        = new Color(245, 247, 250);
        Color ACCENT    = new Color(63, 81, 181);
        Color CHAT_BG   = Color.WHITE;

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel titleLbl = new JLabel("💬 ChatApp");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLbl.setForeground(Color.WHITE);

        JLabel userLbl = new JLabel("Logged in as: " + myUsername);
        userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userLbl.setForeground(new Color(200, 210, 255));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBackground(new Color(233, 30, 99));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setOpaque(true);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.addActionListener(e -> confirmExit());

        header.add(titleLbl, BorderLayout.WEST);
        header.add(userLbl,  BorderLayout.CENTER);
        header.add(logoutBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ── Center: user list + chat ──
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(180);
        split.setDividerSize(5);

        // Left panel: online users
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(BG);
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220)),
                "Online Users", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), ACCENT));

        userList.setModel(userListModel);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userList.setSelectionBackground(new Color(197, 202, 233));
        userList.setBackground(CHAT_BG);
        userList.setBorder(new EmptyBorder(4, 4, 4, 4));

        // Show "(Global)" at top
        userListModel.addElement("🌐 Global Chat");

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(null);
        leftPanel.add(userScroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> {
            client.requestBroadcastHistory(myUsername);
            client.requestUserList(); // Fixes the sync bug!
        });
        leftPanel.add(refreshBtn, BorderLayout.SOUTH);

        split.setLeftComponent(leftPanel);

        // Right panel: chat area + input
        JPanel rightPanel = new JPanel(new BorderLayout(0, 0));
        rightPanel.setBackground(BG);

        // Chat target label
        chatTargetLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        chatTargetLbl.setForeground(ACCENT);
        chatTargetLbl.setBorder(new EmptyBorder(6, 12, 6, 12));
        rightPanel.add(chatTargetLbl, BorderLayout.NORTH);

        // Chat area
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBackground(CHAT_BG);
        chatArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 230)));
        rightPanel.add(chatScroll, BorderLayout.CENTER);

        // Input row
        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setBorder(new EmptyBorder(8, 8, 8, 8));
        inputRow.setBackground(BG);

        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 185, 210)),
                new EmptyBorder(6, 8, 6, 8)));

        styleButton(sendBtn,    ACCENT,                Color.WHITE);
        styleButton(fileBtn,    new Color(255, 152, 0), Color.WHITE);
        styleButton(historyBtn, new Color(96, 125, 139), Color.WHITE);

        // Clear button
        JButton clearBtn = new JButton("🗑 Clear");
        styleButton(clearBtn, new Color(244, 67, 54), Color.WHITE);
        clearBtn.addActionListener(e -> chatArea.setText(""));

        JPanel btnPanel = new JPanel(new GridLayout(1, 4, 5, 0)); // Changed to accommodate 4 buttons
        btnPanel.setBackground(BG);
        btnPanel.add(sendBtn);
        btnPanel.add(fileBtn);
        btnPanel.add(historyBtn);
        btnPanel.add(clearBtn);

        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(btnPanel,   BorderLayout.EAST);
        rightPanel.add(inputRow, BorderLayout.SOUTH);

        split.setRightComponent(rightPanel);
        root.add(split, BorderLayout.CENTER);

        // ── Status bar ──
        statusBar.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusBar.setForeground(Color.GRAY);
        statusBar.setBorder(new EmptyBorder(2, 10, 2, 10));
        root.add(statusBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void styleButton(JButton btn, Color bg, Color fg) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(90, 34));
    }

    // ── Listeners ─────────

    private void attachListeners() {
        // Send on button click
        sendBtn.addActionListener(e -> sendMessage());

        // Send on Enter key
        inputField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });

        // File transfer
        fileBtn.addActionListener(e -> sendFile());

        // History button
        historyBtn.addActionListener(e -> loadHistory());

        // User list selection
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String sel = userList.getSelectedValue();
                if (sel == null || sel.startsWith("🌐")) {
                    selectedUser = null;
                    chatTargetLbl.setText("💬 Global Chat");
                } else {
                    selectedUser = sel.replace("🟢 ", "");
                    chatTargetLbl.setText("🔒 Private chat with " + selectedUser);
                }
                chatArea.setText("");
                loadHistory();
            }
        });
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        if (selectedUser == null) {
            client.sendBroadcast(myUsername, text);
        } else {
            client.sendPrivate(myUsername, selectedUser, text);
        }
        inputField.setText("");
    }

    private void sendFile() {
        if (selectedUser == null) {
            showStatus("⚠ Select a user to send a file.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select file to send to " + selectedUser);
        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (file.length() > 50 * 1024 * 1024) { // 50 MB limit
            showStatus("⚠ File too large (max 50 MB).");
            return;
        }

        showStatus("Sending " + file.getName() + " ...");
        new Thread(() -> {
            try {
                byte[] data = Files.readAllBytes(file.toPath());
                client.sendFile(myUsername, selectedUser, file.getName(), data);
                SwingUtilities.invokeLater(() -> showStatus("✅ File sent: " + file.getName()));
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> showStatus("❌ File read error: " + ex.getMessage()));
            }
        }).start();
    }

    private void loadHistory() {
        if (selectedUser == null) {
            client.requestBroadcastHistory(myUsername);
        } else {
            client.requestPrivateHistory(myUsername, selectedUser);
        }
    }

    private void confirmExit() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Disconnect and exit?", "Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            client.logout(myUsername);
            dispose();
            System.exit(0);
        }
    }

    // ── Message handler (called from network thread) ──────────────────────────

    private void handleMessage(Message msg) {
        SwingUtilities.invokeLater(() -> {
            switch (msg.getType()) {
                case BROADCAST    -> appendChat(msg.toString());
                case PRIVATE      -> appendChat("🔒 " + msg.toString());
                case USER_LIST    -> updateUserList(msg.getContent());
                case CHAT_HISTORY -> {
                    chatArea.setText("");
                    appendChat("─── Chat History ─────────────────────\n" +
                               msg.getContent() +
                               "──────────────────────────────────────");
                }
                case FILE_REQUEST -> promptSaveFile(msg);
                case SUCCESS      -> showStatus("✅ " + msg.getContent());
                case ERROR        -> showStatus("❌ " + msg.getContent());
                default           -> {}
            }
        });
    }

    private void appendChat(String text) {
        chatArea.append(text + "\n");
        // Auto-scroll to bottom
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void updateUserList(String csv) {
        userListModel.clear();
        userListModel.addElement("🌐 Global Chat");
        if (csv != null && !csv.isBlank()) {
            for (String u : csv.split(",")) {
                if (!u.equals(myUsername)) {
                    userListModel.addElement("🟢 " + u);
                }
            }
        }
    }

    private void promptSaveFile(Message msg) {
        int opt = JOptionPane.showConfirmDialog(this,
                msg.getSender() + " is sending you a file: " + msg.getFileName() +
                " (" + (msg.getFileSize() / 1024) + " KB)\n\nDo you want to receive it?",
                "Incoming File", JOptionPane.YES_NO_OPTION);
                
        if (opt == JOptionPane.YES_OPTION) {
            // Find the user's Downloads folder
            String userHome = System.getProperty("user.home");
            File downloadsDir = new File(userHome, "Downloads");
            if (!downloadsDir.exists()) downloadsDir.mkdirs(); // Create it if it somehow doesn't exist
            
            File dest = new File(downloadsDir, msg.getFileName());
            
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(msg.getFileData());
                showStatus("✅ Saved to Downloads: " + msg.getFileName());
                
                // Show in chat that it was downloaded
                appendChat("📎 System: File saved to Downloads -> " + msg.getFileName());
            } catch (IOException ex) {
                showStatus("❌ Save failed: " + ex.getMessage());
            }
        } else {
            // Notify in chat that you declined
            appendChat("📎 System: You declined the file '" + msg.getFileName() + "' from " + msg.getSender());
        }
    }

    private void showStatus(String text) {
        statusBar.setText(text);
        // Clear after 4 seconds
        Timer t = new Timer(4000, e -> statusBar.setText(" "));
        t.setRepeats(false);
        t.start();
    }
}