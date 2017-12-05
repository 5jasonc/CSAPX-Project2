package place.network;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest.RequestType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

// fully commented

/**
 * A network middle-man for a Place server.
 *
 * @author Kevin Becker (kjb2503)
 */
public class NetworkServer
{
    //TODO: server log function
    /**
     * The Map that contains all of the currently connected users.
     * The key is a String that is the username
     * The value is that user's ObjectOutputStream
     */
    private Map<String, ObjectOutputStream> users;

    /**
     * The "master" PlaceBoard that is used to send to users and
     */
    private PlaceBoard board;


    /**
     * Constructs a new NetworkServer used to communicate to Place clients.
     *
     * THIS CONSTRUCTOR IS ONLY CALLED BY: PlaceServer
     *
     * @param dim the dimension of the board once it is set up.
     */
    public NetworkServer(int dim)
    {
        // creates a new HashMap that will house all of the logged in users
        this.users = new HashMap<>();
        // this holds the "master" PlaceBoard that will be updated with every move and sent to new users
        this.board = new PlaceBoard(dim);
    }

    /**
     * Logs in a user. This is synchronized so we don't run into people requesting the same username at the same time.
     *
     * THIS METHOD IS ONLY CALLED BY: PlaceClientThread
     *
     * @param usernameRequest The requested username from a user.
     * @param out The output stream for the user.
     */
    public synchronized boolean login(String usernameRequest, ObjectOutputStream out)
    {
        // checks if the username is taken
        // if it's not, log ourselves in and return true so user can update
        try
        {
            // checks if our username is valid or not (a.k.a. is there a key with our username?)
            if(!usernameTaken(usernameRequest))
            {
                // put ourselves in the HashMap
                users.put(usernameRequest, out);

                // alert that user has connected
                System.out.println("[Server]: " + usernameRequest + " has joined the server.");

                // tell the user they were logged in successfully
                out.writeUnshared(new PlaceRequest<>(RequestType.LOGIN_SUCCESS, usernameRequest));
                // sends the board as well since we have connected and we need to build our board
                out.writeUnshared(new PlaceRequest<>(RequestType.BOARD, this.board));
                // return true iff login was successful
                return true;
            }
            else
            {
                // tell the user the username is taken
                out.writeUnshared(new PlaceRequest<>(RequestType.ERROR, "Username taken."));
            }
            // write our result out (doesn't matter which because either way we need to send something)
            out.flush();
        }
        catch(IOException e)
        {
            // oops
        }
        // if login fails, we let the PlaceClientThread
        return false;
    }

    /**
     * Checks to see if a requested username is valid from a player.
     *
     * @param usernameRequest The requested username from the user which we are checking for.
     *
     * @return A boolean. True if the username is already taken; false otherwise.
     */
    private boolean usernameTaken(String usernameRequest)
    {
        // return whether or not the HashMap contains our key
        return users.containsKey(usernameRequest);
    }

    /**
     * If the user provides a bad request (i.e. sending something only the server can send), we tell them that they gave
     * us bad input so they can shutdown.
     *
     * @param username The username of the user that provided bad input.
     * @param type The type of request that gave us the issue.
     *
     * @throws IOException If there is an issue communicating with the client.
     */
    public void badRequest(String username, String type) throws IOException
    {
        // gets the ObjectOutputStream associated with the username
        ObjectOutputStream out = this.users.get(username);

        // alerts the user they sent a bad request as well as the type (if somehow we get here they are being naughty
        // and using a custom client.)
        // please don't be that person
        out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Bad request received: " + type + ". Terminating connection."));

        // prints to the server log that user has sent a bad request
        System.err.println("[Server]: Bad request received from " + username + ". REQUEST: " + type);

        // flushes the stream so it sends
        out.flush();
    }

    /**
     * Logs a user out. Doesn't need to be synchronized since we won't have multiple people logged in with the same
     * username trying to log out.
     *
     * THIS IS CALLED BY: PlaceClientThread
     *
     * @param username The username of the user wishing to log out.
     */
    public void logout(String username)
    {
        // logs a user out (essentially logs just removes them from the map)
        // since the ObjectOutputStream is just a pointer, this is all we have to do
        users.remove(username);
        // alert that user has disconnected
        System.out.println("[Server]: " + username + " has left the server.");
    }

    /**
     * Alerts all of the users who are logged in that a new tile change request has occurred. It is synchronized so that
     * if multiple users send a move for the same tile at the same time we don't have mismatched boards.
     *
     * Synchronization is also used so that we update a single tile at a time.
     *
     * @param tile the PlaceTile request that was made.
     */
    public synchronized boolean tileChangeRequest(PlaceTile tile)
    {
        if(isValid(tile))
        {
            // creates our changedTile request to send to all users
            PlaceRequest<PlaceTile> changedTile = new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tile);
            // loops through each user that is currently connected
            for (ObjectOutputStream out : users.values()) {
                try {
                    // writes out our changed tile
                    out.writeUnshared(changedTile);
                    // sets the place in the board that was just changed
                    this.board.setTile(tile);
                } catch (IOException e) {
                    // oops
                }
            }
            // once we've gone through every user we van return true
            return true;
        }
        // if we get here something is no good!
        return false;
    }

    /**
     * Checks to see if a move is valid or not before requesting a tile change.
     *
     * @param tile the PlaceTile that is being checked for validity.
     *
     * @return A boolean. True if the PlaceTile is valid for the board; false otherwise.
     */
    private boolean isValid(PlaceTile tile)
    {
        return this.board.isValid(tile);
    }

    /**
     * In the event of a catastrophic server error, we attempt to tell the clients that we've experienced an error so
     * they may shutdown gracefully.
     */
    public void serverError()
    {
        // creates our error request to send to all users
        PlaceRequest<String> error = new PlaceRequest<>(PlaceRequest.RequestType.ERROR,
                "The server has hit an unrecoverable error.");
        // loops through each user that is currently connected
        for( ObjectOutputStream out : users.values() )
        {
            try
            {
                // write out our error to all clients
                out.writeObject(error);
            }
            catch(IOException e)
            {
                // oops
            }
        }
    }
}
