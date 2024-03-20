package ch.zhaw.pm2.multichat.server;

import ch.zhaw.pm2.multichat.protocol.*;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * This class is responsible for creating and handling connections between server and client
 * It extends the ConnectionHandler class
 */
public class ServerConnectionHandler extends ConnectionHandler {

    /** The current state of this connection */
    private Config.State state = Config.State.NEW;

	/**
	 * Global counter to generate connection IDs
	 */
	private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    /** The network connection to be used for receiving and sending requests */
    private final NetworkHandler.NetworkConnection<String> connection;

	/**
	 * The ID of this connection
	 */
	private final int connectionId = connectionCounter.incrementAndGet();

	/**
	 * Reference to the registry managing all connections
	 */
	private final Map<String, ServerConnectionHandler> connectionRegistry;


	/**
	 * The username associated with this connection
	 * Using Anonymous-{@link #connectionId} if not specified by the client
	 */
	private String userName = "Anonymous-" + connectionId;


	/**
	 * Creates a new `ServerConnectionHandler` object with the given network connection and connection registry.
	 * @param connection The network connection to handle.
	 * @param registry The registry of all connections.
	 */
	public ServerConnectionHandler(NetworkHandler.NetworkConnection<String> connection,
								   Map<String, ServerConnectionHandler> registry) {
		Objects.requireNonNull(connection, "Connection must not be null");
		Objects.requireNonNull(registry, "Registry must not be null");
        this.connection = connection;
		this.connectionRegistry = registry;
	}


	public String getUserName() {
		return this.userName;
	}

	/**
	 * Start receiving packages from the network connection.
	 * It continuously receives packages from the network connection and processes it depending on the package type
	 */
	@Override
	public void startReceiving() {
		System.out.println("Starting Connection Handler for " + userName);
		try {
			System.out.println("Start receiving data...");
			while (connection.isAvailable()) {
				String data = connection.receive();
				processData(data);
			}
		} catch (SocketException e) {
			System.out.println("Connection terminated locally");
			connectionRegistry.remove(userName);
			System.out.println("Unregistered because connection terminated: " + userName + " " + e.getMessage());
		} catch (EOFException e) {
			System.out.println("Connection terminated by remote peer");
			connectionRegistry.remove(userName);
			System.out.println("Unregistered because connection terminated: " + userName + " " + e.getMessage());
		} catch (IOException e) {
			System.err.println("Communication error: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println("Received object of unknown type: " + e.getMessage());
		}
		System.out.println("Ended Connection Handler for " + userName);
	}



	/**
	 * Processes the received data depending on the package type.
	 * @param data The received data.
	 */
	@Override
	protected void processData(String data) {
		try {
			Message message = parseData(data);
			switch (message.getType()) {
				case CONNECT -> handleConnectionRequest(message);
				case DISCONNECT -> handleDisconnectionRequest(message);
                case CONFIRM -> System.out.println("Not expecting to receive a CONFIRM request from client");
				case MESSAGE -> handleMessage(message);
				case ERROR -> handleError(message);
				default -> System.out.println("Unknown data type received: " + message.getType());
			}
		} catch (ChatProtocolException error) {
			System.err.println("Error while processing data: " + error.getMessage());
			sendData(new Message(Config.USER_NONE, userName, Config.MessageType.ERROR, error.getMessage()));
		}
	}


    /**
     * This method sends a message object over the stored connection
     * @param message
     */
    @Override
    protected void sendData(Message message) {
        if (connection.isAvailable()) {
            try {
                connection.send(message.toString());
            } catch (SocketException e) {
                System.err.println("Connection closed: " + e.getMessage());
            } catch (EOFException e) {
                System.err.println("Connection terminated by remote peer");
            } catch (IOException e) {
                System.err.println("Communication error: " + e.getMessage());
            }
        }
    }

	/**
	 * Handles an error message.
	 * @param message the error message
	 */
	@Override
	protected void handleError(Message message) {
		System.out.println("Received error from client (" + message.getSender() + "): " + message.getPayload());
	}

	/**
	 * Sends a message to the appropriate client(s).
	 * @param message the message to send
	 * @throws ChatProtocolException if there was an error processing the message
	 */
	@Override
	protected void handleMessage(Message message) throws ChatProtocolException {
		if (state != Config.State.CONNECTED) throw new ChatProtocolException("Illegal state for message request: " + state);
		if (Config.USER_ALL.equals(message.getReceiver())) {
			for (ServerConnectionHandler handler : connectionRegistry.values()) {
				handler.sendData(message);
			}
		} else {
			ServerConnectionHandler handler = connectionRegistry.get(message.getReceiver());
			if (handler != null) {
				handler.sendData(message);
                this.sendData(message);
			} else {
				this.sendData(new Message(Config.USER_NONE, userName, Config.MessageType.ERROR, "Unknown User: " + message.getReceiver()));
			}
		}
	}

    /**
	 * Handles a disconnection request from a client.
	 * @param message the disconnection message
	 * @throws ChatProtocolException if there was an error processing the message
	 */
	@Override
	protected void handleDisconnectionRequest(Message message) throws ChatProtocolException {
		if (state == Config.State.DISCONNECTED) {
			throw new ChatProtocolException("Illegal state for disconnect request: " + state);
		}
		if(state == Config.State.CONNECTED) {
			connectionRegistry.remove(userName);
		}
		state = Config.State.DISCONNECTED;
		sendData(new Message(userName, Config.USER_NONE, Config.MessageType.DISCONNECT, "User " + userName + " has successfully disconnected"));
		stopReceiving();
	}

	/**
	 * Handles a connection request from a client.
	 * @param message the connection message
	 * @throws ChatProtocolException if there was an error processing the message
	 */
	private void handleConnectionRequest(Message message) throws ChatProtocolException {
		if (this.state != Config.State.NEW) throw new ChatProtocolException("Illegal state for connect request: " + state);
		if (message.getSender() == null || message.getSender().isBlank()) message.setSender(userName);
		if (connectionRegistry.containsKey(message.getSender()))
			throw new ChatProtocolException("User name already taken: " + message.getSender());
		userName = message.getSender();
		connectionRegistry.put(userName, this);
		sendData(new Message(Config.USER_NONE, userName, Config.MessageType.CONFIRM, Config.REGISTRATION_SUCCESSFUL + userName));
		state = Config.State.CONNECTED;
	}


	/**
	 * Stops receiving packages from the network connection.
	 */
	@Override
    protected void stopReceiving() {
        System.out.println("Closing Connection Handler for " + userName + "...");
        try {
            System.out.println("Stop receiving data...");
            connection.close();
            System.out.println("Stopped receiving data.");
        } catch (IOException e) {
            System.err.println("Failed to close connection." + e.getMessage());
        }
        System.out.println("Closed Connection Handler for " + userName);
    }
}
