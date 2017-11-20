package place.server;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.Socket;

import java.util.Scanner;

import place.PlaceProtocol;

public class PlaceClientThread extends Thread
{
    private Scanner clientIn;
    private PrintWriter clientOut;

    private PlaceServer server;

    private int dim;

    private String username;

    public PlaceClientThread(Socket player, int dim, PlaceServer server) throws IOException
    {
        this.clientIn = new Scanner( player.getInputStream() );
        this.clientOut = new PrintWriter( player.getOutputStream() );

        // LOGIN username; gets second part and sets it to the name
        this.username = clientIn.nextLine().split(" ")[1];

        this.dim = dim;

        this.server = server;
    }

    void loginFailed()
    {
        clientOut.println(PlaceProtocol.LOGIN_FAILED + " " + this.username);
    }

    String getUsername(){ return this.username; }

    /**
     * An override of the run method which runs the thread
     */
    @Override
    public void run()
    {

    }

}
