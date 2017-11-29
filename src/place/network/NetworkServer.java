package place.network;

import place.network.PlaceRequest;
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
     * @param usernameRequest
     * @param playerOutputStream
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
