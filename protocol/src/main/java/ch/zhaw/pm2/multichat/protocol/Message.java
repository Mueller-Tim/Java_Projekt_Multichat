package ch.zhaw.pm2.multichat.protocol;

import java.io.Serializable;

/**
 * The Message class represents a message sent between clients and servers in the multi-user chat
 * application. It encapsulates the sender, receiver, type, and payload of the message.
 */
public class Message implements Serializable {
	private String sender;
	private String receiver;
	private Config.MessageType type;
	private String payload;


    /**
     * Constructs a new Message with the specified sender, receiver, type, and payload.
     * @param sender The sender of the message.
     * @param receiver The receiver of the message.
     * @param type The type of the message.
     * @param payload The payload of the message.
     */
	public Message(String sender, String receiver, Config.MessageType type, String payload) {
		this.sender = sender;
		this.receiver = receiver;
		this.type = type;
		this.payload = payload;
	}

    /**
     * Constructs a new Message with default values for all fields.
     */
	public Message() {
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public Config.MessageType getType() {
		return type;
	}

	public void setType(Config.MessageType type) {
		this.type = type;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

    /**
     * Returns a string representation of the message, including its sender, receiver, type, and payload.
     * @return A string representation of the message.
     */
	@Override
	public String toString() {
		return sender + "\n" + receiver + "\n" + type + "\n" + payload;
	}
}
