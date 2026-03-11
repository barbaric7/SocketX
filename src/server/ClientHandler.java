package server;

import common.Message;
import common.Message.Type;
import database.DatabaseManager;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * ClientHandler runs in its own thread and manages communication
 * with a single connected client.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Map<String, ClientHandler> clients;   // shared map
    private final DatabaseManager db;

    private ObjectInputStream  in;
    private ObjectOutputStream out;
    private String username;

    public ClientHandler(Socket socket, Map<String, ClientHandler> clients, DatabaseManager db) {
        this.socket  = socket;
        this.clients = clients;
        this.db      = db;
    }

    // ── Main loop ────────────────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            // First message must be LOGIN or REGISTER
            handleLogin();

            // Process subsequent messages
            Message msg;
            while ((msg = (Message) in.readObject()) != null) {
                System.out.println("[Server] " + msg.getType() + " from " + username);
                switch (msg.getType()) {
                    case BROADCAST     -> handleBroadcast(msg);
                    case PRIVATE       -> handlePrivate(msg);
                    case FILE_REQUEST  -> handleFileTransfer(msg);
                    case CHAT_HISTORY  -> sendChatHistory(msg);
                    case LOGOUT        -> { disconnect(); return; }
                    default            -> {}
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            System.out.println("[Server] Client disconnected: " + username);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Server] Error with client " + username + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    // ── Login / Register ─────────────────────────────────────────────────────

    private void handleLogin() throws IOException, ClassNotFoundException {
        Message msg = (Message) in.readObject();

        String user = msg.getSender();
        String pass = msg.getContent();

        if (msg.getType() == Type.LOGIN) {
            if (!db.validateLogin(user, pass)) {
                send(new Message(Type.ERROR, "Server", user, "Invalid username or password."));
                socket.close();
                return;
            }
            if (clients.containsKey(user)) {
                send(new Message(Type.ERROR, "Server", user, "User already logged in."));
                socket.close();
                return;
            }
        } else {
            // REGISTER treated as message type SUCCESS path
            if (db.userExists(user)) {
                send(new Message(Type.ERROR, "Server", user, "Username already taken."));
                socket.close();
                return;
            }
            if (!db.registerUser(user, pass)) {
                send(new Message(Type.ERROR, "Server", user, "Registration failed."));
                socket.close();
                return;
            }
        }

        this.username = user;
        clients.put(username, this);

        // Confirm login
        send(new Message(Type.SUCCESS, "Server", username, "Welcome, " + username + "!"));

        // Send current online user list to this client
        broadcastUserList();

        // Notify other clients
        Message joinMsg = new Message(Type.BROADCAST, "Server", null,
                "🟢 " + username + " has joined the chat.");
        broadcastToOthers(joinMsg);

        System.out.println("[Server] " + username + " logged in. Online: " + clients.size());
    }

    // ── Message Handlers ─────────────────────────────────────────────────────

    private void handleBroadcast(Message msg) {
        db.saveMessage(msg.getSender(), null, msg.getContent());
        broadcastToAll(msg);
    }

    private void handlePrivate(Message msg) {
        db.saveMessage(msg.getSender(), msg.getReceiver(), msg.getContent());
        ClientHandler target = clients.get(msg.getReceiver());
        if (target != null) {
            target.send(msg);
            // Echo back to sender so they see it in their chat
            send(msg);
        } else {
            send(new Message(Type.ERROR, "Server", username,
                    "User '" + msg.getReceiver() + "' is not online."));
        }
    }

    private void handleFileTransfer(Message msg) {
        ClientHandler target = clients.get(msg.getReceiver());
        if (target != null) {
            System.out.println("[Server] Routing file '" + msg.getFileName() +
                               "' from " + username + " to " + msg.getReceiver());
            target.send(msg);
            send(new Message(Type.SUCCESS, "Server", username,
                    "File '" + msg.getFileName() + "' sent to " + msg.getReceiver()));
        } else {
            send(new Message(Type.ERROR, "Server", username,
                    "User '" + msg.getReceiver() + "' is not online."));
        }
    }

    private void sendChatHistory(Message msg) {
        String other = msg.getContent(); // "BROADCAST" or a username
        List<String> history = "BROADCAST".equals(other)
                ? db.getBroadcastHistory(50)
                : db.getPrivateHistory(username, other, 50);

        StringBuilder sb = new StringBuilder();
        for (String line : history) sb.append(line).append("\n");

        send(new Message(Type.CHAT_HISTORY, "Server", username,
                sb.length() > 0 ? sb.toString() : "(No history found)"));
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────

    /** Send to all connected clients including self. */
    private void broadcastToAll(Message msg) {
        synchronized (clients) {
            for (ClientHandler handler : clients.values()) {
                handler.send(msg);
            }
        }
    }

    /** Send to all connected clients except self. */
    private void broadcastToOthers(Message msg) {
        synchronized (clients) {
            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                if (!entry.getKey().equals(username)) {
                    entry.getValue().send(msg);
                }
            }
        }
    }

    /** Broadcast updated online-user list to ALL clients. */
    void broadcastUserList() {
        String userList;
        synchronized (clients) {
            userList = String.join(",", clients.keySet());
        }
        Message listMsg = new Message(Type.USER_LIST, "Server", null, userList);
        synchronized (clients) {
            for (ClientHandler h : clients.values()) h.send(listMsg);
        }
    }

    // ── Send helper ────────────────────────────────────────────────────────────

    public synchronized void send(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            out.reset(); // prevent caching stale objects
        } catch (IOException e) {
            System.err.println("[Server] Send error to " + username + ": " + e.getMessage());
        }
    }

    // ── Disconnect ─────────────────────────────────────────────────────────────

    private void disconnect() {
        if (username != null) {
            clients.remove(username);
            Message leaveMsg = new Message(Type.BROADCAST, "Server", null,
                    "🔴 " + username + " has left the chat.");
            broadcastToAll(leaveMsg);
            broadcastUserList();
            System.out.println("[Server] " + username + " disconnected. Online: " + clients.size());
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    public String getUsername() { return username; }
}
