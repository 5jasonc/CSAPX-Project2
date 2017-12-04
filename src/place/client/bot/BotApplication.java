package place.client.bot;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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
     * Run a bot application.
     * Launch a standalone application. This method is typically called from the main method().
     * It must not be called more than once or an exception will be thrown. This is equivalent to launch(TheClass.class, args) where TheClass is the immediately enclosing class of the method that called launch. It must be a subclass of Application or a RuntimeException will be thrown.
     * The launch method does not return until the application has exited, either via a call to Platform.exit or all of the application windows have been closed.
     * Typical usage is:
     * public static void main(String[] args)
     * {
     *      BotApplication.launch( [class );
     * }
     *
     * @param botClass the class object that refers to the class that is to be instantiated.
     */
    public static void launch(Class< ? extends BotApplication> botClass)
    {
        launch(botClass, new String[ 0 ]);
    }

    /**
     * Run a complete bot application, with command line arguments.
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
            // tries to set up a new instance of the bot class
            BotApplication bot = botClass.newInstance();
            // makes a copy of the arguments
            bot.arguments = Arrays.copyOf( args, args.length );

            try
            {
                // tries to run the init method of the bot class
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
                System.err.println( "Console event thread interrupted." );
            }
            finally
            {
                // stops the bot
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
     * A do-nothing init method. It does nothing so that it can be overwritten by subclasses when necessary
     */
    public void init() throws Exception {}

    /**
     * A do-nothing stop method. It does nothing so that it can be overwritten by subclasses when necessary.
     */
    public void stop() {}

    /**
     * Used to fetch the bot application's command line arguments. It simply returns a list that was passed in as
     * arguments.
     *
     * @return The string array of arguments passed in at launch as a List of Strings.
     */
    List< String > getArguments()
    {
        return Arrays.asList(this.arguments );
    }


    /**
     * A private class that keeps the bot running while the application should be running
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
         * The run method used by the thread for the bot.
         */
        public void run()
        {
            // scanner can close, PrintWriter needs to remain open
            PrintWriter out;
            try ( Scanner consoleIn = new Scanner( System.in ) ) {
                try {
                    out = new PrintWriter(
                            new OutputStreamWriter( System.out ), true );
                    // starts the bot (indication that it is go time)
                    bot.start( consoleIn, out );
                    //out = null;
                }
                catch( Exception e )
                {
                    // print a stack trace if anything fatal occurs
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * The method that begins the start procedure of the bot. consoleIn is required so that different sorts of commands
     * can be run. consoleOut is used to print to standard out.
     *
     * @param consoleIn  the source of the user input
     * @param consoleOut the destination where text output should be printed
     */
    public abstract void start(Scanner consoleIn, PrintWriter consoleOut) throws Exception;
}
