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

    private PlaceServer server;

    private NetworkServer networkServer;

    private String username;

    // if the connection is alive or not (used to disconnect)
    private boolean alive = false;

    public PlaceClientThread(Socket player, PlaceServer server, NetworkServer networkServer) throws PlaceException
    {
        try
        {
            // sets the ObjectInputStream
            this.in = new ObjectInputStream(player.getInputStream());
            // sets the ObjectOutputStream
            this.out = new ObjectOutputStream(player.getOutputStream());
            // sets the server
            this.server = server;
            // sets the networkServer
            this.networkServer = networkServer;

            this.alive = true;
        }
        // if we catch these exceptions
        catch(IOException e)
        {
            // we throw them out to server
            throw new PlaceException(e);
        }
    }

    // probably won't be needed but I wrote it just in case
    String getUsername()
    {
        return this.username;
    }

    /**
     * An override of the run method which runs the thread
     */
    @Override
    public void run()
    {
        // while the connection is still alive
        while(this.alive)
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
                        // so we don't make a mistake later on
                        if(username == null)
                        {
                            // set our username
                            this.username = (String) request.getData();
                            // attempts to login here
                            networkServer.login(this.username, this.out);
                        }
                        break;
                    case CHANGE_TILE:
                        // lets the networkServer know of a new tile change request
                        networkServer.tileChangeRequest( (PlaceTile) request.getData() );
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
            catch(IOException | ClassNotFoundException e)
            {
                // skip this read if there is an IOException or ClassNotFoundException
                // might have to be a termination here if we run into it
            }
        }
        // we have now exited the loop which means the user will be disconnecting now
        // we can close the Output and Input streams.
        close();
    }

    private void badRequest(String type) throws IOException
    {
        // alerts the user they sent a bad request as well as the type (if somehow we get here they are being naughty
        // and using a custom client.
        // please don't be that person
        out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Bad request received: " + type + ". Terminating connection."));
        // flushes the stream so it sends
        out.flush();
        // logs user out from the server
        networkServer.logout(this.username);
        // terminate thread
        this.alive = false;
    }

    private void close()
    {
        try
        {
            this.in.close();
            this.out.close();
        }
        catch(IOException e)
        {
            // this shouldn't ever happen.
        }
    }
}
