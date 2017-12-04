package place.client.ptui;

import place.PlaceBoardObservable;
import place.PlaceException;
import place.network.NetworkClient;

import java.io.*;
import java.util.*;

/**
    Class that contains the plain text client that will connect to place server
    @ Jason Streeter
    @ Kevin
 */

public class PlacePTUI extends ConsoleApplication implements Observer
{
    private List<String> parameters;

    private String username;

    private PlaceBoardObservable model;

    private NetworkClient serverConn;

    private boolean go = false;

    /**
     * Initializes client before doing anything with model of our board
     */
    @Override
    public void init() throws Exception {
        super.init();

        this.parameters = super.getArguments();
        // Get named command line arguments to be used
        String hostname = this.parameters.get(0);
        int port = Integer.parseInt(this.parameters.get(1));
        this.username = this.parameters.get(2);

        // Creates blank model for board
        this.model = new PlaceBoardObservable();

        try
        {
            // Connects with the NetworkClient to communicate with PlaceServer
            serverConn = new NetworkClient(hostname, port, this.username, model);
        }
        catch(Exception e) {
            System.err.println("Error connecting with Place server...");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }



    /**
     * Constructs the board we will use and then prints it out
     * @param in Scanner that reads the user's input
     * @param out Communication with PlaceServer
     */
    @Override
    public synchronized void go(Scanner in, PrintWriter out) throws Exception {
        // make PTUI an observer of the board
        this.model.addObserver(this);

        // begins thread for NetworkClient to listen to server
        serverConn.start();

        // Display current board now it has been built
        System.out.println(toString());
    }

    @Override
    public void stop() {
        super.stop();
        serverConn.close();
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

    @Override
    public String toString() {
        return this.model.getBoard().toString();
    }
}
