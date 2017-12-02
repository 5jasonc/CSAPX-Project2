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

public class PlaceClientThread extends Thread
{
    private ObjectInputStream in;
    private ObjectOutputStream out;

    /**
     * A link to the NetworkServer so that
     */
    private NetworkServer networkServer;

    /**
     * The String that is our username
     */
    private String username;

    // if the connection is alive or not (used to disconnect)
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
     * Constructs a new thread for a player once they connect to the server.
     *
     * @param player The player socket.
     * @param networkServer The NetworkServer so we can communicate with it.
     * @throws PlaceException
     */
    public PlaceClientThread(Socket player, NetworkServer networkServer) throws PlaceException
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
     * An override of the run method which runs the thread.
     */
    @Override
    public void run()
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
                            login(usernameRequest);
                        }
                        break;
                    case CHANGE_TILE:
                        // lets the networkServer know of a new tile change request
                        tileChangeRequest( (PlaceTile) request.getData() );
                        break;
                    // we shouldn't ever receive these from the player...
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
                // skip this read if there is a ClassNotFoundException
                // might have to be a termination here if we run into it
            }
            catch(IOException e)
            {
                // if we hit a IOException one of the connections closed we are assuming disconnection
                this.go = false;
            }
        }
        // we have now exited the loop which means the user will be disconnecting now
        // we can close the Output and Input streams.
        this.close();
    }

    /**
     * ****** MOVE CONTENTS TO NetworkServer ******
     * If we receive a bad request from a client, we send a similar message for each of those, which we handle here.
     *
     * @param type the type of error that is run into for alerting user.
     *
     * @throws IOException if somehow we manage to get an IOException.
     */
    private void badRequest(String type) throws IOException
    {
        // alerts the user they sent a bad request as well as the type (if somehow we get here they are being naughty
        // and using a custom client.)
        // please don't be that person
        out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Bad request received: " + type + ". Terminating connection."));

        // prints to the server log that user has sent a bad request
        System.err.println("Bad request received from " + this.username + ". REQUEST: " + type);


        // flushes the stream so it sends
        out.flush();
        // terminate thread
        this.go = false;
    }

    private void login(String usernameRequest)
    {
        // attempts to login to the server
        if(networkServer.login(usernameRequest, this.out))
            this.username = usernameRequest;
    }

    private void tileChangeRequest(PlaceTile tile)
    {
        // tells the networkServer we want to change a tile
        networkServer.tileChangeRequest(tile);
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
                networkServer.logout(this.username);
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
