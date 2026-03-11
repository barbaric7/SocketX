package client;

import common.Message;
import common.Message.Type;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * ChatClient manages the TCP socket connection to the server.
 * Incoming messages are delivered to the registered messageHandler callback.
 */
public class ChatClient {

    private final String host;
    private final int    port;

    private Socket           socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;

    private volatile boolean connected = false;
    private Consumer<Message> messageHandler;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Set callback invoked for every message received from the server. */
    public void setMessageHandler(Consumer<Message> handler) {
        this.messageHandler = handler;
    }

    // ── Connect ───────────────────────────────────────────────────────────────

    /**
     * Establish TCP connection.
     * @return true if socket opened successfully.
     */
    public boolean connect() {
        try {
            socket = new Socket(host, port);
            out    = new ObjectOutputStream(socket.getOutputStream());
            in     = new ObjectInputStream(socket.getInputStream());
            connected = true;
            startReceiver();
            return true;
        } catch (IOException e) {
            System.err.println("[Client] Cannot connect to " + host + ":" + port + " – " + e.getMessage());
            return false;
        }
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    public synchronized void send(Message msg) {
        if (!connected) return;
        try {
            out.writeObject(msg);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("[Client] Send error: " + e.getMessage());
            connected = false;
        }
    }

    // ── Convenience send methods ──────────────────────────────────────────────

    public void sendLogin(String username, String password) {
        send(new Message(Type.LOGIN, username, null, password));
    }

    public void sendRegister(String username, String password) {
        // Re-use SUCCESS type as registration signal; server checks type
        Message msg = new Message(Type.SUCCESS, username, null, password);
        msg.setType(Type.SUCCESS);
        // Use a dedicated registration message
        Message reg = new Message(Type.LOGIN, username, null, password);
        reg.setType(Type.SUCCESS); // we distinguish by checking Type.SUCCESS on server
        // Simpler: use a separate flag via content prefix
        send(buildRegisterMessage(username, password));
    }

    private Message buildRegisterMessage(String username, String password) {
        // We use a special Message type. Since we only have LOGIN/SUCCESS we repurpose:
        // Sending Type.SUCCESS from client signals "register".
        Message msg = new Message(Type.SUCCESS, username, null, password);
        return msg;
    }

    public void sendBroadcast(String username, String text) {
        send(new Message(Type.BROADCAST, username, null, text));
    }

    public void sendPrivate(String username, String receiver, String text) {
        send(new Message(Type.PRIVATE, username, receiver, text));
    }

    public void sendFile(String username, String receiver, String fileName, byte[] data) {
        Message msg = new Message(Type.FILE_REQUEST, username, receiver, "File transfer");
        msg.setFileName(fileName);
        msg.setFileData(data);
        msg.setFileSize(data.length);
        send(msg);
    }

    public void requestBroadcastHistory(String username) {
        send(new Message(Type.CHAT_HISTORY, username, null, "BROADCAST"));
    }

    public void requestPrivateHistory(String username, String other) {
        send(new Message(Type.CHAT_HISTORY, username, null, other));
    }

    public void logout(String username) {
        send(new Message(Type.LOGOUT, username, null, "bye"));
        disconnect();
    }

    // ── Receiver thread ───────────────────────────────────────────────────────

    private void startReceiver() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Message msg = (Message) in.readObject();
                    if (messageHandler != null) messageHandler.accept(msg);
                }
            } catch (EOFException | java.net.SocketException e) {
                System.out.println("[Client] Disconnected from server.");
            } catch (IOException | ClassNotFoundException e) {
                if (connected) System.err.println("[Client] Receive error: " + e.getMessage());
            } finally {
                connected = false;
            }
        }, "receiver-thread");
        t.setDaemon(true);
        t.start();
    }

    // ── Disconnect ────────────────────────────────────────────────────────────

    public void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}
