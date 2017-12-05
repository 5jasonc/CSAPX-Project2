package place.client.ptui;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.NetworkClient;

import java.io.*;
import java.util.*;

/**
 * A PTUI client which connects to a PlaceServer.
 *
 * Run on the command line using the following:
 *     <pre>$ java PlacePTUI host port username</pre>
 * to properly log in.
 *
 * @author Jason Streeter (jcs1738)
 * @author Kevin Becker (kjb2503)
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
        // calls the superclass' init method just in case there's something there.
        super.init();

        this.parameters = super.getArguments();
        // Get command line arguments to be used
        String hostname = this.parameters.get(0);
        int port = Integer.parseInt(this.parameters.get(1));
        this.username = this.parameters.get(2);

        // Creates blank model for board
        this.model = new PlaceBoardObservable();

        try
        {
            // Connects with the NetworkClient to communicate with PlaceServer
            this.serverConn = new NetworkClient(hostname, port, this.username, getClass().getSimpleName(), this.model);
        }
        catch(Exception e)
        {
            System.err.println("Error connecting with Place server...");
            this.serverConn.close();
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Constructs the board we will use and then prints it out
     */
    @Override
    public synchronized void go( Scanner in ) throws Exception {
        // begins thread for NetworkClient to listen to server
        this.serverConn.start();

        // make PTUI an observer of the board
        this.model.addObserver(this);

        // Display current board now it has been built
        printBoard();

        this.go = true;

        // Creates new thread to run user operations
        new Thread( () -> this.run( in )).start();
    }

    /**
     * Thread that gets run to check for user input
     */
    public void run( Scanner in ) {
        while(this.go) {
            String [] playerInput = in.nextLine().trim().split(" ");
            PlaceTile newTile;

            // Check if user exits program
            if(playerInput[0].equals("-1")) {
                this.go = false;
            }
            else {
                // Otherwise check for player input of where to place move
                int placeRow = Integer.parseInt(playerInput[0]);

                int placeCol = Integer.parseInt(playerInput[1]);
                int color = Integer.parseInt(playerInput[2]);

                // Checks if color user selected is valid
                if(color <= 15 && color >= 0) {
                    // Get color user wants to place
                    PlaceColor newColor = PlaceColor.values()[color];

                    // Sends user's move to the server
                    newTile = new PlaceTile(placeRow, placeCol, this.username, newColor, System.currentTimeMillis());

                    // Checks to see if a valid tile was selected and sends it to server if it is
                    if(this.model.getBoard().isValid(newTile)) {
                        this.serverConn.sendTile(newTile);
                    }
                    else {
                        this.serverConn.logErr("Please select a valid tile on the board.");
                    }
                }
                else {
                    this.serverConn.logErr("Please select a color value between 0-15.");
                }
            }
        }
    }

    /**
     * Closes connection with Place server
     */
    @Override
    public void stop() {
        super.stop();
        this.serverConn.close();
    }

    /**
     * Update the state of our model with user input from the console.
     */
    private void refresh() {
        printBoard();
    }

    /**
     * Used to update our model
     * @param o Our model of the board we are observing
     * @param arg An object -- Not used
     */
    @Override
    public void update(Observable o, Object arg)
    {
        assert o == this.model: "Update from an incorrect model of the board...";

        this.refresh();
    }

    /**
     * Prints the state of our current board
     */
    private void printBoard() {
        System.out.println(this.model.getBoard());
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
