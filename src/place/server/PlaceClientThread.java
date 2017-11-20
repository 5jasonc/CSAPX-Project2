package place.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class PlaceClientThread extends Thread
{
    private Scanner clientIn;
    private PrintWriter clientOut;

    private PlaceServer server;


    public PlaceClientThread(Socket player, PlaceServer server) throws IOException
    {
        this.clientIn = new Scanner( player.getInputStream() );
        this.clientOut = new PrintWriter( player.getOutputStream() );
        this.server = server;
    }

    /**
     * An override of the run method which runs the thread
     */
    @Override
    public void run()
    {

    }

}
