import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

    private static final int PORT = 5000;
    private static Set<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args) {

        System.out.println("Chat Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            while (true) {

                Socket socket = serverSocket.accept();
                System.out.println("New client connected");

                ClientHandler clientHandler = new ClientHandler(socket, clients);
                clients.add(clientHandler);

                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}