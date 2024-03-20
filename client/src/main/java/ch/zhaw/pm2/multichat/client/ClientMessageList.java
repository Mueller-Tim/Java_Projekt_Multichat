package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.Config;
import ch.zhaw.pm2.multichat.protocol.Message;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This class is responsible for storing the relevant messages for this client instance
 */
public class ClientMessageList {
	private final List<Message> messages = new ArrayList<>();
	private final StringProperty chatHistory = new SimpleStringProperty();
	private String filter = "";

    /**
     * Constructor that initializes the chat history.
     */
	public ClientMessageList() {
		chatHistory.setValue("");
	}

    /**
     * Adds a new message to the instance message list and applies the current filter to update the chat history.
     *
     * @param type     the type of the message (ERROR, INFO, or MESSAGE).
     * @param sender   the name of the sender.
     * @param receiver the name of the receiver.
     * @param message  the message content.
     */
	public void addMessage(Config.MessageType type, String sender, String receiver, String message) {
		Message newMessage = new Message(sender, receiver, type, message);
		messages.add(newMessage);
		writeFilteredMessages(filter);
	}

    /**
     * Clears all messages from the message list.
     */
    public void emptyChatHistory(){
        messages.clear();
    }

	public StringProperty getChatHistory(){
		return chatHistory;
	}


	/**
	 * This method applies a filter to the message list
	 * @param filter
	 */
	public void setFilter(String filter){
		this.filter = filter;
		writeFilteredMessages(filter);
	}


	private void writeFilteredMessages(String filter) {
		boolean showAll = filter == null || filter.isBlank();
		chatHistory.set("");
		for (Message message : messages) {
			if (showAll ||
					(Objects.nonNull(message.getSender()) && message.getSender().contains(filter)) ||
					(Objects.nonNull(message.getReceiver()) && message.getReceiver().contains(filter)) ||
					(Objects.nonNull(message.getPayload()) && message.getPayload().contains(filter))) {
				updateChatHistory(message);
			}
		}
	}

	private void updateChatHistory(Message message){
		String oldChatHistory = chatHistory.get();
		switch (message.getType()) {
			case MESSAGE -> chatHistory.set(oldChatHistory + appendMessage(message.getSender(),
					message.getReceiver(), message.getPayload()));
			case ERROR -> chatHistory.set(oldChatHistory + appendError(message.getPayload()));
			case INFO -> chatHistory.set(oldChatHistory + appendInfo(message.getPayload()));
			default -> chatHistory.set(oldChatHistory + appendError(Config.UNEXPECTED_MESSAGE + message.getType()));
		}
	}

	private String appendMessage(String sender, String receiver, String message){
		return String.format(Config.Message_FORMAT, sender, receiver, message);
	}

	private String appendInfo(String message) {
		return String.format(Config.INFO_FORMAT, message);
	}

	private String appendError(String message) {
		return String.format(Config.ERROR_FORMAT, message);
	}
}
