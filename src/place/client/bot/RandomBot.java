package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.NetworkClient;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Thread.sleep;

/**
 * A Place Bot which connects and randomly places tiles around the board.
 *
 * Note: running like 10 of these looks really cool in the GUI.
 */
public class RandomBot extends BotApplication implements BotProtocol {

    /**
     * The minimum color we can choose (located at index 0)
     */
    private static final int MIN_COLOR = 0;

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
     * Our currently selected color.
     */
    private int currentColor;

    /**
     * The number of milliseconds between the sending of each PlaceTile.
     */
    private int speed = DEFAULT_SPEED;

    /**
     *
     */
    private boolean sticky;

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
            this.serverConn = new NetworkClient(host, port, this.username, getClass().getSimpleName(), this.model);
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
        this.serverConn.log("Setup complete. Bot is starting.");

        // starts the serverCon listening (not really used because the bot doesn't display the board at all)
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
        // creates a new random number generator
        Random rand = new Random();
        // gets the highest row and column we can go to and subtracts
        int rows = this.model.getDIM();
        int cols = this.model.getDIM();

        // keeps going until the go boolean is set to false
        while(this.go())
        {
            // if we're paused, sleep for a second and then try again
            if(this.paused())
            {
                try { sleep(PAUSED_CHECK); } catch(InterruptedException ie){/* do nothing just continue forward */}
            }
            // otherwise place the next tile
            else
            {
                // gets a random row and col
                int row = rand.nextInt(rows);
                int col = rand.nextInt(cols);

                // gets a random color if we're not sticky
                if(!sticky)
                    this.currentColor = rand.nextInt(PlaceColor.TOTAL_COLORS);

                // send a tile at that location
                this.serverConn.sendTile(
                        new PlaceTile(row, col, this.username,
                                PlaceColor.values()[this.currentColor], System.currentTimeMillis())
                );

                // sleeps for however long speed is set to
                try { sleep(this.speed); } catch(InterruptedException ie){/* do nothing we don't care */}
            }
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
        printHelp();
        // continues looping until we need to quit
        while(this.go())
        {
            // prints out the prompt character
            System.out.print(this.username + PROMPT);
            // gets the next command (first full word)
            // sets it to lowercase just so any form can be understood (i.e. eXiT == exit)
            String command = in.next().toLowerCase().trim();

            // gets the rest of the line and throws it into a tokens array (used for sticky)
            String [] tokens = in.nextLine().trim().split(" ");

            // goes through each recognized command and performs the actions associated with them
            // if not recognized, a message is print saying so
            switch(command)
            {
                case HELP:
                    printHelp();
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
                case SPEED:
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
                case STICKY:
                    // if we were given a color we use it
                    if(tokens[0].equals(""))
                    {
                        sticky();
                        // otherwise we use the default method
                    }
                    else
                    {
                        try { sticky(Integer.parseInt(tokens[0])); }
                        catch(NumberFormatException e) { badCommand(command + " " + tokens[0]); }
                    }
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
     * Prints the help manual to the screen (so the user knows what commands do what)
     */
    private void printHelp()
    {
        System.out.println(
                "----------------------------------------- Commands -----------------------------------------\n" +
                        "  help : display this information again.\n" +
                        "  quit : exits the bot cleanly.\n" +
                        "  pause : pauses the bot at its current tile.\n" +
                        "  resume : resumes the bots cycle at its current tile.\n"+
                        "  speed [number] : sets the time in milliseconds between each tile the bot places.\n"+
                        "  \t (note: number must be " + MIN_SPEED + "-" + MAX_SPEED + "; if none given, speed is set to 1000.)\n" +
                        "  sticky [color] : keeps the bot on a single color.\n" +
                        "  \t (note: color must be " + MIN_COLOR + "-" + MAX_COLOR + "; if none given, color is set to the currently selected.)\n" +
                        "  random : change the color to a random color for every tile placed.\n" +
                        "--------------------------------------------------------------------------------------------"
        );
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
            badCommand("speed " + speed);
            return;
        }
        // tell the user we are now setting our speed
        this.serverConn.log("Setting the place speed to " + speed + "ms.");
        // sets our speed
        this.speed = speed;
    }

    /**
     * Sets the mode of the Bot to be random mode. (This is default action of the bot.)
     */
    private void random()
    {
        // logs we are switching to random mode
        this.serverConn.log("Changing to random color mode.");

        // sets us in random mode
        this.sticky = false;
    }

    /**
     * Sets the mode of the Bot to sticky on the current color.
     */
    private void sticky()
    {
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
        this.serverConn.log("Changing to sticky mode on " + PlaceColor.values()[color].name() + ".");

        // sets the current color
        this.currentColor = color;

        // sets us in sticky mode
        this.sticky = true;
    }

    /**
     * If the user inputs a bad command, they are alerted to this and the Bot keeps running.
     *
     * @param command The command that was passed in which was invalid.
     */
    private void badCommand(String command)
    {
        // logs that a bad command was given
        this.serverConn.log("\"" + command + "\" is not recognized as a valid command.");
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
            System.err.println("Please run the bot as: ");
            System.err.println("$ java RandomBot host port username");
            return;
        }

        try
        {
            // launch the application
            BotApplication.launch(RandomBot.class, args);
        }
        catch(Exception e)
        {
            System.err.println("We've hit an unrecoverable issue. Please try to launch again.");
            System.err.println( e.getMessage() );
        }
    }
}
