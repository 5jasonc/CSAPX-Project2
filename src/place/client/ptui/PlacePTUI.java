package place.client.ptui;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceTile;
import place.network.NetworkClient;

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

    private final static String PROMPT = "Enter a tile to color (row col color): ";

    private String username;

    private PlaceBoardObservable model;

    private NetworkClient serverConn;

    private boolean go;

    private boolean go()
    {
        return this.go;
    }

    /**
     * Initializes client before doing anything with model of our board
     */
    @Override
    public void init() throws Exception {
        // calls the superclass' init method just in case there's something there.
        super.init();

        List<String> parameters = super.getArguments();
        // Get command line arguments to be used
        String hostname = parameters.get(0);
        int port = Integer.parseInt(parameters.get(1));
        this.username = parameters.get(2);

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

        this.go = true;

        // Starts our run function
        run(in);
    }

    /**
     * Function that gets run to continuously check for user input
     */
    public void run( Scanner in )
    {
        // prints the board
        printBoard();
        // prompts the user to input
        this.serverConn.log(PROMPT);

        while(this.go()) {
            String [] playerInput = in.nextLine().trim().split(" ");
            PlaceTile newTile;

            // Check if user exits program
            if(playerInput[0].equals("-1") && playerInput.length == 1) {
                this.go = false;
            }
            else if(playerInput.length != 3) {
                // if the user enters something that isn't -1 and doesn't have 3 arguments
                // let them know their input was invalid.
                this.serverConn.logErr("Please enter a valid command.");
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
                        this.serverConn.logErr("Please enter a valid tile on the board.");
                    }
                }
                else {
                    this.serverConn.logErr("Please enter a color value between 0-15.");
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
        // reprints the board
        printBoard();
        // prompts the user again
        System.out.println(PROMPT);
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
        refresh();
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
