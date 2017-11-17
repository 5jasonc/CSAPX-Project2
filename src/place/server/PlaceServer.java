package place.server;

import place.PlaceBoard;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import place.PlaceException;
import place.PlaceProtocol;

public class PlaceServer implements PlaceProtocol, Closeable {

    private PlaceBoard model;
    private ServerSocket server;
    private List<PlaceClientThread> users;
    private boolean go = false;

    public PlaceServer(int port) throws PlaceException
    {
        // makes a new ArrayList of PlaceClientThreads (this allows us to update the users online)
        this.users = new ArrayList<>();

        try
        {
            // makes a new ServerSocket on port
            this.server = new ServerSocket(port);
        }
        catch(IOException ioe)
        {
            throw new PlaceException(ioe);
        }
        this.go = true;
    }

    public void run(int dim) throws PlaceException
    {
        while(this.go)
        {
            try
            {
                Socket newPlayer = server.accept();
            }
            catch(IOException ioe)
            {
                throw new PlaceException(ioe);
            }
        }
    }

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
