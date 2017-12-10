package place.client.ptui;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.NetworkClient;

import java.util.Observer;
import java.util.Observable;
import java.util.List;
import java.util.Scanner;

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

    /**
     * The prompt that is printed to the window when it's time for a user to enter their tile choice.
     */
    private final static String PROMPT = "Enter a tile to color (row col color): ";

    /**
     * The username of the user.
     */
    private String username;

    /**
     * The Observable model that is used to keep our board up to date.
     */
    private PlaceBoardObservable model;

    /**
     * The connection to the Place server.
     */
    private NetworkClient serverConn;

    /**
     * The boolean that maintains if the application should keep running.
     */
    private boolean go;

    /**
     * A simple synchronized go method to get the status of the program.
     *
     * @return the status of this.go, true if application should keep going, false otherwise.
     */
    private synchronized boolean go()
    {
        return this.go;
    }

    /**
     * Initializes client before doing anything with model of our board.
     *
     * @throws Exception if any sort of issue is encountered with setting up our server connection.
     */
    @Override
    public void init() throws Exception {
        // calls the superclass init method just in case there's something there.
        super.init();

        // gets our parameters
        List<String> parameters = super.getArguments();

        // sets our needed parameters
        String hostname = parameters.get(0);
        int port = Integer.parseInt(parameters.get(1));
        this.username = parameters.get(2);

        // Creates blank model for board (updated in serverConn)
        this.model = new PlaceBoardObservable();

        try
        {
            // Connects with the NetworkClient to communicate with PlaceServer
            this.serverConn = new NetworkClient(hostname, port, this.username, getClass().getSimpleName(), this.model);
        }
        catch(PlaceException e)
        {
            // closes serverConn
            this.serverConn.close();
            // tells the user about the issue we've run into
            throw e;
        }

        // if we can get to this point we have successfully connected to the Place server

        // begins thread for NetworkClient to listen to server
        this.serverConn.start();

        // make PTUI an observer of the board
        this.model.addObserver(this);

        // sets go to true because it's go time
        this.go = true;
    }

    /**
     * Method that runs the program listening for user inputs.
     *
     * @param in The scanner used for user input of moves.
     */
    @Override
    public void start( Scanner in )
    {
        // prints the board
        printBoard();
        // prompts the user to input
        this.serverConn.log(PROMPT);

        // while we are still good to go on both server communications and client side
        while(this.go() && this.serverConn.go()) {
            // get the next move of the player and split it up
            String [] playerInput = in.nextLine().trim().split(" ");

            // Check if user exits program
            if(playerInput[0].equals("-1")) {
                this.serverConn.log("Exit command has been read. Exiting PTUI.");
                this.go = false;
            }
            // Check if user enters too much/little
            else if(playerInput.length != 3) {
                this.serverConn.logErr("Please enter a valid command.");
            }
            else {
                try {
                    // Otherwise check for player input of where to place move
                    int row = Integer.parseInt(playerInput[0]);
                    int col = Integer.parseInt(playerInput[1]);
                    int color = Integer.parseInt(playerInput[2]);

                    // Checks if color user selected is valid
                    if (color <= 15 && color >= 0) {
                        // creates a new tile that will be sent to the server
                        PlaceTile newTile = new PlaceTile(row, col, this.username, PlaceColor.values()[color], System.currentTimeMillis());

                        // sends tile placement to server
                        this.serverConn.sendTile(newTile);
                    } else {
                        this.serverConn.logErr("Please enter a color value between 0-15.");
                    }
                }
                // if something user enter isn't a number
                catch (NumberFormatException e) { this.serverConn.logErr("Please only enter numbers"); }
            }
        }
    }

    /**
     * Closes connection with Place server
     */
    @Override
    public void stop() {
        // calls super stop method
        super.stop();
        // closes our serverConn
        this.serverConn.close();
    }

    /**
     * Update the state of our model with user input from the console.
     */
    private void refreshBoard() {
        // reprints the board
        printBoard();
        // prompts the user again
        this.serverConn.log(PROMPT);
    }

    /**
     * Used to update our model.
     *
     * @param o Our model of the board we are observing.
     * @param arg An object -- Not used.
     */
    @Override
    public void update(Observable o, Object arg)
    {
        // checks to make sure we are called from the correct model
        assert o == this.model: "Update message came from non-board";

        // if we're good to go, we refresh
        refreshBoard();
    }

    /**
     * Prints the state of our current board
     */
    private void printBoard() {
        System.out.println(this.model.getBoard());
    }

    /**
     * The main method of our program. It is run on the command line using
     * @param args
     */
    public static void main(String[] args) {
        // Checks for proper command line arguments
        if(args.length != 3) {
            // makes sure we were given the proper number of arguments, if not, tell the user the proper way to start it
            System.err.println("Please run the PTUI as:");
            System.err.println("$ java PlacePTUI host port username");
            return;
        }

        // if we get here it's go time
        ConsoleApplication.launch(PlacePTUI.class, args);
    }
}
