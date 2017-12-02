package place.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;

import place.PlaceException;
import place.network.NetworkServer;

public class PlaceServer implements Closeable {
    // the server should be 15 lines-ish

    // server communicates to "NetworkServer"
    // server communicates with "PlaceClientThread"
    // HashMap<String, ObjectOutputStream>
    //          user    output connection
    // NetworkServer needs to be synchronized: login, sending board to user(in login), tile change (looped through), logoff

    //TODO: make a server console GUI (for better looking updates)?

    private ServerSocket server;

    private NetworkServer networkServer;

    private boolean go;

    private boolean go()
    {
        return this.go;
    }


    /**
     * Constructs a new PlaceServer which is used to accept connections from Place clients.
     *
     * NOTE: access is private because we only want the main method to be able to create an instance of it.
     *
     * @param port the port that is requested to run on.
     * @param dim the square dimension of the Place board.
     *
     * @throws PlaceException if any sort of exception is run into it is wrapped in a PlaceException.
     */
    private PlaceServer(int port, int dim) throws PlaceException
    {
        try
        {
            // makes a new server socket broadcasting on port
            this.server = new ServerSocket(port);
            // makes a new NetworkServer (the major brains of the program)
            this.networkServer = new NetworkServer(dim);
        }
        catch(IOException ioe)
        {
            // throws the exception as a PlaceException
            throw new PlaceException(ioe);
        }
        this.go = true;
        // say this to output once we've set everything up.
        System.out.println("[PlaceServer]: Server initialization complete. Now accepting connections. Listening on: " +
                server.getInetAddress().getHostAddress() + ":" + server.getLocalPort());
    }

    /**
     * Runs the server which essentially just accepts connections and spawns PlaceClientThreads until it is shut down.
     *
     * @throws PlaceException if any sort of exception is run into, it is wrapped in a PlaceException.
     */
    private void run() throws PlaceException
    {
        // while we are still good to go
        while(this.go())
        {
            try
            {
                // gather a new client and start it
                new PlaceClientThread(server.accept(), this.networkServer).start();
            }
            catch(Exception e)
            {
                this.networkServer.serverError();
                // throws the error as  PlaceException
                throw new PlaceException(e);
            }
        }
    }

    /**
     * Closes the ServerSocket so that no more connections can be created.
     */
    public void close()
    {
        try
        {
            // closes the ServerSocket (we're done with it if we're closing)
            this.server.close();
        }
        catch(IOException ioe)
        {
            // if this happens... well. :)
        }
    }

    /**
     * The main method which is used to create a PlaceServer.
     *
     * @param args The arguments of the main method. The arguments should have: [port, dimension].
     */
    public static void main(String[] args)
    {
        // check to make sure we've been given the right NUMBER of
        if(args.length != 2)
        {
            // alert the user they have given us bad stuff
            System.out.println("Usage: PlaceServer port dimension");
            return;
        }

        // the port of the server, could fool-proof this and check to make sure this is valid port
        int port = Integer.parseInt(args[0]);
        // the dimension of the board
        int dim = Integer.parseInt(args[1]);

        // tries to make a new server object running on the port
        try ( PlaceServer server = new PlaceServer(port, dim) )
        {
            // if we've made it this far, we can begin running the server
            server.run();
        }
        catch (PlaceException e)
        {
            System.err.println("FATAL ERROR: Server hit a fatal error.");
            e.printStackTrace();
        }
    }
}
