package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceException;
import place.network.NetworkClient;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

// fully commented

/**
 * The BotApplication class is used to launch a Bot client which connects to a PlaceServer.
 *
 * @author Kevin Becker (kjb2503)
 * @author James Heliotis
 */
public abstract class BotApplication {

    /**
     * The array of arguments passed in by the Subclass when launching.
     */
    private String[] arguments;

    /**
     * The event thread.
     */
    private Thread botThread;

    /**
     * The NetworkClient is housed within BotApplication
     */
    private NetworkClient serverConn;

    /**
     * The PlaceBoardObservable that is passed to the Bot upon startup
     */
    private PlaceBoardObservable model;

    /**
     * The Bot's username.
     */
    private String username;

    /**
     * The class name of the Bot that launched this iteration of the application.
     */
    private String className;

    /**
     * Run a Bot application.
     * Launch a standalone application. This method is typically called from the main method().
     * It must not be called more than once or an exception will be thrown. This is equivalent to launch(TheClass.class, args) where TheClass is the immediately enclosing class of the method that called launch. It must be a subclass of Application or a RuntimeException will be thrown.
     * The launch method does not return until the application has exited, either via a call to Platform.exit or all of the application windows have been closed.
     * Typical usage is:
     * public static void main(String[] args)
     * {
     *      BotApplication.launch( [class );
     *
     * }
     * @param botClass the class object that refers to the class that is to be instantiated.
     */
    public static void launch(Class< ? extends BotApplication> botClass) { launch(botClass, new String[ 0 ]); }

    /**
     * Run a complete Bot application, with command line arguments.
     *
     * Typical usage is:
     * public static void main(String[] args)
     * {
     *     BotApplication.launch( [class], args)
     * }
     *
     * @param botClass the class object that refers to the class to
     *             be instantiated
     * @param args the array of strings from the command line
     */
    public static void launch(Class< ? extends BotApplication > botClass, String[] args)
    {
        try
        {
            // tries to set up a new instance of the Bot class
            BotApplication bot = botClass.newInstance();

            // sets our className
            bot.className = botClass.getSimpleName();

            // makes a copy of the arguments
            bot.arguments = Arrays.copyOf( args, args.length );

            try
            {
                // tries to run the init method of the Bot class
                bot.init();
                // creates a new BotRunner (private class below)
                bot.botThread = new Thread( new BotRunner( bot ) );
                // attempts to start the thread
                bot.botThread.start();
                // attempts to join it
                bot.botThread.join();
            }
            catch( Exception ie )
            {
                // if ANY sort of issue is had while running, print it to standard error
                System.err.println("Bot thread interrupted.");
            }
            finally
            {
                // stops the Bot
                bot.stop();
            }
        }
        catch( InstantiationException ie )
        {
            System.err.println("Bot instantiation failed:");
            System.err.println( ie.getMessage() );
        }
        catch( IllegalAccessException iae )
        {
            System.err.println( iae.getMessage() );
        }
    }

    /**
     * The init method is the same for all Bots. They require the same setup every time. Classes can override them, but
     * they must run the super method first.
     */
    public void init() throws Exception
    {
        // gets our arguments from BotApplication
        List<String> arguments = getArguments();

        // sets our required elements
        String host = arguments.get(0);
        int port = Integer.parseInt(arguments.get(1));

        this.username = arguments.get(2);

        // creates a new blank model
        this.model = new PlaceBoardObservable();

        // tries to setup the serverConn
        try
        {
            // sets our network client, this is the last thing we do to minimize time between receiving the board and
            // opening the Bot
            this.serverConn = new NetworkClient(host, port, this.username, this.className, model);
        }
        catch(PlaceException e)
        {
            // closes serverConn
            serverConn.close();
            // runs our stop method so that we can deconstruct anything we've built thus far
            // this.stop();
            // tells the user about the issue we've run into
            throw e;
        }
    }

    /**
     * Used to fetch the Bot application's command line arguments. It simply returns a list that was passed in as
     * arguments.
     *
     * @return The string array of arguments passed in at launch as a List of Strings.
     */
    List< String > getArguments()
    {
        return Arrays.asList(this.arguments );
    }

    /**
     * A do-nothing stop method. It does nothing so that it can be overwritten by subclasses when necessary.
     */
    public void stop() {}


    /**
     * A private class that keeps the Bot running while the application should be running.
     */
    private static class BotRunner implements Runnable
    {
        /**
         * The BotApplication we want to run.
         */
        private final BotApplication bot;

        /**
         * The constructor for BotRunner that is used to create a BotRunner runnable.
         *
         * @param bot the BotApplication we want to run.
         */
        BotRunner( BotApplication bot )
        {
            this.bot = bot;
        }

        /**
         * The run method used by the thread for the Bot.
         */
        public void run()
        {
            // once this completes, the Bot has disconnected from the PlaceServer for some reason
            try ( Scanner in = new Scanner( System.in ) ) {
                // starts the Bot (indication that it is go time)
                bot.start( bot.serverConn, bot.username, bot.model );
                // starts the Bot listening for commands
                bot.listen(in);
            }
            catch( Exception e )
            {
                // print a stack trace if anything fatal occurs
                e.getMessage();
            }
        }
    }

    /**
     * The method that begins the start procedure of the Bot.
     */
    public abstract void start( NetworkClient serverConn, String username, PlaceBoardObservable model ) throws Exception;

    /**
     * A do-nothing command listener that must be implemented by a user. At the base it must handle exiting.
     */
    public abstract void listen(Scanner in);

    /**
     * This is used by the thread to make sure it should keep going.
     *
     * Synchronized so that we only allow running it one thread at a time.
     *
     * @return true if this.go is set to true; false otherwise.
     */
    public abstract boolean go();


    // STATIC METHOD ===========================
    /**
     * Prints a help manual to the screen (so the user knows what commands do what)
     */
    public static void printHelp(String manual)
    {
        System.out.println(manual);
    }

    /**
     * Prints out a help manual to the screen so a user of a Bot can tell what the Bot does..
     *
     * @param prompt The help manual that should be print to the screen.
     */
    public static void prompt(String prompt)
    {
        System.out.print(prompt);
    }
}
