package place.server;

import java.io.Closeable;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;

import place.PlaceException;
import place.network.NetworkServer;

/**
 * A PlaceServer is a location that PlaceClients can connect and create cool tile drawings.
 *
 * This class implements closeable so its "close()" method is called automatically upon exit.
 *
 * @author Kevin Becker (kjb2503)
 * @author Jason Streeter (jcs1738)
 */
public class PlaceServer implements Closeable, AutoCloseable {

    /**
     * A simple formatter which formats the current time to appear as a nice, easy to read format.
     */
    private final static SimpleDateFormat TIME_STAMP_FORMAT = new SimpleDateFormat("MM-dd-yyyy HH.mm.ss");

    /**
     * The ServerSocket which is used to connect to clients.
     */
    private ServerSocket server;

    /**
     * The NetworkServer which is the brains of the communication operation.
     */
    private NetworkServer networkServer;

    /**
     * The boolean which tells the listener thread if it should keep listening.
     */
    private boolean go;

    /**
     * This is used by the thread to make sure it should keep going.
     *
     * Synchronized so that we only allow running it one thread at a time.
     *
     * @return true if this.go is set to true; false otherwise.
     */
    private synchronized boolean go()
    {
        return this.go;
    }

    /**
     * Constructs a new PlaceServer which is used to accept connections from Place clients.
     *
     * NOTE: access is private because we only want the main method to be able to create an instance of it.
     *
     * @param port the port that is requested to run on.
     * @param dim the square dimension of the Place board.
     *
     * @throws PlaceException if any sort of exception is run into it is wrapped in a PlaceException.
     */
    private PlaceServer(int port, int dim) throws PlaceException
    {
        try
        {

            // makes our directory if we need to (mostly used if user deletes, or first-run)
            File directory = new File("logs");
            // checks if we need to create our directory
            if(!directory.exists())
                directory.mkdir();


            // gets our current timestamp
            String ts = TIME_STAMP_FORMAT.format(new Date(System.currentTimeMillis()));

            // creates a log with a name: log-(timestamp).txt
            File logLocation = new File("logs/" + ts + ".log");

            // creates the log if it doesn't exist already (why should it??)
            if(!logLocation.exists())
                logLocation.createNewFile();

            // creates a new buffered writer that will write to the log
            // set to append just in case it already exists (it shouldn't)
            PrintWriter log = new PrintWriter(new FileWriter(logLocation, true), true);

            // writes initial header to the log
            log.println("============================= PLACE SERVER LOG =============================");
            log.println("= Log generation started: " + ts);
            log.println("= Beginning startup sequence...");

            // makes a new server socket broadcasting on port
            log.print("= Opening PlaceServer on port " + port + "...");
            this.server = new ServerSocket(port);
            log.println("success.");

            // makes a new NetworkServer (the major brains of the program)
            log.print("= Building main communications...");
            this.networkServer = new NetworkServer(dim, log);
            log.println("success.");

            log.println("= Startup sequence complete.");
            log.println("=============================================================================");
        }
        catch(Exception e)
        {
            // throws an exception if any sort of issue is run into
            throw new PlaceException(e);
        }

        // sets go to true so we know beginning is happening
        this.go = true;

        // say this to output once we've set everything up.
        this.networkServer.serverStarted(port);
    }

    /**
     * Runs the server which essentially just accepts connections and spawns PlaceClientThreads until it is shut down.
     *
     * @throws PlaceException if any sort of exception is run into, it is wrapped in a PlaceException.
     */
    private void run() throws PlaceException
    {
        // while we are still good to go
        while(this.go())
        {
            try
            {
                // gather a new client and start it
                new PlaceClientThread(server.accept(), this.networkServer).start();
            }
            catch(Exception e)
            {
                this.networkServer.serverError();
                // throws the error as  PlaceException
                throw new PlaceException(e);
            }
        }
    }

    /**
     * Closes the ServerSocket so that no more connections can be created.
     */
    public void close()
    {
        try
        {
            // closes the ServerSocket (we're done with it if we're closing)
            this.server.close();
        }
        catch(IOException ioe)
        {
            // if this happens... well. :)
        }
    }

    /**
     * The main method which is used to create a PlaceServer.
     *
     * @param args The arguments of the main method. The arguments should have: [port, dimension].
     */
    public static void main(String[] args)
    {
        // check to make sure we've been given the right NUMBER of
        if(args.length != 2)
        {
            // alert the user they have given us bad stuff
            System.err.println("Please run the server as:");
            System.err.println("$ java PlaceGUI host port username");
            return;
        }

        // the port of the server, could fool-proof this and check to make sure this is valid port
        int port = Integer.parseInt(args[0]);
        // the dimension of the board
        int dim = Integer.parseInt(args[1]);

        // tries to make a new server object running on the port
        try ( PlaceServer server = new PlaceServer(port, dim) )
        {
            // if we've made it this far, we can begin running the server
            server.run();
        }
        catch (PlaceException e)
        {
            // tells that the server hit something totally unrecoverable
            System.err.println("We've hit an unrecoverable issue. Please try to launch again.");
            System.err.println( e.getMessage() );
        }
    }
}
