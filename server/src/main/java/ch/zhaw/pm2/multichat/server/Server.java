package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.NetworkHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * This class is responsible for launching a new server instance
 */
public class Server {

    /** Network server for incoming connections. */
    private NetworkHandler.NetworkServer<String> networkServer;

    /** Registry for open connections. */
    private Map<String,ServerConnectionHandler> connections = new HashMap<>();

    /**
     * Create a new server instance.
     * @param serverPort Port to listen on.
     * @throws IOException If the server could not be created.
     */
    private Server(int serverPort) throws IOException {
        System.out.println("Create server connection");
        networkServer = NetworkHandler.createServer(serverPort);
        System.out.printf("Listening on %s:%d%n", networkServer.getHostAddress(), networkServer.getHostPort());
    }

    /**
     * Start the server.
     * Opens a network server and waits for incoming connections.
     * For each connection a new {@link ServerConnectionHandler} is created and started in a new thread.
     * If the network server is closed, all connections are closed and the server is stopped.
     */
    private void start() {
        System.out.println("Server started.");
        ExecutorService executorService = Executors.newCachedThreadPool();
        while (networkServer.isAvailable()) {
            try {
                NetworkHandler.NetworkConnection<String> connection = networkServer.waitForConnection();
                ServerConnectionHandler connectionHandler = new ServerConnectionHandler(connection, connections);
                executorService.submit(connectionHandler::startReceiving);
                System.out.printf("Connected new Client %s with IP:Port <%s:%d>%n",
                    connectionHandler.getUserName(),
                    connection.getRemoteHost(),
                    connection.getRemotePort()
                );
            } catch (IOException e) {
            	System.out.println("Warning: Connect failed " + e.getMessage());
            }
        }
        executorService.shutdown();
        System.out.println("Server Stopped.");
    }

    /**
     * Terminate the server.
     * Closes the network server, which will terminate the server and close all connections.
     */
    private void terminate() {
        try {
            System.out.println("Close server connection.");
            networkServer.close();
        } catch (IOException e) {
        	System.err.println("Failed to close server connection: " + e.getMessage());
        }
    }

    /**
     * Main method for starting the server.
     * It will open a network server on the given port and wait for incoming connections.
     * If no port is specified, the default port is used (@see NetworkHandler#DEFAULT_PORT)
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        Server server = null;
        try {
            int port;
            switch (args.length) {
                case 0 -> port = NetworkHandler.DEFAULT_PORT;
                case 1 -> port = Integer.parseInt(args[0]);
                default -> {
                    System.out.println("Illegal number of arguments:  [<ServerPort>]");
                    return;
                }
            }
            server = new Server(port);
            server.start();
        } catch (IOException e) {
            System.err.println("Error while starting server. " + e.getMessage());
        } finally {
            if (server != null) {
                System.out.println("Shutting initiated...");
                server.terminate();
            }
            System.out.println("Shutdown complete.");
        }
    }

}
