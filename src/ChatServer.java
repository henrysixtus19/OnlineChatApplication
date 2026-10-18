import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {

    private static final int PORT = 5000;

    private static final AtomicInteger NEXT_USER_ID =
            new AtomicInteger(1);

    private static final Map<Integer, ClientHandler> CLIENTS =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Chat Server Started!");
            System.out.println("Waiting for clients...");
            System.out.println("Server is listening on port " + PORT);

            while (true) {

                Socket socket = serverSocket.accept();

                int userId = NEXT_USER_ID.getAndIncrement();

                ClientHandler handler =
                        new ClientHandler(socket, userId);

                CLIENTS.put(userId, handler);

                handler.start();
            }

        } catch (IOException e) {

            System.out.println(
                    "Server error: " + e.getMessage()
            );
        }
    }

    private static void broadcast(String message) {

        for (ClientHandler client : CLIENTS.values()) {
            client.send(message);
        }
    }

    static class ClientHandler extends Thread {

        private final Socket socket;
        private final int userId;

        private PrintWriter output;
        private String username;

        ClientHandler(Socket socket, int userId) {
            this.socket = socket;
            this.userId = userId;
        }

        synchronized void send(String message) {

            if (output != null) {
                output.println(message);
            }
        }

        @Override
        public void run() {

            try (
                    Socket clientSocket = socket;
                    BufferedReader input = new BufferedReader(
                            new InputStreamReader(
                                    clientSocket.getInputStream()
                            )
                    );
                    PrintWriter writer = new PrintWriter(
                            clientSocket.getOutputStream(), true
                    )
            ) {

                output = writer;

                // Receive the username from the client
                username = input.readLine();

                if (username == null || username.isBlank()) {
                    username = "User" + userId;
                }

                username = username.trim();

                System.out.println(
                        username + " (ID " + userId + ") connected."
                );

                broadcast(
                        "[SERVER] " + username + " joined the chat."
                );

                String message;

                while ((message = input.readLine()) != null) {

                    if (message.equalsIgnoreCase("/quit")) {
                        break;
                    }

                    if (!message.isBlank()) {

                        String formattedMessage =
                                "[" + username + " | ID "
                                        + userId + "] " + message;

                        System.out.println(formattedMessage);

                        broadcast(formattedMessage);
                    }
                }

            } catch (IOException e) {

                System.out.println(
                        "Connection ended for User " + userId
                );

            } finally {

                CLIENTS.remove(userId);

                broadcast(
                        "[SERVER] "
                                + (username == null
                                ? "User" + userId : username)
                                + " left the chat."
                );
            }
        }
    }
}