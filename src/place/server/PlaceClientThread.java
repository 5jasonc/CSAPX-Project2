package place.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;

import place.network.PlaceRequest;

public class PlaceClientThread extends Thread
{
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private PlaceServer server;

    String username;

    private int dim;

    public PlaceClientThread(Socket player, int dim, PlaceServer server) throws IOException, ClassNotFoundException
    {
        this.in = new ObjectInputStream( player.getInputStream() );
        this.out = new ObjectOutputStream( player.getOutputStream() );

        // LOGIN username; gets second part and sets it to the name
        PlaceRequest<?> request = (PlaceRequest<?>)in.readObject();

        if(request.getType() == PlaceRequest.RequestType.LOGIN)
            this.username = (String) request.getData();
        else
        {
            PlaceRequest<String> loginError = new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Login failed.");
            out.writeObject(loginError);
            out.flush();
        }

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
        return this.username;
    }

    /**
     * An override of the run method which runs the thread
     */
    @Override
    public void run()
    {

    }

}
