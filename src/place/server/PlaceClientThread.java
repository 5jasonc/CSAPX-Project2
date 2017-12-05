package place.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

import place.PlaceException;
import place.PlaceTile;

import place.network.NetworkServer;
import place.network.PlaceRequest;
import place.network.PlaceRequest.RequestType;

/**
 * The PlaceClientThread is the server-sided class that listens to the client's input and relays it to the NetworkServer.
 *
 * @author Kevin Becker (kjb2503)
 */
public class PlaceClientThread
{
    /**
     * The ObjectInputStream from the client (reads the requests that the client sends to server).
     */
    private ObjectInputStream in;
    /**
     * The ObjectOutputStream from the client (sends the requests that the server sends to client).
     */
    private ObjectOutputStream out;

    /**
     * A link to the NetworkServer.
     */
    private NetworkServer networkServer;

    /**
     * The String that is our username.
     */
    private String username;

    /**
     * The indicator to the thread whether it should keep running or not.
     *
     * If the thread should continue running this is true; false otherwise.
     */
    private boolean go;

    /**
     * Getter that is used by run to tell if it should keep going.
     *
     * @return A boolean. True if this.go is set to true; false otherwise.
     */
    private synchronized boolean go()
    {
        return this.go;
    }
    /**
     * Setter that is used to stop the thread in the event of need to stop.
     */
    private void stop()
    {
        this.go = false;
    }


    /**
     * Constructs a new thread for a player once they connect to the server.
     *
     * @param player The player socket.
     * @param networkServer The NetworkServer so we can communicate with it.
     *
     * @throws PlaceException If there is an issue creating the thread
     */
    PlaceClientThread(Socket player, NetworkServer networkServer) throws PlaceException
    {
        try
        {
            // sets the ObjectOutputStream (need to do OUTPUT before we can do INPUT)
            this.out = new ObjectOutputStream(player.getOutputStream());
            // sets the ObjectInputStream
            this.in = new ObjectInputStream( player.getInputStream() );
            // sets the networkServer
            this.networkServer = networkServer;
            // sets go to true so we can begin
            this.go = true;
        }
        // if we catch these exceptions
        catch(IOException e)
        {
            // we throw them out to server
            throw new PlaceException(e);
        }
    }

    /**
     * Starts a new thread used for the user using the run method of the class.
     */
    public void start()
    {
        // creates a new thread using run
        new Thread(this::run).start();
    }

    /**
     * Runs the PlaceClientThread.
     */
    private void run()
    {
        // while the connection is still alive
        while(this.go())
        {
            try
            {
                // will eventually put this into PlaceExchange in network package (for code re-usage)
                // reads in a request from the user (blocks until it reads in)
                PlaceRequest<?> request = ( PlaceRequest<?> ) in.readObject();

                switch(request.getType())
                {
                    // might have to rework this so that we set username in the constructor instead of here (might save
                    // time in terms of having to check for this every time)
                    case LOGIN:
                        // we should only receive this once, so we make sure username is still null (as it is in at start)
                        // so we don't make a mistake later on.
                        if(username == null)
                        {
                            // set our username
                            String usernameRequest = (String) request.getData();

                            // attempts to login here
                            if(login(usernameRequest))
                                this.username = usernameRequest;
                        }
                        break;
                    case CHANGE_TILE:
                        PlaceTile tile = (PlaceTile) request.getData();
                        // if the move requested is valid, we make the move, otherwise we have to quit
                        if(tileChangeRequest(tile))
                        {
                            System.out.println("[Server]: " + this.username + " has requested to change a tile.");
                            System.out.println("\tNew tile: " + tile);
                        }
                        // otherwise we need to quit before it's too late
                        else
                        {
                            System.err.println("[Server]: " + this.username + " has requested to change a tile that doesn't exist.");
                            System.err.println("\t Terminating their connection.");
                            badRequest("Tile not valid.");
                        }
                        break;
                    // we shouldn't ever receive these from the player... they are bad requests
                    case BOARD:
                        badRequest(RequestType.BOARD.toString());
                        break;
                    case ERROR:
                        badRequest(RequestType.ERROR.toString());
                        break;
                    case TILE_CHANGED:
                        badRequest(RequestType.TILE_CHANGED.toString());
                        break;
                    case LOGIN_SUCCESS:
                        badRequest(RequestType.LOGIN_SUCCESS.toString());
                        break;
                    default:
                        // if we get an unknown request send an error reporting it
                        badRequest("UNKNOWN");
                }
            }
            catch(ClassNotFoundException e)
            {
                // move to the next read and hopefully it wasn't important
            }
            catch(IOException e)
            {
                // if we hit a IOException one of the connections closed we are assuming disconnection
                this.stop();
            }
        }
        // we have now exited the loop which means the user will be disconnecting now
        // we can close the Output and Input streams.
        this.close();
    }

    /**
     * Requests the NetworkServer to log us in.
     *
     * @param usernameRequest The username that we want to have.
     *
     * @return A boolean. True if login was successful; false otherwise.
     */
    private boolean login(String usernameRequest)
    {
        // attempts to login to the server
        return networkServer.login(usernameRequest, this.out);
    }

    /**
     * If we receive a bad request from a client, we send a similar message for each of those, which we handle here.
     *
     * @param type The type of error that is run into for alerting user.
     *
     * @throws IOException If somehow we manage to get an IOException.
     */
    private void badRequest(String type) throws IOException
    {
        // alert the user that they have sent us a bad request and that their connection is being terminated
        this.networkServer.badRequest(this.username, type);

        // terminate thread
        this.stop();
    }

    /**
     * Requests the NetworkServer change the tile that user wants to change.
     *
     * @param tile The PlaceTile that is being requested to change.
     */
    private boolean tileChangeRequest(PlaceTile tile)
    {
        // tells the networkServer we want to change a tile
        return this.networkServer.tileChangeRequest(tile);
    }


    /**
     * Closes the connections so that we can gracefully shut down.
     */
    private void close()
    {
        try
        {
            // logs user out from the server before closing connections if they were allowed logged in
            if(this.username != null)
                this.networkServer.logout(this.username);
            // closes the in and out connections
            this.in.close();
            this.out.close();
        }
        catch(IOException e)
        {
            // this shouldn't ever happen.
        }
    }
}
