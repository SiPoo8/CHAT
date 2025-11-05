import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {
    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;

    public Server() {
        connections = new ArrayList<>();
        done = false;
    }


    @Override
    public void run() {
        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();
            while (!done) {
                Socket client = server.accept();
                ConnectionHandler Handler = new ConnectionHandler(client);
                connections.add(Handler);
                pool.execute(Handler);
            }
        } catch (Exception e) {
            shutdown();
        }

    }

    public void broadcast(String message) {
        for (ConnectionHandler connection : connections) {
            if (connection != null) {
                connection.sendMessage(message);
            }
        }
    }

    public void shutdown() {
        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }
            for (ConnectionHandler connection : connections) {
                connection.shutdown();
            }
        } catch (IOException e) {
            // Not Handleble
        }
    }


    class ConnectionHandler implements Runnable {

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String name;


        public ConnectionHandler(Socket client) {
            this.client = client;
        }


        @Override
        public void run() {
            try {
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                out.println("Please enter your name:");
                name = in.readLine();
                System.out.println(name + " joined the server");
                broadcast("Welcome " + name);
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/name")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(name + " renamed themselves to" + messageSplit[1]);
                            System.out.println(name + " renamed themselves to" + messageSplit[1]);
                            name = messageSplit[1];
                            out.println("Successfully changed name to" + messageSplit[1]);
                        } else {
                            out.println("No name provided");
                        }

                    } else if (message.startsWith("/quit")) {
                        broadcast(name + "left the server");
                        shutdown();
                    } else {
                        broadcast(name + ": " + message);
                    }
                }
            } catch (Exception e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {
            try {
                in.close();
                out.close();

                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                // Cant be handled
            }

        }


    }


    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }





}
