package place.server;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

import place.PlaceException;
import place.network.NetworkServer;
import place.network.PlaceRequest;

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

    // probably won't be needed but I write it just in case
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
        // while the connection is alive
        while(this.alive)
        {
            try
            {
                // reads in a request from the user (blocks until it reads in)
                PlaceRequest<?> request = ( PlaceRequest<?> ) in.readObject();

                switch(request.getType())
                {
                    // might have to rework this so that we set username in the constructor instead of here (might save
                    // time in terms of having to check for this every time)
                    case LOGIN:
                        // we should only receive this once, so we make sure username is still null (as it is in at start)
                        if(username == null)
                        {
                            // set our username
                            this.username = (String) request.getData();
                            // attempts to login here
                            networkServer.login(this.username, this.out);
                        }
                        break;
                    case CHANGE_TILE:
                        break;
                    case BOARD:
                        // we shouldn't every receive a board from player...
                        out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Please don't send boards."));
                        out.flush();
                        break;
                    case ERROR:
                        break;
                    case TILE_CHANGED:
                        break;
                    case LOGIN_SUCCESS:
                        break;
                    default:
                        // if we get send and error
                }
            }
            catch(IOException | ClassNotFoundException e)
            {
                // skip this read if there is an IOException or ClassNotFoundException
            }
        }
    }

}
