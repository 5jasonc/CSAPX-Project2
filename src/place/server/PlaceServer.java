package place.server;

import place.PlaceBoard;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import place.PlaceException;

public class PlaceServer implements Closeable {
    // the server should be 15 lines-ish

    // server communicates to "NetworkServer"
    // server communicates with "PlaceClientThread"
    // HashMap<String, ObjectOutputStream>
    //          user    output connection
    // NetworkServer needs to be synchronized: login, sending board to user(in login), tile change (looped through), logoff


    private ServerSocket server;
    private boolean go = false;

    /**
     *
     *
     * @param port
     *
     * @throws PlaceException
     */
    public PlaceServer(int port) throws PlaceException
    {
        try
        {
            // makes a new server
            this.server = new ServerSocket(port);
        }
        catch(IOException ioe)
        {
            // throws the exception as a PlaceException
            throw new PlaceException(ioe);
        }
        this.go = true;
    }

    /**
     *
     *
     * @param dim
     *
     * @throws PlaceException
     */
    private void run(int dim) throws PlaceException
    {
        while(this.go)
        {
            try
            {
                // gather a new client
                PlaceClientThread client = new PlaceClientThread(server.accept(), dim, this);
                // start the thread and keep going
                client.start();
            }
            catch(Exception e)
            {
                // throws the error as  PlaceException
                throw new PlaceException(e);
            }
        }
    }

    /**
     *
     */
    public void close()
    {
        try
        {
            this.server.close();
        }
        catch(IOException ioe)
        {
            // oops sorry don't know what to do here...
        }
    }

    /**
     *
     *
     * @param args
     */
    public static void main(String[] args) {
        if(args.length != 2)
        {
            System.out.println("Usage: PlaceServer port dimension");
            return;
        }
        // the port of the server, could fool-proof this and check to make sure this is valid port
        int port = Integer.parseInt(args[0]);
        // the dimension of the board
        int dim = Integer.parseInt(args[1]);


        // tries to make a new server object running on the port
        try ( PlaceServer server = new PlaceServer( port ) )
        {
            server.run( dim );
        }
        catch (PlaceException e)
        {
            System.err.println("FATAL ERROR: Server could not be started.");
            e.printStackTrace();
        }
    }
}
