package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.NetworkClient;


import java.util.*;

import static java.lang.Thread.sleep;

// fully commented

/**
 * A type of Bot client that connects to a PlaceServer and performs actions that FILL the screen with color.
 *
 * @author Kevin Becker (kjb2503)
 */
public class FillBot extends BotApplication {

    /**
     * The list of PlaceColor choices that the user can select from.
     */
    private static List<PlaceColor> COLOR_CHOICES;

    /**
     * The minimum color we can choose (located at index 0)
     */
    private static final int MIN_COLOR = 0;

    /**
     * The maximum color we can choose (located at index TOTAL_COLORS - 1)
     */
    private static final int MAX_COLOR = PlaceColor.TOTAL_COLORS - 1;

    /**
     * The prompt String that is posted every time a user should provide input.
     */
    private final static String PROMPT = ">>>";

    /**
     * The minimum number of milliseconds to wait between each place of a PlaceTile.
     */
    private final static int MAX_SPEED = 2000;

    /**
     * The default number of milliseconds to wait between each place of a PlaceTile.
     */
    private final static int DEFAULT_SPEED = 50;

    /**
     * The maximum number of milliseconds to wait between each place of a PlaceTile.
     */
    private final static int MIN_SPEED = 20;

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
     * Initializes the bot.
     *
     * @throws Exception if any exception is run into
     */
    @Override
    public void init() throws Exception
    {
        // gets our arguments from BotApplication
        List<String> arguments = super.getArguments();

        // sets our required elements
        String host = arguments.get(0);
        int port = Integer.parseInt(arguments.get(1));
        this.username = arguments.get(2);

        // creates a new blank model
        this.model = new PlaceBoardObservable();

        try
        {
            // sets our network client, this is the last thing we do to minimize time between receiving the board and
            // opening the Bot
            this.serverConn = new NetworkClient(host, port, username, model);
        }
        catch(PlaceException e)
        {
            // closes serverConn
            this.serverConn.close();
            // runs our stop method so that we can deconstruct anything we've built thus far
            // this.stop();
            // tells the user about the issue we've run into
            throw e;
        }
    }

    /**
     * Starts the Bot by connecting to the server, and starting the Bot filling thread.
     */
    @Override
    public void start()
    {
        // logs that the setup is complete
        log("Setup complete. Bot is starting.");

        // starts the serverCon listening (not really used because the bot doesn't display the board at all)
        this.serverConn.start();

        // sets go to true
        this.go = true;

        // creates a new thread which runs the Bot's filling procedures
        new Thread(this::run).start();
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
        printHelp();
        // continues looping until we need to quit
        while(this.go())
        {
            // prints out the prompt character
            System.out.print(PROMPT);
            // gets the next command (first full word)
            // sets it to lowercase just so any form can be understood (i.e. eXiT == exit)
            String command = in.next().toLowerCase().trim();

            // gets the rest of the line and throws it into a tokens array (used for sticky)
            String [] tokens = in.nextLine().trim().split(" ");

            // goes through each recognized command and performs the actions associated with them
            // if not recognized, a message is print saying so
            switch(command)
            {
                case "help":
                    printHelp();
                    break;
                // quit or exit can be used (just in case)
                case "exit":
                case "quit":
                    exit();
                    break;
                case "pause":
                case "stop":
                    pause();
                    break;
                case "resume":
                case "play":
                    resume();
                    break;
                case "speed":
                    // if we were given a speed we use it
                    if(tokens[0].equals(""))
                        speed();
                    // otherwise we use the default method
                    else
                    {
                        try { speed(Integer.parseInt(tokens[0])); }
                        catch(NumberFormatException e) { badCommand(command + " " + tokens[0]); }
                    }
                    break;
                case "sticky":
                    // if we were given a color we use it
                    if(tokens[0].equals(""))
                        sticky();
                        // otherwise we use the default method
                    else
                    {
                        try { sticky(Integer.parseInt(tokens[0])); }
                        catch(NumberFormatException e) { badCommand(command + " " + tokens[0]); }
                    }
                    break;
                case "cycle":
                    cycle();
                    break;
                case "rainbow":
                    rainbow();
                    break;
                default:
                    badCommand(command);
            }
        }
    }

    /**
     * Prints the help manual to the screen (so the user knows what commands do what)
     */
    private void printHelp()
    {
        System.out.println(
            "----------------------------------------- Commands -----------------------------------------\n" +
            "| help : display this information again.\n" +
            "| quit : exits the bot cleanly.\n" +
            "| pause : pauses the bot at its current tile.\n" +
            "| resume : resumes the bots cycle at its current tile.\n"+
            "| speed [number] : sets the time in milliseconds between each tile the bot places.\n"+
            "|\t (note: the number is optional, it must be an integer 20-2000.)\n" +
            "| sticky [color] : keeps the bot on a single color.\n" +
            "| \t (note: the color is optional, it must be an integer 0-15.)\n" +
            "| cycle : fills the board with a single color then goes to the next.\n" +
            "| rainbow : change color for every tile placed.\n"+
            "--------------------------------------------------------------------------------------------"
        );
    }

    /**
     * Exits the Bot.
     */
    private void exit()
    {
        // logs we are exiting
        log("Exiting the Bot.");
        // sets go to false indicating to the thread it needs to stop
        this.go = false;
    }

    /**
     * Sets the Bot on pause.
     */
    private void pause()
    {
        // logs we are pausing the fill
        log("Pausing the fill. To resume, use \"resume\".");
        // pauses the fill
        this.pause = true;
    }

    /**
     * Resumes the Bot on its cycle.
     */
    private void resume()
    {
        // logs that we will resume
        log("Resuming the fill.");
        // resumes the fill
        this.pause = false;
    }

    /**
     * Sets the length of the wait to the default speed.
     */
    private void speed()
    {
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
            badCommand("speed " + speed);
            return;
        }
        // tell the user we are now setting our speed
        log("Setting the place speed to " + speed + "ms.");
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
            badCommand("sticky " + color);
            return;
        }

        // if we've made it here, we can log that we are making a change
        log("Changing to sticky mode on color " + color + ".");

        // sets the current color
        this.currentColor = color;

        // sets us in sticky mode
        this.sticky = true;
        this.rainbow = false;
    }

    /**
     * Sets the mode of the Bot to be cycle mode.
     */
    private void cycle()
    {
        // logs we are gong to cycle mode
        log("Changing to cycle mode.");

        // sets us in cycle mode
        this.rainbow = false;
        this.sticky = false;
    }

    /**
     * Sets the mode of the Bot to be rainbow mode.
     */
    private void rainbow()
    {
        // logs that we are changing modes
        log("Get ready to see color!");
        // sets sticky to false
        this.sticky = false;
        // sets rainbow to true
        this.rainbow = true;
    }

    /**
     * If the user inputs a bad command, they are alerted to this and the Bot keeps running.
     *
     * @param command The command that was passed in which was invalid.
     */
    private void badCommand(String command)
    {
        // logs that a bad command was given
        log("\"" + command + "\" is not recognized as a command.");
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
     * The run method is used to loop through the Fill action.
     */
    private void run()
    {
        // used to indicate the current row and column
        int currentRow = 0;
        int currentCol = 0;
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
                                COLOR_CHOICES.get(this.currentColor), System.currentTimeMillis())
                );
                // adds one to row and mod by cols (this way it sets to 0 if needed)
                currentCol = (++currentCol) % cols;

                // if col was reset, add one to row and mod by total rows (this way it sets to 0 if needed)
                if( currentCol == 0 )
                    currentRow = (++currentRow) % rows;

                // sets the color to the next in the list if needed
                if((currentRow == 0 && currentCol == 0  || this.rainbow) && !this.sticky)
                    this.currentColor = (currentColor + 1) % COLOR_CHOICES.size();

                // sleeps for however long speed is set to
                try { sleep(this.speed); } catch(InterruptedException ie){/* do nothing we don't care */}
            }
        }
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
            System.err.println("Please run the bot as: ");
            System.err.println("$ java FillBot host port username");
            return;
        }

        // creates a new List of color choices
        COLOR_CHOICES = Arrays.asList(PlaceColor.values());

        try
        {
            // launch the application
            BotApplication.launch(FillBot.class, args);
        }
        catch(Exception e)
        {
            System.err.println("We've hit an unrecoverable issue. Please try to launch again.");
            System.err.println( e.getMessage() );
        }
    }
}
