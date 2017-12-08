package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceTile;
import place.network.NetworkClient;

import java.util.Scanner;

import static java.lang.Thread.sleep;

// fully commented

/**
 * A multithreaded Bot client that connects to a PlaceServer and performs actions that FILL the screen with color.
 *
 * @author Kevin Becker (kjb2503)
 */
public class FillBot extends BotApplication implements BotProtocol {

    // a few custom items that apply ONLY to this Bot
    /**
     * The CYCLE command is called to make the color which the FillBot places occur once it fills the entire screen.
     */
    private static final String CYCLE = "cycle";

    /**
     * The manual that is printed at the start and when help is called.
     */
    private static final String FILL_MANUAL =
        "----------------------------------------- Commands -----------------------------------------\n" +
        "  help : display this information again.\n" +
        "  quit : exits the Bot.\n" +
        "  about : displays information about the Bot.\n" +
        "  pause : pauses the Bot at its current tile.\n" +
        "  resume : resumes filling at its current tile.\n"+
        "  speed [number] : sets the time in milliseconds between each tile the Bot places.\n" +
        "  \t (note: number must be " + MIN_SPEED + "-" + MAX_SPEED + "; if none given, speed is set to 1000.)\n" +
        "  sticky [color] : keeps the Bot on a single color.\n" +
        "  \t (note: color must be " + MIN_COLOR + "-" + MAX_COLOR + "; if none given, color is set to the currently selected.)\n" +
        "  cycle : fills the board with a single color then goes to the next.\n" +
        "  rainbow : change color to the next color for every tile placed.\n" +
        "  random : change the color to a random color for every tile placed.\n" +
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
     * The current row we are filling at.
     */
    private int currentRow;

    /**
     * The current column we are filling at.
     */
    private int currentCol;

    /**
     * The currently selected PlaceColor that will be used to send to the server to make a move.
     */
    private int currentColor = 0;

    /**
     * The number of milliseconds between the sending of each PlaceTile.
     */
    private int speed = DEFAULT_SPEED;

    /**
     * The boolean used to tell if we should stick with the same color or not.
     *
     * If the thread should be in sticky mode, this is true; false otherwise.
     */
    private boolean sticky;

    /**
     * The boolean used to tell if we are in rainbow mode or not.
     *
     * If the thread should be in rainbow mode this is true; false otherwise.
     */
    private boolean rainbow;

    /**
     * The boolean used to tell if we are in random mode or not.
     *
     * If the thread should be in random mode this is true; false otherwise.
     */
    private boolean random;

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

        // logs that the setup is complete
        this.serverConn.log(SETUP_COMPLETE_MSG);

        // starts the serverCon listening (not really used because the Bot doesn't display the board at all)
        this.serverConn.start();

        // sets go to true
        this.go = true;

        // creates a new thread which runs the Bot's filling procedures
        new Thread(this::run).start();
    }

    /**
     * The run method is used to loop through the Fill action.
     */
    private void run()
    {
        // used to indicate the current row and column
        this.currentRow = 0;
        this.currentCol = 0;

        // gets the highest row and column we can go to and subtracts
        int rows = this.model.getDIM();
        int cols = this.model.getDIM();

        // keeps going until the go boolean is set to false
        while(this.go())
        {
            // if we're paused, sleep for a second and then try again
            if(this.paused())
            {
                try { sleep(1000); } catch(InterruptedException ie){/* do nothing just continue forward */}
            }
            // otherwise place the next tile
            else
            {
                // send a tile
                this.serverConn.sendTile(
                        new PlaceTile(currentRow, currentCol, this.username,
                                PlaceColor.values()[this.currentColor], System.currentTimeMillis())
                );
                // adds one to row and mod by cols (this way it sets to 0 if needed)
                currentCol = (++currentCol) % cols;

                // if col was reset, add one to row and mod by total rows (this way it sets to 0 if needed)
                if( currentCol == 0 )
                    currentRow = (++currentRow) % rows;

                // sets us to a new color if needed
                if((currentRow == 0 && currentCol == 0 || this.rainbow || this.random)  && !sticky)
                {
                    // changes the color
                    this.currentColor = (this.random) ? ((int)Math.floor((Math.random()) * PlaceColor.TOTAL_COLORS)) :
                            ((currentColor + 1) % PlaceColor.TOTAL_COLORS);
                }

                // sleeps for however long speed is set to
                try { sleep(this.speed); } catch(InterruptedException ie){/* do nothing we don't care */}
            }
        }
    }

    /**
     * Starts the listening for commands from the user to make the Bot do different actions.
     *
     * @param in A Scanner which is used to take commands from the user to make the Bot do different actions.
     */
    @Override
    public void listen(Scanner in)
    {
        // prints out help before the first run so users know what commands there are
        BotApplication.printHelp(FILL_MANUAL);
        // continues looping until we need to quit
        while(this.go())
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
                    BotApplication.printHelp(FILL_MANUAL);
                    break;
                // quit or exit can be used (just in case)
                case EXIT:
                case QUIT:
                    exit();
                    break;
                case ABOUT:
                    about();
                    break;
                case PAUSE:
                case STOP:
                    pause();
                    break;
                case RESUME:
                case PLAY:
                    resume();
                    break;
                case SPEED:
                    // if we were given a speed we use it
                    if(tokens[0].equals(""))
                        speed();
                    // otherwise we use the default method
                    else
                        try { speed(Integer.parseInt(tokens[0])); }
                        // if we catch a NFE it was an issue with the type of input, it's invalid
                        catch(NumberFormatException e) { invalidCommand(command + " " + tokens[0]); }
                        // if we catch a IOOB it was an issue with the NUMBER of inputs, its formatted improperly.
                        catch(IndexOutOfBoundsException ioob) { badFormat(command); }
                    break;
                case STICKY:
                    // if we were given a color we use it
                    if(tokens[0].equals(""))
                    {
                        sticky();
                        // otherwise we use the default method
                    }
                    else
                    {
                        // tries to run sticky
                        try { sticky(Integer.parseInt(tokens[0])); }
                        // if we catch a NFE it was an issue with the type of input, it's invalid
                        catch(NumberFormatException e) { invalidCommand(command + " " + tokens[0]); }
                        // if we catch a IOOB it was an issue with the NUMBER of inputs, its formatted improperly.
                        catch(IndexOutOfBoundsException ioob) { badFormat(command); }
                    }
                    break;
                case CYCLE:
                    cycle();
                    break;
                case RAINBOW:
                    rainbow();
                    break;
                case RANDOM:
                    random();
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
        this.serverConn.log(EXIT_MSG);
        // sets go to false indicating to the thread it needs to stop
        this.go = false;
    }

    /**
     * Displays information about the Bot.
     */
    private void about()
    {
        // logs the current status of the Bot
        this.serverConn.log("This is FillBot. It likes to fill the entire board with color.");
        this.serverConn.log("FillBot has four modes: cycle, sticky, rainbow, and random.");
        this.serverConn.log("It is currently set to " +
                ((this.sticky) ? "sticky mode, which means it only places a single color, always." :
                        ((this.rainbow) ? "rainbow mode, which means it changes to the next color for every tile." :
                                ((this.random) ? "random mode, which means it chooses a random color for every tile.":
                                                 "cycle mode, which means it fills the board with one color, then moves to the next."))));
        this.serverConn.log("FillBot is currently filling at " + this.currentRow + ", " + this.currentCol + ").");
        this.serverConn.log("Its current color is " + PlaceColor.values()[this.currentColor].name());
    }

    /**
     * Sets the Bot on pause.
     */
    private void pause()
    {
        // logs we are pausing the fill
        this.serverConn.log(PAUSE_MSG);
        // pauses the fill
        this.pause = true;
    }

    /**
     * Resumes the Bot on its cycle.
     */
    private void resume()
    {
        // logs that we will resume
        this.serverConn.log(RESUME_MSG);
        // resumes the fill
        this.pause = false;
    }

    /**
     * Sets the length of the wait to the default speed.
     */
    private void speed()
    {
        // resets the speed to default
        speed(DEFAULT_SPEED);
    }

    /**
     * Sets the length of the wait to the speed passed in.
     *
     * @param speed The number of milliseconds to wait between each place of the PlaceTile.
     */
    private void speed(int speed)
    {
        // makes sure its a valid speed
        if(speed < MIN_SPEED || speed > MAX_SPEED)
        {
            // if it isn't tell the user
            invalidCommand(SPEED + " " + speed);
            return;
        }
        // logs we are changing speeds
        this.serverConn.log("Setting the place speed to " + speed + "ms.");
        // sets our speed
        this.speed = speed;
    }

    /**
     * Sets the mode of the Bot to sticky on the current color.
     */
    private void sticky()
    {
        // calls the sticky method with the current color
        sticky(this.currentColor);
    }

    /**
     * Sets the mode of the Bot to sticky on the color passed in.
     *
     * @param color The color to remain sticky on.
     */
    private void sticky(int color)
    {
        // makes sure its a valid color
        if(color < MIN_COLOR || color >= MAX_COLOR)
        {
            // if it isn't tell the user
            invalidCommand(STICKY + " " + color);
            return;
        }

        // logs we are switching to sticky mode
        this.serverConn.log("Changing to sticky mode on " + PlaceColor.values()[color].name() + ".");

        // sets the current color
        this.currentColor = color;

        // sets us in sticky mode
        this.sticky = true;
        this.rainbow = false;
        this.random = false;
    }

    /**
     * Sets the mode of the Bot to be cycle mode.
     */
    private void cycle()
    {
        // logs we are gong to cycle mode
        this.serverConn.log("Changing to cycle color mode.");

        // sets us in cycle mode
        this.sticky = false;
        this.rainbow = false;
        this.random = false;
    }

    /**
     * Sets the mode of the Bot to be rainbow color mode.
     */
    private void rainbow()
    {
        // logs that we are changing modes
        this.serverConn.log("Get ready to see color!");

        // sets us in rainbow mode
        this.sticky = false;
        this.rainbow = true;
        this.random = false;
    }

    /**
     * Sets the mode of the Bot to be random color mode.
     */
    private void random()
    {
        // logs we are switching to random mode
        this.serverConn.log("Changing to random color mode.");

        // sets us in random mode
        this.sticky = false;
        this.rainbow = false;
        this.random = true;
    }

    /**
     * Logs if an invalid command has been placed.
     *
     * @param command The command that was passed which is invalid.
     */
    private void invalidCommand(String command)
    {
        // logs the invalid command
        this.serverConn.log("\"" + command + "\" " + INVALID_MSG);
    }

    /**
     * Logs if an if a command is formatted properly.
     *
     * @param command The command type that was passed which was invalid.
     */
    private void badFormat(String command)
    {
        // logs the bad format
        this.serverConn.log("\"" + command + "\" " + FORMAT_MSG);
    }

    /**
     * If the user inputs a bad command, they are alerted to this and the Bot keeps running.
     *
     * @param command The command that was passed in which was invalid.
     */
    private void badCommand(String command)
    {
        // logs the bad command
        this.serverConn.log("\"" + command + "\" " + BAD_CMD_MSG);
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
        // we need exactly 3 arguments
        if(args.length != 3)
        {
            System.err.println("Please run the Bot as: ");
            System.err.println("$ java FillBot host port username");
            return;
        }

        try
        {
            // launch the application
            BotApplication.launch(FillBot.class, args);
        }
        catch(Exception e)
        {
            System.err.println("We've hit an unrecoverable issue. Please try to launch again.");
        }
    }
}
