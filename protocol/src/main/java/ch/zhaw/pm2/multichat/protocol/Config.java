package ch.zhaw.pm2.multichat.protocol;

/**
 * Contains configuration parameters and enums used in the chat protocol
 */
public class Config {

    /**
     * Enum defining the type of messages that can be sent
     */
	public enum MessageType {
		INFO, CONNECT, CONFIRM, DISCONNECT, MESSAGE, ERROR;
	}

    /**
     * Enum defining the different states of the connection handler
     */
	public enum State {
		NEW, CONFIRM_CONNECT, CONNECTED, CONFIRM_DISCONNECT, DISCONNECTED;
	}

    /**
     * Exception message for invalid message format
     */
    public static final String NOT_VALID_MESSAGE = "Not a valid message format.";

    /**
     * Exception message for missing connection handler
     */
    public static final String NO_CONNECTION_HANDLER = "No connection handler";

    /**
     * Exception message for closed connection
     */
    public static final String CONNECTION_CLOSED = "Connection to the server has been closed";

    /**
     * Exception message for attempting to send message to oneself
     */
    public static final String MESSAGE_TO_YOURSELF = "Can't send messages to yourself";

    /**
     * Exception message for unexpected message type
     */
    public static final String UNEXPECTED_MESSAGE = "Unexpected message type: ";

    /**
     * Format string for informational messages
     */
    public static final String INFO_FORMAT = "[INFO] %s\n";

    /**
     * Format string for message sent by a user to another user
     */
    public static final String Message_FORMAT = "[%s -> %s] %s\n";

    /**
     * Format string for error messages
     */
    public static final String ERROR_FORMAT = "[ERROR] %s\n";

    /**
     * Success message for user registration
     */
    public static final String REGISTRATION_SUCCESSFUL = "Registration successful for ";

    /**
     * Constant for no user
     */
	public static final String USER_NONE = "";

    /**
     * Constant for all users
     */
	public static final String USER_ALL = "*";

    /**
     * Exception message for empty messages
     */
    public static final String EMPTY_MESSAGE = "Message is empty.";
}
