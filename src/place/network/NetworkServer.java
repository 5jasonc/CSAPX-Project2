package place.network;

import place.PlaceTile;
import place.PlaceBoardObservable;
import place.network.PlaceRequest.RequestType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class NetworkServer
{
    //          uname,  output to user
    private Map<String, ObjectOutputStream> users;
    private PlaceBoardObservable model;


    /**
     * Constructs a new NetworkServer used to communicate to Place clients.
     *
     * @param dim the dimension of the board once it is set up.
     */
    public NetworkServer(int dim)
    {
        this.users = new HashMap<>();
        this.model = new PlaceBoardObservable(dim);
    }

    /**
     * Logs in a user. This is synchronized so we don't run into people requesting the same username at the same time.
     *
     * @param usernameRequest The requested username from a user.
     * @param playerOutputStream The output stream for the user.
     */
    public synchronized boolean login(String usernameRequest, ObjectOutputStream playerOutputStream)
    {
        // checks if the username is taken
        // if it's not, log ourselves in and return true so user can update
        try
        {
            // checks if our username is valid or not (a.k.a. is there a key with our username?)
            if(!usernameTaken(usernameRequest))
            {
                // put ourselves in the HashMap
                users.put(usernameRequest, playerOutputStream);

                // alert that user has connected
                System.out.println("[NetworkServer]: " + usernameRequest + " has joined the server.");

                // tell the user they were logged in successfully
                playerOutputStream.writeObject(new PlaceRequest<>(RequestType.LOGIN_SUCCESS, usernameRequest));
                // sends the board as well since we have connected and we need to build our board
                playerOutputStream.writeObject(new PlaceRequest<>(RequestType.BOARD, this.model.getPlaceBoard()));
                return true;
            }
            else
            {
                // tell the user the username is taken
                playerOutputStream.writeObject(new PlaceRequest<>(RequestType.ERROR, "Username taken."));
            }
            // write our result out (doesn't matter which because either way we need to send something)
            playerOutputStream.flush();
        }
        catch(IOException e)
        {
            // heck what do we do here??
        }
        return false;
    }

    /**
     * Checks to see if a requested username is valid from a player
     *
     * @param usernameRequest the requested username from the user which we are checking for.
     *
     * @return a boolean; true if the username is already taken, false otherwise.
     */
    private boolean usernameTaken(String usernameRequest)
    {
        // return whether or not the HashMap contains our key
        return users.containsKey(usernameRequest);
    }

    /**
     * Logs a user out. Doesn't need to be synchronized since we won't have multiple people logged in with the same
     * username trying to log out.
     *
     * @param username The username of the user wishing to log out.
     */
    public void logout(String username)
    {
        // logs a user out (essentially logs just removes them from the map)
        // since the ObjectOutputStream is just a pointer, this is all we have to do
        users.remove(username);
        // alert that user has disconnected
        System.out.println("[NetworkServer]: " + username + " has left the server.");
    }

    /**
     * Alerts all of the users who are logged in that a new tile change request has occurred. It is synchronized so that
     * if multiple users send a move for the same tile at the same time we don't have mismatched boards.
     *
     * Synchronization is also used so that we update a single tile at a time.
     *
     * @param tile the PlaceTile request that was made.
     */
    public synchronized void tileChangeRequest(PlaceTile tile)
    {
        for( ObjectOutputStream out : users.values() )
        {
            try
            {
                out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tile));
            }
            catch(IOException e)
            {
                // squash
            }
        }
    }

}
