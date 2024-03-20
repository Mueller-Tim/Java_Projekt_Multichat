package ch.zhaw.pm2.multichat.client;

import javafx.application.Application;

/**
 * Entry point for starting a new client instance
 */
public class Client {

    /**
     * The Client class is the entry point for starting a new client instance of the multi-user chat application.
     * It launches a new instance of the JavaFX Application using the ClientUI class as the main application
     * class and passes any command-line arguments to it.
     */
    public static void main(String[] args) {
        System.out.println("Starting Client Application");
        Application.launch(ClientUI.class, args);
        System.out.println("Client Application ended");
    }
}

