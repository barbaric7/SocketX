import java.net.*;
import java.io.*;
import java.util.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private Set<ClientHandler> clients;

    public ClientHandler(Socket socket, Set<ClientHandler> clients) {

        this.socket = socket;
        this.clients = clients;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            writer = new PrintWriter(
                    socket.getOutputStream(), true);

            username = reader.readLine();

            System.out.println(username + " joined the chat");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {

        String message;

        try {

            while ((message = reader.readLine()) != null) {

                String fullMessage = username + ": " + message;
                System.out.println(fullMessage);

                broadcast(fullMessage);

            }

        } catch (IOException e) {
            System.out.println(username + " disconnected");
        }
    }

    private void broadcast(String message) {

        for (ClientHandler client : clients) {

            if (client != this) {
                client.writer.println(message);
            }

        }
    }
}