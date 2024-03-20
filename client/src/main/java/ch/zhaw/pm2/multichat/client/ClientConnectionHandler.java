package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

/**
 * This class is responsible for creating and handling connections between
 * client and server It extends the ConnectionHandler class and implements the
 * Runnable interface.
 */
public class ClientConnectionHandler extends ConnectionHandler implements Runnable {

    private final ClientMessageList messageList;
	private final SimpleObjectProperty<Config.State> propertyState = new SimpleObjectProperty<>(Config.State.NEW);
	private final StringProperty userName = new SimpleStringProperty(Config.USER_NONE);
	private final SimpleObjectProperty<NetworkHandler.NetworkConnection<String>> connection = new SimpleObjectProperty<>();

	/**
	 * Constructs a new ClientConnectionHandler object.
	 *
	 * @param connection the network connection
	 * @param userName   the username associated with this connection
	 * @param messageList the message list
	 */
	public ClientConnectionHandler(NetworkHandler.NetworkConnection<String> connection, String userName, ClientMessageList messageList) {
		this.connection.setValue(connection);
		setUserName((userName == null || userName.isBlank()) ? Config.USER_NONE : userName);
        this.messageList = messageList;
	}

	public Config.State getState() {
		return propertyState.getValue();
	}

	public void setState(Config.State newState) {
		propertyState.setValue(newState);
	}

	public SimpleObjectProperty<Config.State> getPropertyState() {
		return propertyState;
	}

	public String getUserName() {
		return userName.getValue();
	}

	private void setUserName(String userName) {
		this.userName.setValue(userName);
	}

	public StringProperty getPropertyUserName() {
		return userName;
	}

	private NetworkHandler.NetworkConnection<String> getConnection() {
		return connection.getValue();
	}

	public SimpleObjectProperty<NetworkHandler.NetworkConnection<String>> getPropertyConnection() {
		return connection;
	}

	/**
	 * Start the connection handler. It will start listening for incoming messages
	 * from the server and process them.
	 */
	@Override
	public void run() {
		System.out.println("Starting Connection Handler");
		startReceiving();
		System.out.println("Ended Connection Handler");
	}

	/**
	 * Terminate the Connection Handler by closing the connection to not receive any
	 * more messages.
	 */
	public void terminate() {
		System.out.println("Closing Connection Handler to Server");
		stopReceiving();
		System.out.println("Closed Connection Handler to Server");
	}

	/**
	 * Starts receiving data from the server.
	 */
	@Override
	public void startReceiving() {
		try {
			System.out.println("Start receiving data...");
			while (getConnection().isAvailable()) {
				String data = getConnection().receive();
				processData(data);
			}
		} catch (SocketException e) {
			System.out.println("Connection terminated locally");
			this.setState(Config.State.DISCONNECTED);
			System.out.println("Unregistered because connection terminated" + e.getMessage());
		} catch (EOFException e) {
			System.out.println("Connection terminated by remote peer");
			this.setState(Config.State.DISCONNECTED);
			System.out.println("Unregistered because connection terminated" + e.getMessage());
		} catch (IOException e) {
			System.err.println("Communication error: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			System.err.println("Received object of unknown type: " + e.getMessage());
		}
	}

	/**
	 * Stops receiving packages from the network connection.
	 */
	@Override
	protected void stopReceiving() {
		try {
			System.out.println("Stop receiving data...");
			getConnection().close();
			System.out.println("Stopped receiving data.");
		} catch (IOException e) {
			System.err.println("Failed to close connection." + e.getMessage());
		}
	}

	/**
	 * Processes the received data depending on the package type.
	 *
	 * @param data The received data.
	 */
	@Override
	protected void processData(String data) {
		try {
			Message message = parseData(data);
			switch (message.getType()) {
			case CONNECT -> System.err.println("Illegal connect request from server");
			case DISCONNECT -> handleDisconnectionRequest(message);
			case CONFIRM -> handleConfirmRequest(message);
			case MESSAGE -> handleMessage(message);
			case ERROR -> handleError(message);
			default -> System.out.println("Unknown data type received: " + message.getType());
			}
		} catch (ChatProtocolException error) {
			System.err.println("Error while processing data: " + error.getMessage());
			sendData(new Message(Config.USER_NONE, getUserName(), Config.MessageType.ERROR, error.getMessage()));
		}
	}

	/**
	 * This method sends a message object over the stored connection
	 *
	 * @param message
	 */
	@Override
	protected void sendData(Message message) {
		if (getConnection().isAvailable()) {
			try {
				getConnection().send(message.toString());
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
	 * Handles a disconnection request from a client.
	 *
	 * @param message the disconnection message
	 */
	@Override
	protected void handleDisconnectionRequest(Message message) {
		if (getState() == Config.State.DISCONNECTED) {
			System.out.println("DISCONNECT: Already in disconnected: " + message.getPayload());
			return;
		}
		addInfo(message.getPayload());
		System.out.println("DISCONNECT: " + message.getPayload());
		this.setState(Config.State.DISCONNECTED);
	}

	private void handleConfirmRequest(Message message) {
		switch (getState()) {
		case CONFIRM_CONNECT -> handleConfirmConnect(message);
		case CONFIRM_DISCONNECT -> handleConfirmDISCONNECT(message);
		default -> System.err.println("Got unexpected confirm message: " + message.getPayload());
		}
	}

	private void handleConfirmConnect(Message message) {
		setUserName(message.getReceiver());
		addInfo(message.getPayload());
		System.out.println("CONFIRM: " + message.getPayload());
		this.setState(Config.State.CONNECTED);
	}

	private void handleConfirmDISCONNECT(Message message) {
		addInfo(message.getPayload());
		System.out.println("CONFIRM: " + message.getPayload());
		this.setState(Config.State.DISCONNECTED);
	}

	/**
	 * Handles an error message.
	 *
	 * @param message the error message
	 */
	@Override
	protected void handleError(Message message) {
        addError(message.getPayload());
		System.out.println("ERROR: " + message.getPayload());
	}

	/**
	 * Sends a message to the appropriate client(s).
	 *
	 * @param message the message to send
	 */
	@Override
	protected void handleMessage(Message message) {
		if (getState() != Config.State.CONNECTED) {
			System.out.println("MESSAGE: Illegal state " + getState() + " for message: " + message.getPayload());
			return;
		}
		addMessage(message.getSender(), message.getReceiver(), message.getPayload());
		System.out.println(
				"MESSAGE: From " + message.getSender() + " to " + message.getReceiver() + ": " + message.getPayload());
	}

	/**
	 * This method tries to connect a new client to the stored server
	 *
	 * @throws ChatProtocolException
	 */
	public void connect() throws ChatProtocolException {
		if (getState() != Config.State.NEW) {
			throw new ChatProtocolException("Illegal state for connect: " + getState());
		}
		sendData(new Message(getUserName(), Config.USER_NONE, Config.MessageType.CONNECT, null));
		setState(Config.State.CONFIRM_CONNECT);
	}

	/**
	 * This method tries to disconnect a client to the stored server
	 *
	 * @throws ChatProtocolException
	 */
	public void disconnect() throws ChatProtocolException {
		if (getState() != Config.State.NEW && getState() != Config.State.CONNECTED) {
			throw new ChatProtocolException("Illegal state for disconnect: " + getState());
		}
		sendData(new Message(getUserName(), Config.USER_NONE, Config.MessageType.DISCONNECT, null));
		setState(Config.State.CONFIRM_DISCONNECT);
	}

	/**
	 * This method sends the provided message to the specified user
	 *
	 * @param receiver
	 * @param message
	 * @throws ChatProtocolException
	 */
	public void message(String receiver, String message) throws ChatProtocolException {
		if (getState() != Config.State.CONNECTED) {
			throw new ChatProtocolException("Illegal state for message: " + getState());
		}
		if (!userName.get().equals(receiver)) {
			sendData(new Message(getUserName(), receiver, Config.MessageType.MESSAGE, message));
		} else {
			addError(Config.MESSAGE_TO_YOURSELF);
		}
	}

    /**
     * This method adds the provided message to the client message list
     * @param sender, receiver, message
     */
    public void addMessage(String sender, String receiver, String message) {
        messageList.addMessage(Config.MessageType.MESSAGE, sender, receiver, message);
    }

    private void addInfo(String message) {
        messageList.addMessage(Config.MessageType.INFO, null, null, message);
    }

    /**
     * This method adds the provided message to the client message list
     * @param message
     */
    public void addError(String message) {
        messageList.addMessage(Config.MessageType.ERROR, null, null, message);
    }
}
