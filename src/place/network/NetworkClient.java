package place.network;

import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;
import place.PlaceBoardObservable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

import static java.lang.Thread.sleep;

// fully commented

/**
 * A network middle-man for a Place client.
 *
 * @author Kevin Becker (kjb2503)
 */
public class NetworkClient {

    /**
     * The number of milliseconds a user must wait before they can send their next tile.
     */
    private final static int COOLDOWN_TIME = 500;

    /**
     * Our observable PlaceBoard wrapper.
     */
    private PlaceBoardObservable board;

    /**
     * The connection to the server.
     */
    private Socket serverConn;

    /**
     * The ObjectInputStream that is used to read from the server.
     */
    private ObjectInputStream in;

    /**
     * The ObjectOutputStream taht is used to communicate to the server.
     */
    private ObjectOutputStream out;

    /**
     * A "header" of sorts that is printed before every log.
     */
    private String logHeader;

    /**
     * A boolean that indicates if this client is cooling down after placing a tile.
     *
     * If it is, the client cannot send a new piece. If it is true, and a client tries to send a PlaceTile,
     * it displays an error.
     */
    private boolean coolDown;

    /**
     * The indicator to the thread whether it should keep running or not.
     *
     * If the thread should continue running this is true; false otherwise.
     */
    private boolean go;

    /**
     * This is used by the thread to make sure it should keep going.
     *
     * Synchronized so that we only allow running it one thread at a time.
     *
     * @return true if this.go is set to true; false otherwise.
     */
    private synchronized boolean go() { return this.go; }

    /**
     * This is used to stop the thread from running.
     *
     * Synchronized so that we only allow running it one thread at a time.
     */
    private void stop() { this.go = false; }

    /**
     * Constructor for the NetworkClient class.
     *
     * @param host The host String that we should connect to.
     * @param port The port int to connect to.
     * @param username The desired username. (WARNING: this could cause a problem if you request an already taken
     *                 username.
     * @param board The PlaceBoardObservable that will be used to connect the client UI to the server allowing for
     *              update calls.
     *
     * @throws PlaceException If there is any exception thrown during the connect process that prevents successful
     *                        usage of the server.
     */
    public NetworkClient(String host, int port, String username, String className, PlaceBoardObservable board) throws PlaceException
    {
        // BEGIN SETTING UP NEW NetworkClient
        try
        {
            // SETTING CLASS NAME (for log) ================================
            this.logHeader = "[" + className + "]: ";
            // CONNECTION BUILDING SEQUENCE ================================
            // connects to the server
            this.serverConn = new Socket(host, port);
            // set our board to the one we were passed
            this.board = board;


            // COMMUNICATION BUILDING SEQUENCE ================================
            // sets the in and out streams
            this.out = new ObjectOutputStream( serverConn.getOutputStream() );
            // flushes out just in case
            out.flush();
            this.in = new ObjectInputStream( serverConn.getInputStream() );


            // LOG IN SEQUENCE ================================
            // write our login request with our username
            out.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username));
            // wait for response from server to determine if we should continue starting or not
            PlaceRequest<?> response = (PlaceRequest<?>) in.readUnshared();
            // go through each case to determine what the response was
            // LOGIN_SUCCESS or ERROR (or unknown case)
            switch (response.getType())
            {
                case LOGIN_SUCCESS:
                    log("Successfully joined Place server as \"" + response.getData() + "\".");
                    break;
                case ERROR:
                    logErr("Failed to join Place server. Server response: " + response.getData() + ".");
                    this.close();
                    throw new PlaceException("Unable to join.");
                default:
                    logErr("Bad response received from server.");
                    this.close();
                    throw new PlaceException("Unable to join.");
            }


            // BOARD READ-IN SEQUENCE ===============================
            // read in the object (should be a board)
            PlaceRequest<?> boardResponse = ( PlaceRequest<?> ) in.readUnshared();

            // check to make sure what we just read in was in fact a board
            if(boardResponse.getType() == PlaceRequest.RequestType.BOARD)
                // initialize our board with the once we've been given
                this.board.initializeBoard( (PlaceBoard) boardResponse.getData() );
            // if we weren't sent a board, we were given something bad, we need to escape now.
            else
                throw new PlaceException("Board class never sent.");
            // sets go to be true
            this.go = true;
        }
        catch(IOException | ClassNotFoundException e)
        {
            throw new PlaceException(e);
        }
        // NetworkClient SETUP COMPLETE ===============================
    }

    /**
     * This start method is so that handling tile change requests in the client work properly.
     *
     * Scenario:
     * When a client is starting to build everything, if another user makes a request during that very minimal time,
     * the NetworkClient class won't alert the PlaceBoardObservable until AFTER the thread is begun.
     */
    public void start()
    {
        // starts the listener thread because we've built everything in client and need to move forward
        new Thread(this::run).start();
    }

    /**
     * The thread which continually loops listening to the server so that proper status of the board can be kept.
     */
    private void run()
    {
        // this.go() is synchronized so we only do this one at a time
        while(this.go())
        {
            // try to read the next request
            try
            {
                // reads the next request from the buffer
                PlaceRequest<?> request = ( PlaceRequest<?> ) this.in.readUnshared();
                // determines which type of request was given
                switch(request.getType())
                {
                    case TILE_CHANGED:
                        tileChanged( (PlaceTile) request.getData() );
                        break;
                    case ERROR:
                        error( (String) request.getData() );
                        break;
                    // should not ever get these, if we get here we have to stop our client
                    case BOARD:
                        badResponse();
                        break;
                    case LOGIN:
                        badResponse();
                        break;
                    case LOGIN_SUCCESS:
                        badResponse();
                        break;
                    case CHANGE_TILE:
                        badResponse();
                        break;
                    default:
                        badResponse();
                }
            }
            catch(IOException | ClassNotFoundException e)
            {
                disconnected();

                // stop the client because we've hit an unrecoverable issue
                this.stop();
            }
        }
        // closes everything because we've left the thread loop and that means we're over
        this.close();
    }

    /**
     * Used to send a tile to the server if the user has requested to change a tile.
     *
     * @param tile The tile that is being requested to change. (It contains all the necessary information).
     */
    public synchronized void sendTile(PlaceTile tile)
    {
        if(!this.coolDown)
        {
            // writes the tile to the server
            try
            {
                // write the tile to the output buffer
                this.out.writeUnshared(new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile));
                // flushes the object written out
                out.flush();
            }
            catch(IOException e)
            {
                // do nothing
            }
            // spawns a thread that exists for 500ms which forces the user to wait 500ms from one send to the next
            new Thread(this::coolDown).start();
        }
        else
        {
            logErr("You must wait half a second between each tile place");
        }
    }

    /**
     * A small sleeper thread class which makes it so a user cannot send any PlaceTile for 500ms. (A cool-down)
     */
    private void coolDown()
    {
        // sleeps
        this.coolDown = true;
        // sleeps for 500ms
        try
        {
            Thread.sleep(COOLDOWN_TIME);
        }
        catch(InterruptedException e)
        {
            /* do nothing */
        }
        // stops coolDown
        this.coolDown = false;
    }

    /**
     * If a tile is changed (an item sent by the server) we note that here.
     *
     * @param tile The tile that has been changed.
     */
    private void tileChanged(PlaceTile tile)
    {
        // update the model to reflect the new tile change (so it can alert users)
        this.board.tileChanged(tile);
    }

    /**
     * Tell the user we've hit an error meaning the client will quit.
     *
     * @param error The error message the server sent.
     */
    private void error(String error)
    {
        logErr("Server responded with error message: \"" + error + "\"");
        this.stop();
    }

    /**
     * Tell the user we have received a bad response so they know they have disconnected.
     */
    private void badResponse()
    {
        logErr("Bad response received from server. Terminating connection.");
        this.stop();
    }

    private void disconnected()
    {
        logErr("Lost connection to server.");
    }

    /**
     * Logs a non-error message to standard output.
     *
     * @param msg The message to be printed out.
     */
    public void log(String msg)
    {
        System.out.println(logHeader + msg);
    }

    /**
     * Logs an error message to standard output.
     *
     * @param msg The message to be printed out.
     */
    public void logErr(String msg)
    {
        System.err.println(logHeader + msg);
    }

    /**
     * A close method that clears out everything so we can close without fear of any bad stuff going on.
     */
    public void close()
    {
        try
        {
            // try to close our server connection and Object streams
            this.serverConn.close();
            this.in.close();
            this.out.close();
        }
        catch (IOException e)
        {
            // oops
        }
    }

}
