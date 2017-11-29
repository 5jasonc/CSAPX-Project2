package place.server;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

import place.PlaceException;
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

            // reads in the first request from user
            PlaceRequest<?> request = ( PlaceRequest<?> ) in.readObject();

            // if the request is a LOGIN, set the username to the data of the request
            if (request.getType() == PlaceRequest.RequestType.LOGIN)
                this.username = (String) request.getData();
            // otherwise we've hit an oops and we need to tell the user to exit (they sent a bad request)
            else
            {
                // write an error to the buffer saying login failed
                out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Login failed."));
                // flush buffer to user
                out.flush();
            }
        }
        // if we catch these exceptions
        catch(IOException | ClassNotFoundException e)
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
        // first we must login using our USERNAME and our OUTPUT
        if(this.networkServer.login(this.username, this.out))
            this.alive = true;

        // while the connection is alive
        while(this.alive)
        {
            try
            {
                // reads in a request from the user (blocks until it reads in)
                PlaceRequest<?> request = ( PlaceRequest<?> ) in.readObject();

                switch(request.getType())
                {
                    case BOARD:
                        // we shouldn't every receive a board from player...
                        out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Please don't send boards."));
                        out.flush();
                        break;
                    case LOGIN:
                        // we shouldn't ever receive a login request from player at this point.
                        break;
                    case ERROR:
                        break;
                    case TILE_CHANGED:
                        break;
                    case CHANGE_TILE:
                        break;
                    case LOGIN_SUCCESS:
                        break;
                }
            }
            catch(IOException | ClassNotFoundException e)
            {
                // skip this read if there is an IOException or ClassNotFoundException
            }
        }
    }

}
