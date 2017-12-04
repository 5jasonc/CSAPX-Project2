package place.client.bot;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public abstract class BotApplication {

    private String[] arguments;

    private Thread eventThread;

    /**
     * Run a bot application.
     * Launch a standalone application. This method is typically called from the main method().
     * It must not be called more than once or an exception will be thrown. This is equivalent to launch(TheClass.class, args) where TheClass is the immediately enclosing class of the method that called launch. It must be a subclass of Application or a RuntimeException will be thrown.
     * The launch method does not return until the application has exited, either via a call to Platform.exit or all of the application windows have been closed.
     * Typical usage is:
     * public static void main(String[] args)
     * {
     *      BotApplication.launch(  );
     * }
     *
     * @param botClass the class object that refers to the class to be instantiated.
     */
    public static void launch(Class< ? extends BotApplication> botClass)
    {
        launch(botClass, new String[ 0 ]);
    }

    /**
     * Run a complete bot application, with command line arguments.
     * @param botClass the class object that refers to the class to
     *             be instantiated
     * @param args the array of strings from the command line
     */
    public static void launch(Class< ? extends BotApplication > botClass, String[] args) {
        try
        {
            BotApplication bot = botClass.newInstance();
            bot.arguments = Arrays.copyOf( args, args.length );

            try
            {
                bot.init();
                bot.eventThread = new Thread( new BotRunner( bot ) );
                bot.eventThread.start();
                bot.eventThread.join();
            }
            catch( Exception ie ) {
                System.err.println( "Console event thread interrupted." );
            }
            finally {
                bot.stop();
            }
        }
        catch( InstantiationException ie ) {
            System.err.println("Can't instantiate Bot:");
            System.err.println( ie.getMessage() );
        }
        catch( IllegalAccessException iae ) {
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
    public List< String > getArguments()
    {
        return Arrays.asList(this.arguments );
    }


    private static class BotRunner implements Runnable {
        private final BotApplication bot;

        public BotRunner( BotApplication bot )
        {
            this.bot = bot;
        }

        public void run()
        {
            // We don't put the PrintWriter in try-with-resources because
            // we don't want it to be closed. The Scanner can close.
            PrintWriter out;
            try ( Scanner consoleIn = new Scanner( System.in ) ) {
                try {
                    out = new PrintWriter(
                            new OutputStreamWriter( System.out ), true );
                    bot.start( consoleIn, out );
                    //out = null;
                }
                catch( Exception e ) {
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
