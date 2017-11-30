package place.client.ptui;

import javafx.application.Platform;
import place.PlaceBoard;
import place.network.NetworkServer;
import place.network.PlaceRequest;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

/**
    Class that contains the plain text client that will connect to place server
    @ Jason Streeter
 */

public class PlacePTUI extends ConsoleApplication implements Observer
{
    private String username;

    private PlaceBoard board;

    private NetworkServer server;

    private Socket serverConn;

    private ObjectInputStream in;

    private ObjectOutputStream out;

    private boolean go = false;

    /**
     * Initializes client before creating GUI
     */
    @Override
    public void init() {
        // Takes command line arguments and creates client socket and necessary client information
        List<String> args = super.getArguments();
        String hostname = args.get(0);
        int port = Integer.parseInt(args.get(1));
        username = args.get(2);

        try {
            // Connects to server
            serverConn = new Socket(hostname, port);

            // Initializes input and output with Place server
            in = new ObjectInputStream(serverConn.getInputStream());
            out = new ObjectOutputStream(serverConn.getOutputStream());

            // send out our login place request immediately
            out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, this.username));

            try {
                // read in our first object (should be PlaceRequest<String> with username or ERROR)
                PlaceRequest<?> request = (PlaceRequest<?>) in.readObject();

                switch (request.getType()) {
                    case LOGIN_SUCCESS:
                        System.out.println("Successfully joined Place server as \"" + request.getData() + "\".");
                        this.go = true;
                        break;
                    case ERROR:
                        System.out.println("Failed to join Place server. Reason: " + request.getData() + ". Terminating.");
                        Platform.exit();
                        break;
                    default:
                        System.out.println("Unknown response received. Terminating.");
                        Platform.exit();
                        // unknown response, terminate
                }

                if (this.go) {
                    // read in our next object
                    request = (PlaceRequest<?>) in.readObject();

                    // read in our board checking to make sure it really is a board
                    if (request.getType() == PlaceRequest.RequestType.BOARD)
                        this.board = (PlaceBoard) request.getData();
                }
            }
            catch(ClassNotFoundException e) {
                // ?????? needed for in.readObject()
                System.err.println("Class not found...");
                System.exit(1);
            }
        }
        catch(IOException e) {
            System.err.println("Error connecting with Place server...");
            System.exit(1);
        }
    }

    @Override
    public synchronized void go(Scanner in, PrintWriter out) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void update(Observable o, Object arg) {

    }


    public static void main(String[] args) {
        // Checks for proper command line arguments
        if(args.length != 3) {
            System.err.println("Usage: $ java PlacePTUI host port username");
            System.exit(1);
        }

        ConsoleApplication.launch(PlacePTUI.class, args);
    }
}
