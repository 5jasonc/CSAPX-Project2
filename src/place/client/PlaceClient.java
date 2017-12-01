package place.client;

import place.PlaceBoard;
import place.PlaceException;
import place.PlaceTile;
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

    private synchronized void stop()
    {
        this.go = false;
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
                PlaceRequest<?> board = ( PlaceRequest<?> ) in.readObject();
                if(board.getType() == PlaceRequest.RequestType.BOARD)
                {
                    this.board = model;
                    this.board.initializeBoard( (PlaceBoard) board.getData() );
                }
                else
                    this.go = false;
            }
            new Thread( () -> this.run() ).start();
        }
        catch(IOException | ClassNotFoundException e)
        {
            throw new PlaceException(e);
        }
    }

    public void sendTile(PlaceTile tile)
    {
        try
        {
            this.out.writeObject(new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile));
        }
        catch (IOException e)
        {
            // squash
        }
    }

    private void tileChanged(PlaceTile tile)
    {
        // update the model to reflect the new tile change (so it can alert users)
        this.board.tileChanged(tile);
    }

    private void error(String error)
    {
        System.err.println("Server responded with error message: \"" + error + "\"");
        this.stop();
    }

    private void badResponse()
    {
        System.err.println("Bad response received from server. Terminating connection.");
        this.stop();
    }


    /**
     *
     */
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
                        tileChanged( (PlaceTile) request.getData() );
                        break;
                    case ERROR:
                        error( (String) request.getData() );
                        break;
                    // should not ever get these, if we get here we have to stop our client
                    case BOARD:
                        badResponse();
                        break;
                    case LOGIN:
                        badResponse();
                        break;
                    case LOGIN_SUCCESS:
                        badResponse();
                        break;
                    case CHANGE_TILE:
                        badResponse();
                        break;
                    default:
                        badResponse();
                }
            }
            catch(IOException | ClassNotFoundException e)
            {
                System.out.println("IOException!");
                this.stop();
                // alert the user there was an error
            }
        }
        this.close();
    }

    public void close()
    {
        try{
            this.serverConn.close();
        }
        catch (IOException e)
        {
            // heck!
        }
        this.board.close();
    }

}
