package ch.zhaw.pm2.multichat.protocol;

/**
 * This Exception is thrown when the chat protocol is violated
 */
public class ChatProtocolException extends Exception {

    /**
     * Constructs a new ChatProtocolException with the specified detail message.
     * @param message The detail message.
     */
    public ChatProtocolException(String message) {
        super(message);
    }
}
