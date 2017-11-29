package place.server;

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
     *
     * @return a boolean; true if the login was successful, false if it failed.
     */
    public synchronized boolean login(String usernameRequest, ObjectOutputStream playerOutputStream)
    {
        // checks if the username is taken
        // if it's not, log ourselves in and return true so user can update
        if(!usernameTaken(usernameRequest))
        {
            // put ourselves in the HashMap
            users.put(usernameRequest, playerOutputStream);
            return true;
        }

        return false;
    }

    /**
     * Checks to see if a requested username is valid from a player
     *
     * @param usernameRequest
     *
     * @return
     */
    private boolean usernameTaken(String usernameRequest)
    {
        return users.containsKey(usernameRequest);
    }

}
