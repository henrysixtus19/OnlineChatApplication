import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

    public static void main(String[] args) {

        try (
                Socket socket = new Socket("localhost", 5000);
                BufferedReader input = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()
                        )
                );
                PrintWriter output = new PrintWriter(
                        socket.getOutputStream(), true
                );
                Scanner keyboard = new Scanner(System.in)
        ) {

            System.out.println("Connected to Chat Server!");

            System.out.print("Enter your username: ");

            String username = keyboard.nextLine().trim();

            if (username.isBlank()) {
                username = "Guest";
            }

            // Send the username to the server
            output.println(username);

            System.out.println(
                    "Welcome, " + username + "!"
            );

            System.out.println(
                    "Type /quit to leave the chat.\n"
            );

            // Receive messages in a separate thread
            Thread receiver = new Thread(() -> {

                try {

                    String incomingMessage;

                    while ((incomingMessage = input.readLine()) != null) {
                        System.out.println("\n" + incomingMessage);
                    }

                } catch (IOException e) {

                    System.out.println(
                            "\nDisconnected from the server."
                    );
                }
            });

            receiver.setDaemon(true);
            receiver.start();

            // Send messages while the receiver listens
            while (true) {

                String message = keyboard.nextLine();

                output.println(message);

                if (message.equalsIgnoreCase("/quit")) {
                    System.out.println("Leaving the chat...");
                    break;
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Could not connect to the server."
            );

            System.out.println("Error: " + e.getMessage());
        }
    }
}