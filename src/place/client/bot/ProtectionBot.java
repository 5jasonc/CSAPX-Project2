package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceTile;
import place.network.NetworkClient;

import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

// fully commented

/**
 * A multithreaded Bot client that connects to a PlaceServer and performs actions that FILL the screen with color.
 *
 * TODO: Put the command response methods as static methods in BotApplication
 *
 * @author Kevin Becker (kjb2503)
 */
public class ProtectionBot extends BotApplication implements BotProtocol, Observer {

    // a few custom items that apply ONLY to this Bot
    /**
     * The protect command that is used to change the protect location.
     */
    private final static String PROTECT = "protect";
    /**
     * The change command is used to
     */
    private final static String CHANGE = "change";

    /**
     * This is paired with the CHANGE command. It is sent when only the color should be changed.
     */
    private final static String COLOR = "color";

    /**
     * This is paired with the CHANGE command. It is sent when only the ROW should be changed.
     */
    private final static String ROW = "row";

    /**
     * This is paired with the CHANGe command. It is sent when only the COL should be changed.
     */
    private final static String COL = "col";

    /**
     * The manual that is printed at the start and when help is called.
     */
    private final static String PROTECT_MANUAL =
        "----------------------------------------- Commands -----------------------------------------\n" +
        "  help : display this information again.\n" +
        "  quit : exits the bot cleanly.\n" +
        "  pause : pauses the bot at its current tile.\n" +
        "  resume : resumes the bots cycle at its current tile.\n"+
        "  protect row col color : protects a certain row, column, with a color\n" +
        "  change [\"row\",\"col\",\"color\"] value : changes the row, column, or color being protected.\n" +
        "  \t (note: row and col must fit on the board; color must be " + MIN_COLOR + "-" + MAX_COLOR + ".)\n" +
        "--------------------------------------------------------------------------------------------";

    //=============================================

    /**
     * The connection to the server through a NetworkClient.
     */
    private NetworkClient serverConn;

    /**
     * The username that this user wishes to have.
     */
    private String username;

    /**
     * The model which is used to house the board.
     */
    private PlaceBoardObservable model;

    /**
     * The row on the board we want to protect.
     */
    private int protectedRow;

    /**
     * The column on the board we want to protect.
     */
    private int protectedCol;

    /**
     * The currently selected PlaceColor that will be used to send to the server to make a move.
     */
    private int protectedColor;

    /**
     * The indicator to the thread whether it should keep running or not.
     *
     * If the thread should continue running this is true; false otherwise.
     */
    private boolean go;

    /**
     * The boolean used to tell if we are paused or not.
     *
     * If the thread should pause sending the tiles this is true; false otherwise.
     */
    private boolean pause;

    /**
     * This is used by the thread to make sure it should keep going.
     *
     * Synchronized so that we only allow running it one thread at a time.
     *
     * @return true if this.go is set to true; false otherwise.
     */
    @Override
    public synchronized boolean go()
    {
        return this.go;
    }

    /**
     * This is used by the thread to check if it should be paused or not.
     *
     * Synchronized so that we only allow running it once thread at a time.
     *
     * @return true if this.pause is true; false otherwise.
     */
    private synchronized boolean paused()
    {
        return this.pause;
    }

    /**
     * Starts the Bot by connecting to the server, and starting the Bot filling thread.
     */
    @Override
    public void start( NetworkClient serverConn, String username, PlaceBoardObservable model )
    {
        // sets the fields we were passed to start
        this.serverConn = serverConn;
        this.username = username;
        this.model = model;

        // sets go to true (might be changed later)
        this.go = true;

        // gets our arguments
        List< String > arguments = super.getArguments();

        try
        {
            // row
            this.protectedRow = Integer.parseInt(arguments.get(3));
            // column
            this.protectedCol = Integer.parseInt(arguments.get(4));
            // heck
            this.protectedColor = Integer.parseInt(arguments.get(5));
        }
        catch(NumberFormatException | IndexOutOfBoundsException e)
        {
            // if we hit an error, log it and set go to false (we can't begin)
            this.serverConn.logErr("Fatal error in starting up. The Bot will now close.");
            this.go = false;
        }

        if(this.go())
        {
            // logs that the setup is complete
            this.serverConn.log("Setup complete. Bot is starting.");

            // add ourselves as an observable
            this.model.addObserver(this);

            // starts the serverCon listening (not really used because the bot doesn't display the board at all)
            this.serverConn.start();
        }
    }

    /**
     * Updates the Bot (basically just a trigger to check if we need to place our spot again).
     *
     * @param o The Observable that called update.
     * @param tile The tile that was (hopefully) sent with the call to update.
     */
    public void update(Observable o, Object tile)
    {
        // checks to be double sure we're being updated by our Observable
        assert (this.model == o) : "Update called from non-observable.";

        // if the Object is a tile (it should be) and we are not paused
        if(!this.paused() && tile instanceof PlaceTile)
        {
            // cast our tile as a tile
            PlaceTile newTile = (PlaceTile) tile;

            // sends a new tile back because we are selfish and want our name on the tile
            // (we're protecting it after all)
            if(newTile.getRow() == this.protectedRow && newTile.getCol() == this.protectedCol
                    && !newTile.getOwner().equals(this.username))
                this.serverConn.sendTile(new PlaceTile(this.protectedRow, this.protectedCol, this.username,
                        PlaceColor.values()[this.protectedColor], System.currentTimeMillis()));
        }
    }

    /**
     * Starts the listening for commands from the user to make the Bot do different actions.
     *
     * @param in A Scanner which is used to take commands from the user to make the bot do different actions.
     */
    @Override
    public void startCmdListening(Scanner in)
    {
        // prints out help before the first run so users know what commands there are
        BotApplication.printHelp( PROTECT_MANUAL );
        // continues looping until we need to quit
        while( this.go() )
        {
            // prints out the prompt character
            BotApplication.prompt(this.username + PROMPT);
            // gets the next command (first full word)
            // sets it to lowercase just so any form can be understood (i.e. eXiT == exit)
            String command = in.next().toLowerCase().trim();

            // gets the rest of the line and throws it into a tokens array (used for sticky)
            String [] tokens = in.nextLine().toLowerCase().trim().split(" ");

            // goes through each recognized command and performs the actions associated with them
            // if not recognized, a message is print saying so
            switch(command)
            {
                case HELP:
                    BotApplication.printHelp( PROTECT_MANUAL );
                    break;
                // quit or exit can be used (just in case)
                case EXIT:
                case QUIT:
                    exit();
                    break;
                case PAUSE:
                case STOP:
                    pause();
                    break;
                case RESUME:
                case PLAY:
                    resume();
                    break;
                case PROTECT:
                    // protect row col color
                    try
                    {
                        // calls the change all as we are now changing everything
                        changeAll(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]));
                    }
                    // if we catch a NFE it was an issue with the type of input, it's invalid
                    catch(NumberFormatException e) { invalidCommand(command + " " + tokens[0]); }
                    // if we catch a IOOB it was an issue with the NUMBER of inputs, its formatted improperly.
                    catch(IndexOutOfBoundsException ioob) { badFormat(command); }
                    break;
                case CHANGE:
                    // figures out which item we are trying to change
                    try
                    {
                        switch (tokens[0])
                        {
                            case ROW:
                                // changes our row
                                changeRow(Integer.parseInt(tokens[1]));
                                break;
                            case COL:
                                // changes our column
                                changeCol(Integer.parseInt(tokens[1]));
                                break;
                            case COLOR:
                                changeColor(Integer.parseInt(tokens[1]));
                                break;
                            default:
                                badCommand(command + " " + tokens[0]);
                                break;
                        }
                    }
                    // if we catch a NFE it was an issue with the type of input, it's invalid
                    catch(NumberFormatException e) { invalidCommand(command + " " + tokens[0] + " " + tokens[1]); }
                    // if we catch a IOOB it was an issue with the NUMBER of inputs, its formatted improperly.
                    catch(IndexOutOfBoundsException e) { badFormat(command); }
                    break;
                default:
                    badCommand(command);
            }
        }
    }

    /**
     * Exits the Bot.
     */
    private void exit()
    {
        // logs we are exiting
        this.serverConn.log("Exiting the Bot.");
        // sets go to false indicating to the thread it needs to stop
        this.go = false;
    }

    /**
     * Sets the Bot on pause.
     */
    private void pause()
    {
        // logs we are pausing the fill
        this.serverConn.log("Pausing the fill. To resume, use \"resume\".");
        // pauses the fill
        this.pause = true;
    }

    /**
     * Resumes the Bot on its cycle.
     */
    private void resume()
    {
        // logs that we will resume
        this.serverConn.log("Resuming the fill.");
        // resumes the fill
        this.pause = false;
    }

    /**
     * Changes the protect location and color.
     *
     * @param row The row to switch protecting to.
     * @param col The column to switch protecting to.
     * @param color The color to switch protecting to.
     */
    private void changeAll(int row, int col, int color)
    {
        if((row < 0 || row >= this.model.getDIM()) ||
           (col < 0 || col >= this.model.getDIM()) ||
           (color < MIN_COLOR || color >= MAX_COLOR))
        {
            invalidCommand(PROTECT + " " + color);
            return;
        }

        // logs our change
        this.serverConn.log(
                "Changing protection to be at (" + row + "," + col + ") with color " + PlaceColor.values()[color].name()
        );

        // changes our row
        this.protectedRow = row;
        // changes our column
        this.protectedCol = col;
        // changes our color
        this.protectedColor = color;
    }

    /**
     * Changes the row that the ProtectionBot is protecting.
     *
     * @param row The row that the ProtectionBot should be protecting.
     */
    private void changeRow(int row)
    {
        // checks to make sure its a valid row
        if(row < 0 || row >= this.model.getDIM())
        {
            // tells the user if it isn't
            invalidCommand(CHANGE + " " + ROW + " " + row);
            return;
        }

        // logs the change
        this.serverConn.log("Setting protected row to " + row);
        // sets protected row
        this.protectedRow = row;
    }

    /**
     * Changes the column that the ProtectionBot is protecting.
     *
     * @param col The column that the ProtectionBot should be protecting.
     */
    private void changeCol(int col)
    {
        if(col < 0 || col >= this.model.getDIM())
        {
            // tells the user if it isn't
            invalidCommand(CHANGE + " " + COL + " " + col);
            return;
        }

        // logs the change
        this.serverConn.log("Setting protected column to " + col);
        // sets protected row
        this.protectedCol = col;
    }

    /**
     * Changes the column that the ProtectionBot is protecting.
     *
     * @param color The column that the ProtectionBot should be protecting.
     */
    private void changeColor(int color)
    {
        if(color < MIN_COLOR || color >= MAX_COLOR)
        {
            // tells the user if it isn't
            invalidCommand(CHANGE + " " + COLOR + " " + color);
            return;
        }

        // logs the change
        this.serverConn.log("Setting protected color to " + PlaceColor.values()[color].name());
        // sets protected row
        this.protectedColor = color;
    }

    /**
     * Logs if an invalid command has been sent.
     *
     * @param command The invalid command that was sent.
     */
    private void invalidCommand(String command)
    {
        // logs the invalid command
        this.serverConn.log("\"" + command + "\" is not a valid command.");
    }

    private void badFormat(String command)
    {
        // logs the bad format
        this.serverConn.log("\"" + command + "\" was formatted incorrectly. Try again.");
    }

    /**
     * If the user inputs a bad command, they are alerted to this and the Bot keeps running.
     *
     * @param command The command that was passed in which was invalid.
     */
    private void badCommand(String command)
    {
        // logs the bad command
        this.serverConn.log("\"" + command + "\" is not recognized as a command.");
    }

    /**
     * When the Bot is quit, the stop method is called which closes the connection to the server.
     */
    @Override
    public void stop()
    {
        // when the program closes we close our NetworkClient so it knows to stop executing and log us out
        super.stop();
        // indicates to serverConn that it should close
        this.serverConn.close();
    }

    /**
     * The main method which launches the FillBot.
     *
     * @param args The arguments being passed in.
     *             Should be of the form: host port username
     */
    public static void main(String[] args) {
        // we need exactly 6 arguments
        if(args.length != 6)
        {
            System.err.println("Please run the bot as: ");
            System.err.println("$ java ProtectionBot host port username row col color");
            return;
        }

        try
        {
            // launch the application
            BotApplication.launch(ProtectionBot.class, args);
        }
        catch(Exception e)
        {
            System.err.println("We've hit an unrecoverable issue. Please try to launch again.");
        }
    }
}
