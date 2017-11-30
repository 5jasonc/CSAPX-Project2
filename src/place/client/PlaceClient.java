package place.client;

import place.PlaceBoard;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class PlaceClient {

    private String username;

    private PlaceBoard board;

    private Socket serverConn;

    private ObjectInputStream in;

    private ObjectOutputStream out;

    private boolean go = false;

    public PlaceClient(String host, int port, String username) throws IOException
    {
        // connects to the server
        this.serverConn = new Socket(host, port);

        // sets the in and out streams
        this.in = new ObjectInputStream( serverConn.getInputStream() );
        this.out = new ObjectOutputStream( serverConn.getOutputStream() );
        out.flush();
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

    public PlaceBoard readBoard() throws IOException, ClassNotFoundException
    {
        PlaceRequest<?> request = ( PlaceRequest<?> ) in.readObject();
        this.board = (PlaceBoard) request.getData();

        return this.board;
    }


}
