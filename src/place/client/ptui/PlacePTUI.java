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

    private PlaceBoardObservable board;

    private NetworkClient serverConn;

    private boolean go = false;

    /**
     * Method used to get the named parameters from the command line arguments
     * @param name The string name of the parameter you need to get
     * @return the parameter needed from the List
     * @throws PlaceException k
     */
    private String getParamNamed(String name) throws PlaceException {
        // gets parameters from ConsoleApplication
        if(parameters == null) { parameters = super.getArguments(); }
        // checks for argument, throws an error if it is missing
        if(!parameters.contains(name)) { throw new PlaceException("Can't find parameter named " + name); }
        // otherwise get named parameter
        else { return parameters.get(parameters.indexOf(name)); }
    }

    /**
     * Initializes client before doing anything with model of our board
     */
    @Override
    public void init() throws Exception {
        super.init();

        // Get named command line arguments to be used
        username = getParamNamed("user");
        String hostname = getParamNamed("host");
        int port = Integer.parseInt(getParamNamed("port"));

        System.out.println("Creating board");
        // Creates blank model for board
        board = new PlaceBoardObservable();

        try {
            System.out.println("connection");
            // Connects with the NetworkClient to communicate with PlaceServer
            serverConn = new NetworkClient(hostname, port, username, board);
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
        board.addObserver(this);

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
        return "to string called...";
    }
}
