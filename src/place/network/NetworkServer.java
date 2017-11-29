package place.network;

import place.server.PlaceBoardObservable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

public class NetworkServer
{
    //          uname,  output to user
    private Map<String, ObjectOutputStream> users;
    private PlaceBoardObservable model;

    public NetworkServer(int dim)
    {
        this. users = new HashMap<>();
        this.model = new PlaceBoardObservable();
    }

    /**
     * Logs in a user. This is synchronized so we don't run into people requesting the same username at the same time.
     *
     * @param usernameRequest The requested username from a user.
     * @param playerOutputStream The output stream for the user.
     */
    public synchronized void login(String usernameRequest, ObjectOutputStream playerOutputStream)
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
                // tell the user they were logged in successfully
                playerOutputStream.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.LOGIN_SUCCESS, usernameRequest));
            }
            else
            {
                // tell the user the username is taken
                playerOutputStream.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Username taken"));
            }
            // write our result out (doesn't matter which because either way we need to send something)
            playerOutputStream.flush();
        }
        catch(IOException e)
        {
            // heck what do we do here??
        }
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
        users.remove(username);
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

}
