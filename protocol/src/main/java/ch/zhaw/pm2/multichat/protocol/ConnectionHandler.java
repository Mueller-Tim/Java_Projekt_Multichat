package ch.zhaw.pm2.multichat.protocol;

import java.util.Scanner;

/**
 * The ConnectionHandler receives and sends messages over a NetworkConnection object,
 * parses the data received, and handles different types of messages.
 */
public abstract class ConnectionHandler {

	/**
	 * Parses a string of data received from the network into a Message object.
	 *
	 * @param data the data string to be parsed
	 * @return the parsed Message object
	 * @throws ChatProtocolException if the data string is invalid or incomplete
	 */
	protected Message parseData(String data) throws ChatProtocolException {
		Message message = new Message();
		Scanner scanner = new Scanner(data);
		if (scanner.hasNextLine()) {
			message.setSender(scanner.nextLine());
		} else {
			throw new ChatProtocolException("No Sender found");
		}
		if (scanner.hasNextLine()) {
			message.setReceiver(scanner.nextLine());
		} else {
			throw new ChatProtocolException("No Reciever found");
		}
		if (scanner.hasNextLine()) {
			message.setType(mapType(scanner.nextLine()));
		} else {
			throw new ChatProtocolException("No Type found");
		}
		if (scanner.hasNextLine()) {
			message.setPayload(scanner.nextLine());
		}
		return message;
	}

	/**
	 * Maps a string type to a MessageType enum value.
	 *
	 * @param type the string type to be mapped
	 * @return the mapped MessageType enum value
	 * @throws ChatProtocolException if the type string is invalid
	 */
	private Config.MessageType mapType(String type) throws ChatProtocolException {
		return switch (type) {
			case "INFO" -> Config.MessageType.INFO;
			case "CONNECT" -> Config.MessageType.CONNECT;
			case "DISCONNECT" -> Config.MessageType.DISCONNECT;
			case "CONFIRM" -> Config.MessageType.CONFIRM;
			case "ERROR" -> Config.MessageType.ERROR;
			case "MESSAGE" -> Config.MessageType.MESSAGE;
			default -> throw new ChatProtocolException("Invalid type: " + type);
		};
	}

	/**
	 * This method starts receiving data from the network connection.
	 * It should be implemented by concrete subclasses to define the desired behavior.
	 */
	protected abstract void startReceiving();

    /**
     * Stop receiving packages from the network connection, by closing the connection.
     */
    protected abstract void stopReceiving();

	/**
	 * This method processes incoming data received from the network connection.
	 * It should be implemented by concrete subclasses to define the desired behavior.
	 * @param data the raw data received from the network connection
	 */
	protected abstract void processData(String data);

    /**
     * This method sends a message object over the stored connection
     * @param message
     */
    protected abstract void sendData(Message message);

	/**
	 * This method handles disconnection requests received from the network connection.
	 * It should be implemented by concrete subclasses to define the desired behavior.
	 * @param message the message containing the disconnection request
	 * @throws ChatProtocolException if there is an error processing the message
	 */
	protected abstract void handleDisconnectionRequest(Message message) throws ChatProtocolException;

	/**
	 * This method handles error messages received from the network connection.
	 * It should be implemented by concrete subclasses to define the desired behavior.
	 * @param message the message containing the error message
	 */
	protected abstract void handleError(Message message);

	/**
	 * This method handles regular chat messages received from the network connection.
	 * It should be implemented by concrete subclasses to define the desired behavior.
	 * @param message the message containing the chat message
	 * @throws ChatProtocolException if there is an error processing the message
	 */
	protected abstract void handleMessage(Message message) throws ChatProtocolException;
}
