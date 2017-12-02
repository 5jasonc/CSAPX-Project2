package place.network;

import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;
import place.PlaceBoardObservable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

public class NetworkClient {

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
     * Used by the thread to make sure it should keep going.
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
     * This is used to stop the thread from running.
     *
     * Synchronized so that we only allow running it one thread at a time.
     */
    private synchronized void stop()
    {
        this.go = false;
    }

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
    public NetworkClient(String host, int port, String username, PlaceBoardObservable board) throws PlaceException
    {
        // BEGIN SETTING UP NEW NetworkClient
        try
        {
            // CONNECTION BUILDING SEQUENCE ================================
            // connects to the server
            this.serverConn = new Socket(host, port);
            // set our board to the one we were passed
            this.board = board;


            // COMMUNICATION BUILDING SEQUENCE ================================
            // sets the in and out streams
            this.in = new ObjectInputStream( serverConn.getInputStream() );
            this.out = new ObjectOutputStream( serverConn.getOutputStream() );
            // flushes out just in case
            out.flush();


            // LOG IN SEQUENCE ================================
            // write our login request with our username
            out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username));
            // wait for response from server to determine if we should continue starting or not
            PlaceRequest<?> response = (PlaceRequest<?>) in.readObject();
            // go through each case to determine what the response was
            // LOGIN_SUCCESS or ERROR (or unknown case)
            switch (response.getType())
            {
                case LOGIN_SUCCESS:
                    System.out.println("Successfully joined Place server as \"" + response.getData() + "\".");
                    this.go = true;
                    break;
                case ERROR:
                    System.err.println("Failed to join Place server. Server response: " + response.getData() + ".");
                    this.close();
                    throw new PlaceException("Unable to join.");
                default:
                    System.err.println("Bad response received from server.");
                    this.close();
                    throw new PlaceException("Unable to join.");
            }


            // BOARD READ-IN SEQUENCE ===============================
            // read in the object (should be a board)
            PlaceRequest<?> boardResponse = ( PlaceRequest<?> ) in.readObject();

            // check to make sure what we just read in was in fact a board
            if(boardResponse.getType() == PlaceRequest.RequestType.BOARD)
                // initialize our board with the once we've been given
                this.board.initializeBoard( (PlaceBoard) boardResponse.getData() );
            // if we weren't sent a board, we were given something bad, we need to escape now.
            else
            {
                this.go = false;
                this.close();
            }
            new Thread( () -> this.run() ).start();
        }
        catch(IOException | ClassNotFoundException e)
        {
            throw new PlaceException(e);
        }
    }

    /**
     * The thread which continually loops listening to the server so that proper status of the board can be kept.
     */
    private void run()
    {
        // this.go() is synchronized so we only do this one at a time
        while(this.go())
        {
            try
            {
                PlaceRequest<?> request = ( PlaceRequest<?> ) this.in.readObject();

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
                // stop the client because we've hit an unrecoverable issue
                this.stop();
            }
        }
        this.close();
    }

    /**
     * Used to send a tile to the server if the user has requested to change a tile.
     *
     * @param tile The tile that is being requested to change. (It contains all the necessary information).
     */
    public void sendTile(PlaceTile tile)
    {
        try
        {
            this.out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile));
        }
        catch (IOException e)
        {
            // we can't do anything with this so...
        }
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
        System.err.println("Server responded with error message: \"" + error + "\"");
        this.stop();
    }

    /**
     * Tell the user we have received a bad response so they know they have disconnected.
     */
    private void badResponse()
    {
        System.err.println("Bad response received from server. Terminating connection.");
        this.stop();
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
            // heck!
        }
    }

}
