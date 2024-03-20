package ch.zhaw.pm2.multichat.client;

import ch.zhaw.pm2.multichat.protocol.ChatProtocolException;
import ch.zhaw.pm2.multichat.protocol.Config;
import ch.zhaw.pm2.multichat.protocol.Message;
import ch.zhaw.pm2.multichat.protocol.NetworkHandler;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * This controller class handles events which occur in the chat window
 */

public class ChatWindowController {
    private final Pattern messagePattern = Pattern.compile( "^(?:@(\\w+(?:-\\d+)?))?\\s*(.*)$" );
    private ClientConnectionHandler connectionHandler;
    private ClientMessageList messages;

    private final WindowCloseHandler windowCloseHandler = new WindowCloseHandler();

    @FXML private Pane rootPane;
    @FXML private TextField serverAddressField;
    @FXML private TextField serverPortField;
    @FXML private TextField userNameField;
    @FXML private TextField messageField;
    @FXML private TextArea messageArea;
    @FXML private Button connectButton;
    @FXML private Button sendButton;
    @FXML private TextField filterValue;


    /**
     * initialize method is called automatically and should only be called when creating a new instance of the controller class
     */
    @FXML
    public void initialize() {
        serverAddressField.setText(NetworkHandler.DEFAULT_ADDRESS.getCanonicalHostName());
        serverPortField.setText(String.valueOf(NetworkHandler.DEFAULT_PORT));
        stateChanged(Config.State.NEW);
        messages = new ClientMessageList();
        initializeMessageList();
        sendButton.disableProperty().bind(messageField.textProperty().isEmpty());
    }

    private void initializeMessageList(){
        messages.getChatHistory().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                messageArea.setText(newValue);
            }
        });
    }

    private void initializeConnectionHandler(){
        connectionHandler.getPropertyState().addListener(new ChangeListener<Config.State>() {
            @Override
            public void changed(ObservableValue<? extends Config.State> observable, Config.State oldValue, Config.State newValue) {
                stateChanged(newValue);
            }
        });

        connectionHandler.getPropertyUserName().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                setUserName(newValue);
            }
        });

        connectionHandler.getPropertyConnection().addListener(new ChangeListener<NetworkHandler.NetworkConnection<String>>() {
            @Override
            public void changed(ObservableValue<? extends NetworkHandler.NetworkConnection<String>> observable, NetworkHandler.NetworkConnection<String> oldValue, NetworkHandler.NetworkConnection<String> newValue) {
                setServerPort(newValue.getRemotePort());
                setServerAddress(newValue.getRemoteHost());
            }
        });
    }

    private void applicationClose() {
        connectionHandler.setState(Config.State.DISCONNECTED);
    }

    @FXML
    private void toggleConnection () {
        if (connectionHandler == null || connectionHandler.getState() != Config.State.CONNECTED) {
            messages.emptyChatHistory();
            connect();
        } else {
            disconnect();
        }
    }

    private void connect() {
        try {
            startConnectionHandler();
            connectionHandler.connect();
        } catch(ChatProtocolException | IOException e) {
            connectionHandler.addError(e.getMessage());
        }
    }

    private void disconnect() {
        if (connectionHandler == null) {
            messages.addMessage(Config.MessageType.ERROR, null, null,Config.NO_CONNECTION_HANDLER);
            return;
        }
        try {
            connectionHandler.disconnect();
        } catch (ChatProtocolException e) {
            connectionHandler.addError(e.getMessage());
        }
    }
    @FXML
    private void message() {
        if (connectionHandler == null) {
            messages.addMessage(Config.MessageType.ERROR, null, null, Config.NO_CONNECTION_HANDLER);
            return;
        }
        String messageString = messageField.getText().strip();
        Matcher matcher = messagePattern.matcher(messageString);
        if (matcher.find()) {
            String receiver = matcher.group(1);
            String message = matcher.group(2);
            if(!message.isEmpty()){
                if (receiver == null || receiver.isBlank()) receiver = Config.USER_ALL;
                try {
                    connectionHandler.message(receiver, message);
                    messageField.clear();
                } catch (ChatProtocolException e) {
                    connectionHandler.addError(e.getMessage());
                }
            } else{
                connectionHandler.addError(Config.EMPTY_MESSAGE);
            }
        } else if(matcher.find()) {
            connectionHandler.addError(Config.NOT_VALID_MESSAGE);
        } else {
        	connectionHandler.addError(Config.EMPTY_MESSAGE);
        }
    }

    @FXML
    private void applyFilter( ) {
        this.redrawMessageList();
    }

    private void startConnectionHandler() throws IOException {
        String userName = userNameField.getText();
        String serverAddress = serverAddressField.getText();
        int serverPort = Integer.parseInt(serverPortField.getText());
        connectionHandler = new ClientConnectionHandler(
            NetworkHandler.openConnection(serverAddress, serverPort), userName, messages);
        initializeConnectionHandler();
        new Thread(connectionHandler).start();

        rootPane.getScene().getWindow().addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseHandler);
    }

    private void terminateConnectionHandler() {
        rootPane.getScene().getWindow().removeEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, windowCloseHandler);
        if (connectionHandler != null) {
            connectionHandler.terminate();
            connectionHandler = null;
        }
    }


    /**
     * This method allows the setting of the connection button text by passing a state argument
     * @param newState
     */
    private void stateChanged(Config.State newState) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                connectButton.setText((newState == Config.State.CONNECTED ||
                    newState == Config.State.CONFIRM_DISCONNECT)
                    ? "Disconnect" : "Connect");
                if (newState == Config.State.DISCONNECTED) {
                	connectionHandler.handleError(new Message("", "", Config.MessageType.ERROR,
                        Config.CONNECTION_CLOSED));
                }
            }
        });
        if (newState == Config.State.DISCONNECTED) {
            terminateConnectionHandler();
        }
        updateState(newState);

    }

    private void updateState(Config.State newState){
        switch (newState) {
            case NEW, DISCONNECTED -> {
                serverAddressField.setEditable(true);
                serverPortField.setEditable(true);
                userNameField.setEditable(true);
                messageField.setEditable(false);
                filterValue.setEditable(false);
            }
            case CONNECTED -> {
                serverAddressField.setEditable(false);
                serverPortField.setEditable(false);
                userNameField.setEditable(false);
                messageField.setEditable(true);
                filterValue.setEditable(true);
            }
        }
    }

    /**
     * This method allows the setting of a username to the username field by passing a String argument
     * @param userName
     */
    private void setUserName(String userName) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                userNameField.setText(userName);
            }
        });
    }

    /**
     * This method allows the setting of a server address to the server address field by passing a String argument
     * @param serverAddress
     */
    private void setServerAddress(String serverAddress) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverAddressField.setText(serverAddress);
            }
        });
    }

    /**
     * This method allows the setting of a server port to the server port field by passing an int argument
     * @param serverPort
     */
    private void setServerPort(int serverPort) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                serverPortField.setText(Integer.toString(serverPort));
            }
        });
    }


    private void redrawMessageList() {
        Platform.runLater(() -> messages.setFilter(filterValue.getText().strip()));
    }

    class WindowCloseHandler implements EventHandler<WindowEvent> {
        public void handle(WindowEvent event) {
            applicationClose();
        }

    }
}
