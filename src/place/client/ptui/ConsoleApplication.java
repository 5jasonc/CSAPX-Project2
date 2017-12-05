package place.client.ptui;

import place.PlaceException;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * A class to do console-based user interaction in a manner similar to
 * how JavaFX does window-based interaction.
 * This class is to be inherited by any console application.
 *
 * @author James Heliotis
 * Edits made by Jason Streeter (jcs1738)
 */
public abstract class ConsoleApplication {

    private String[] cmdLineArgs;

    private Thread eventThread;

    /**
     * Run a console application.
     * @param ptuiClass the class object that refers to the class to
     *             be instantiated
     */
    public static void launch(
            Class< ? extends ConsoleApplication > ptuiClass
    ) {
        launch( ptuiClass, new String[ 0 ] );
    }

    /**
     * Run a console application, with command line arguments.
     * @param ptuiClass the class object that refers to the class to
     *             be instantiated
     * @param args the array of strings from the command line
     */
    public static void launch(
            Class< ? extends ConsoleApplication > ptuiClass,
            String[] args
    ) {
        try {
            ConsoleApplication ptuiApp = ptuiClass.newInstance();
            ptuiApp.cmdLineArgs = Arrays.copyOf( args, args.length );

            try {
                ptuiApp.init();
                ptuiApp.eventThread = new Thread( new Runner( ptuiApp ) );
                ptuiApp.eventThread.start();
                ptuiApp.eventThread.join();
            }
            catch( Exception ie ) {
                System.err.println( "Console event thread interrupted" );
            }
            finally {
                ptuiApp.stop();
            }
        }
        catch( InstantiationException ie ) {
            System.err.println( "Can't instantiate Console App:" );
            System.err.println( ie.getMessage() );
        }
        catch( IllegalAccessException iae ) {
            System.err.println( iae.getMessage() );
        }
    }

    private static class Runner implements Runnable {
        private final ConsoleApplication ptuiApp;

        public Runner( ConsoleApplication ptuiApp ) { this.ptuiApp = ptuiApp; }

        public void run() {
            try ( Scanner in = new Scanner( System.in ) ) {
                try {
                    ptuiApp.go( in );
                }
                catch( Exception e ) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Fetch the application's command line arguments
     * @return the string array that was passed to launch, if any, or else
     *         an empty array
     */
    public List< String > getArguments() {
        return Arrays.asList( this.cmdLineArgs );
    }

    /**
     * A do-nothing setup method that can be overwritten by subclasses
     * when necessary
     */
    public void init() throws Exception {}

    /**
     * The method that is expected to run the main loop of the console
     * application, prompting the user for text input and displaying
     * text output. It is named differently than
     * {@link javafx.application.Application#start(javafx.stage.Stage)}
     * to emphasize that this method can keep executing (looping,
     * probably) as long as the application is running.
     */
    public abstract void go( Scanner in ) throws Exception;

    /**
     * A do-nothing teardown method that can be overwritten by subclasses
     * when necessary.
     */
    public void stop() {}

}

