package place.network;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest.RequestType;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * A network middle-man for a Place server.
 *
 * @author Kevin Becker (kjb2503)
 */
public class NetworkServer
{
    /**
     * The header that gets printed before every log.
     */
    private static final String LOG_HEADER = "[PlaceServer]: ";

    /**
     * The maximum number of connections the server can have from any client.
     */
    private static final int MAX_TOTAL_CONNECTIONS = 100;

    /**
     * The maximum number of connections a single host can have to our server.
     */
    private static final int MAX_CONNECTIONS_SINGLE_HOST = 10;

    /**
     * The date formatter used when a tile is changed.
     */
    private final static SimpleDateFormat TIME_STAMP_FORMAT = new SimpleDateFormat("MM-dd-yyyy 'at' HH:mm:ss");

    /**
     * The Map that contains all of the currently connected users.
     * The key is a String that is the username
     * The value is that user's ObjectOutputStream
     */
    private Map<String, ObjectOutputStream> users;

    /**
     * The connections that are coming from each location (prevents IP spam/DOS attack).
     */
    private Map<InetAddress, Integer> connections;

    /**
     * The total number of connections on the server at the current moment. The maximum connections is 100.
     */
    private int totalConnections;

    /**
     * The "master" PlaceBoard that is used to send to new users.
     */
    private PlaceBoard board;

    /**
     * The PrintWriter that is used to log the status of the game.
     */
    private PrintWriter log;


    /**
     * Constructs a new NetworkServer used to communicate to Place clients.
     *
     * THIS CONSTRUCTOR IS ONLY CALLED BY: PlaceServer
     *
     * @param dim the dimension of the board once it is set up.
     */
    public NetworkServer(int dim, PrintWriter log)
    {
        // creates a new HashMap that will house all of the logged in users
        this.users = new HashMap<>();

        // creates a new HashMap that will house all of the internet locations for users
        this.connections = new HashMap<>();

        // this holds the "master" PlaceBoard that will be updated with every move and sent to new users
        this.board = new PlaceBoard(dim);

        this.log = log;
    }

    /**
     * The server calls this method once it has started completely.
     *
     * @param port The port the server has started on.
     */
    public void serverStarted(int port)
    {
        log("Server has started successfully. Accepting connections on port " + port + ".");
    }

    /**
     * Logs in a user. This is synchronized so we don't run into people requesting the same username at the same time.
     *
     * THIS METHOD IS ONLY CALLED BY: PlaceClientThread
     *
     * @param usernameRequest The requested username from a user.
     * @param out The output stream for the user.
     */
    public synchronized boolean login(String usernameRequest, InetAddress location, ObjectOutputStream out)
    {
        // checks if the username is taken
        // if it's not, log ourselves in and return true so user can update
        try
        {
            // creates an entry in the connections Map if this is a new location
            if(!connections.containsKey(location))
                connections.put(location, 0);

            // if the username is taken
            if(usernameTaken(usernameRequest))
            {
                logSilent("");
                // tell the user the username they requested is taken
                out.writeUnshared(new PlaceRequest<>(RequestType.ERROR, "Username taken"));
            }
            // else if the number of connections from their IP is at max
            else if (this.connections.get(location) >= MAX_CONNECTIONS_SINGLE_HOST)
            {
                // silently logs if the user has attempted to join while their IP is at max
                logSilent("A user has attempted to join the server from an IP address with max connections.");
                logSilent("Requested username: " + usernameRequest + " [" + location + "]");
                // tell the user there are too many connections from their IP
                out.writeUnshared(new PlaceRequest<>(RequestType.ERROR, "Too many connections from your IP"));
            }
            // else if the number of connections is at max
            else if (this.totalConnections >= MAX_TOTAL_CONNECTIONS)
            {
                // silently logs the user trying to join while server full
                logSilent(usernameRequest + " has attempted to join the server while full. Denying connection.");
                logSilent("Requested username: " + usernameRequest + " [" + location + "]");
                // tell the user the server is full
                out.writeUnshared(new PlaceRequest<>(RequestType.ERROR, "Server full"));
            }
            // if we are able to accept another connection and the username isn't taken, we get to this point
            else
            {
                // put our new user in the HashMap
                this.users.put(usernameRequest, out);

                // adds one to the number of connections from this host
                this.connections.put(location, this.connections.get(location) + 1);

                // adds one to the total connections
                ++this.totalConnections;

                // tell the user they were logged in successfully
                out.writeUnshared(new PlaceRequest<>(RequestType.LOGIN_SUCCESS, usernameRequest));
                // then immediately send the current board so they can begin setup immediately
                out.writeUnshared(new PlaceRequest<>(RequestType.BOARD, this.board));
                // this is the only place we return true
                log( usernameRequest + " has joined the server. [" + location + "]");
                return true;
            }
        }
        catch(IOException e)
        {
            // oops
        }
        // if login fails, we let the PlaceClientThread know by returning false (so it may close properly)
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
        // log we have had an error from (username)
        logErr(username + " has sent a bad request. Request type: " + type);
        logErr("Terminating connection for " + username + ".");

        // gets the ObjectOutputStream associated with the username
        ObjectOutputStream out = this.users.get(username);

        // alerts the user they sent a bad request as well as the type (if somehow we get here, they are being naughty
        // and using a custom client.)
        // please don't be that person
        out.writeObject(new PlaceRequest<>(
                PlaceRequest.RequestType.ERROR, "Bad request received: " + type + ". Terminating connection.")
        );

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
    public void logout(String username, InetAddress location)
    {
        // removes one from their location
        this.connections.put(location, this.connections.get(location)-1);

        // clears it from the map for memory saving purposes
        if(this.connections.get(location) <= 0)
            this.connections.remove(location);

        // removes one from the total connections
        --this.totalConnections;

        // logs a user out (essentially logs just removes them from the map)
        // since the ObjectOutputStream is just a pointer, this is all we have to do
        users.remove(username);
        // alert that user has disconnected
        log(username + " has left the server.");
    }

    /**
     * Alerts all of the users who are logged in that a new tile change request has occurred. It is synchronized so that
     * if multiple users send a move for the same tile at the same time we don't have mismatched boards.
     *
     * Synchronization is also used so that we update a single tile at a time.
     *
     * @param tile the PlaceTile request that was made.
     */
    public synchronized boolean tileChangeRequest(String username, PlaceTile tile)
    {
        // logs silently username's request to change a tile
        logSilent(username + " requested to change a tile: " + tile);

        // checks if a tile is invalid, if it is returns false while we still can
        if(!isValid(tile))
            return false;
        // creates our changedTile request to send to all users
        PlaceRequest<PlaceTile> changedTile = new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tile);
        // loops through each user that is currently connected
        for (ObjectOutputStream out : users.values()) {
            try
            {
                // writes out our changed tile
                out.writeUnshared(changedTile);
                // sets the place in the board that was just changed
                this.board.setTile(tile);
            }
            catch (IOException e) { /* oops*/ }
        }
        // once we've gone through every user we can return true
        return true;
    }

    /**
     * If a user sends a tile within the cool-down period, we note that here and ignore their request.
     *
     * @param username The username of the user that sent a request too quickly.
     */
    public synchronized void fastRequest(String username)
    {
        // logs the error
        logErr(username + " has sent a tile too quickly. Ignoring it.");
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
     * Logs a non-error message to standard output AND log file.
     *
     * @param msg The message to be printed out.
     */
    private void log(String msg)
    {
        System.out.println(LOG_HEADER + msg);
        // logs the msg to the log file
        // auto flush is enabled, no need to flush()
        this.log.println("[" + now() + "]: " + msg);
    }

    /**
     * Logs an error message to standard output AND log file.
     *
     * @param msg The message to be printed out.
     */
    private void logErr(String msg)
    {
        System.err.println(LOG_HEADER + msg);
        // logs we have hit an error
        // auto flush is enabled, no need to flush()
        this.log.println("[" + now() + "]: ALERT! " + msg);
    }

    /**
     * Logs to the log file only; no display to terminal window.
     *
     * @param msg The message to be logged.
     */
    private void logSilent(String msg)
    {
        this.log.println("[" + now() + "]: " + msg);
    }

    /**
     * Returns a time stamp for the current system time.
     *
     * @return A string of the format: MM/dd/YY at HH:MM:SS
     */
    private String now()
    {
        return TIME_STAMP_FORMAT.format(System.currentTimeMillis());
    }

    /**
     * In the event of a catastrophic server error, we attempt to tell the clients that we've experienced an error so
     * they may shutdown gracefully.
     */
    public void serverError()
    {
        // logs we have initiated our server panic sequence of telling all users that we are disconnecting.
        logSilent("Server has initiated panic sequence!");
        logSilent("Alerting all connections the server will deactivate.");

        // creates our error request to send to all users
        PlaceRequest<String> error = new PlaceRequest<>(PlaceRequest.RequestType.ERROR,
                "The server has hit an unrecoverable error. Terminating all connections.");
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
