import java.net.*;
import java.io.*;
import java.util.Scanner;

public class ChatClient {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = scanner.nextLine();

        try {

            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            System.out.println("Connected to chat server");

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(), true);

            writer.println(username);

            Thread receiveThread = new Thread(() -> {

                try {

                    String message;

                    while ((message = reader.readLine()) != null) {
                        System.out.println(message);
                    }

                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }

            });

            receiveThread.start();

            while (true) {

                String message = scanner.nextLine();
                writer.println(message);

            }

        } catch (IOException e) {
            System.out.println("Could not connect to server.");
        }
    }
}