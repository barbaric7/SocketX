package server;

import common.Message;
import database.DatabaseManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ChatServer
 * ----------
 * Listens for incoming TCP connections and spawns a ClientHandler thread
 * for each connected client.  A shared ConcurrentMap tracks active clients.
 *
 * Usage:  java server.ChatServer [port]   (default port: 5000)
 */
public class ChatServer {

    public static final int DEFAULT_PORT = 5000;

    private final int port;
    private final Map<String, ClientHandler> clients =
            Collections.synchronizedMap(new HashMap<>());
    private final DatabaseManager db;
    private ServerSocket serverSocket;
    private volatile boolean running = true;

    public ChatServer(int port) {
        this.port = port;
        this.db   = new DatabaseManager();
    }

    // ── Start ─────────────────────────────────────────────────────────────────

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("╔══════════════════════════════════════╗");
            System.out.println("║    Chat & File Transfer Server       ║");
            System.out.println("╚══════════════════════════════════════╝");
            System.out.println("[Server] Listening on port " + port);

            javax.swing.SwingUtilities.invokeLater(() -> {
                new ServerAdminWindow(this.db);
            });

            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] New connection from " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket, clients, db);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            if (running) System.err.println("[Server] Error: " + e.getMessage());
        }
    }

    private void shutdown() {
        running = false;
        System.out.println("[Server] Shutting down...");
        try {
            // Notify all clients
            Message shutdownMsg = new Message(Message.Type.BROADCAST, "Server", null,
                    "⚠ Server is shutting down.");
            synchronized (clients) {
                for (ClientHandler h : clients.values()) h.send(shutdownMsg);
            }
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        db.close();
    }

    // ── Entry point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try { port = Integer.parseInt(args[0]); }
            catch (NumberFormatException e) { System.err.println("Invalid port; using " + DEFAULT_PORT); }
        }
        new ChatServer(port).start();
    }
}
