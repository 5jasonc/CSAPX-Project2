package place.client;

import place.PlaceBoard;
import place.PlaceException;
import place.network.PlaceRequest;
import place.PlaceBoardObservable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlaceClient {

    private String username;

    private PlaceBoardObservable board;

    private Socket serverConn;

    private ObjectInputStream in;

    private ObjectOutputStream out;

    private boolean go = false;

    private synchronized boolean go()
    {
        return this.go;
    }

    public PlaceClient(String host, int port, String username, PlaceBoardObservable model) throws PlaceException
    {
        try
        {
            // connects to the server
            this.serverConn = new Socket(host, port);
            this.username = username;


            // COMMUNICATION BUILDING SEQUENCE ================================
            // sets the in and out streams
            this.in = new ObjectInputStream(serverConn.getInputStream());
            this.out = new ObjectOutputStream(serverConn.getOutputStream());
            out.flush();


            // LOG IN SEQUENCE ================================
            // write out our login request
            out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username));

            // wait for response from server
            PlaceRequest<?> response = (PlaceRequest<?>) in.readObject();

            // go through each case
            // LOGIN_SUCCESS or ERROR (or unknown case)
            switch (response.getType())
            {
                case LOGIN_SUCCESS:
                    System.out.println("Successfully joined Place server as \"" + response.getData() + "\".");
                    this.go = true;
                    break;
                case ERROR:
                    System.out.println("Failed to join Place server. Server response: " + response.getData() + ". Terminating.");
                    break;
                default:
                    System.out.println("Unknown response received. Terminating.");
                    break;
            }


            // BOARD READ-IN SEQUENCE ===============================
            if(this.go)
            {
                PlaceRequest<?> board =  ( PlaceRequest<?> ) in.readObject();
                if(board.getType() == PlaceRequest.RequestType.BOARD)
                    this.board.initializeBoard( (PlaceBoard) board.getData() );
                else
                    this.go = false;
            }

            this.board = model;

            new Thread( () -> this.run() ).start();
        }
        catch(IOException | ClassNotFoundException e)
        {
            throw new PlaceException(e);
        }
    }

    private void run()
    {
        // this.go() is synchronized so we only do this one at a time
        while(this.go())
        {
            try
            {
                PlaceRequest<?> request = ( PlaceRequest<?> ) this.in.readObject();

                switch(request.getType())
                {
                    case TILE_CHANGED:
                        break;
                    case ERROR:
                        break;
                    // should not ever get these, if we get here we have to stop our client
                    case BOARD:
                        break;
                    case LOGIN:
                        break;
                    case LOGIN_SUCCESS:
                        break;
                    case CHANGE_TILE:
                        break;
                }
            }
            catch(IOException | ClassNotFoundException e)
            {
                // alert the user there was an error
            }
        }
        // yay
    }


    public void sendRequest( PlaceRequest<?> request ) throws IOException
    {
        out.writeObject(request);
        out.flush();
    }

    public Object getRequest() throws IOException, ClassNotFoundException
    {
        return in.readObject();
    }

}