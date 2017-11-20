package place.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

import place.network.PlaceRequest;

public class PlaceClientThread extends Thread
{
    private ObjectInputStream clientIn;
    private ObjectOutputStream clientOut;

    private PlaceServer server;

    private int dim;

    public PlaceClientThread(Socket player, int dim, PlaceServer server) throws IOException, ClassNotFoundException
    {
        this.clientIn = new ObjectInputStream( player.getInputStream() );
        this.clientOut = new ObjectOutputStream( player.getOutputStream() );

        // LOGIN username; gets second part and sets it to the name
        PlaceRequest<?> request = (PlaceRequest<?>) clientIn.readObject();


        this.dim = dim;

        this.server = server;
    }

    void loginFailed() throws IOException
    {

    }

    void loginSucceeded() throws IOException
    {
    }


    String getUsername()
    {
        return "";
    }

    /**
     * An override of the run method which runs the thread
     */
    @Override
    public void run()
    {

    }

}
