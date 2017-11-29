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


    private ServerSocket server;
    private NetworkServer networkServer;
    private boolean go = false;

    /**
     *
     *
     * @param port
     *
     * @throws PlaceException
     */
    public PlaceServer(int port, int dim) throws PlaceException
    {
        try
        {
            // makes a new server
            this.server = new ServerSocket(port);
            this.networkServer = new NetworkServer(dim);
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
     * @throws PlaceException
     */
    private void run() throws PlaceException
    {
        while(this.go)
        {
            try
            {
                // gather a new client and start it
                new PlaceClientThread(server.accept(), this, this.networkServer).start();
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
            // if this happens... well. :)
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
        try ( PlaceServer server = new PlaceServer(port, dim) )
        {
            server.run();
        }
        catch (PlaceException e)
        {
            System.err.println("FATAL ERROR: Server hit a fatal error.");
            e.printStackTrace();
        }
    }
}
