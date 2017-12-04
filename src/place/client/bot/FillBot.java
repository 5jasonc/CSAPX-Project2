package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceException;
import place.network.NetworkClient;

import java.io.PrintWriter;

import java.util.*;

public class FillBot extends BotApplication implements Observer {

    private NetworkClient serverConn;

    private String username;

    private PlaceBoardObservable model;

    private PlaceColor selectedColor;

    private static Map<String, PlaceColor> colorChoices;

    private boolean go;

    private boolean pause;

    private synchronized boolean go()
    {
        return this.go();
    }

    /**
     *
     *
     * @throws Exception if any exception is run into
     */
    public void init() throws Exception
    {
        // gets our arguments from BotApplication
        List<String> arguments = super.getArguments();

        // sets our required elements
        String host = arguments.get(0);
        int port = Integer.parseInt(arguments.get(1));
        this.username = arguments.get(2);
        this.selectedColor = colorChoices.get(arguments.get(3));

        // creates a new blank model
        this.model = new PlaceBoardObservable();

        try
        {
            // sets our network client, this is the last thing we do to minimize time between receiving the board and
            // opening the Bot
            this.serverConn = new NetworkClient(host, port, username, model);
        }
        catch(PlaceException e)
        {
            // closes serverConn
            this.serverConn.close();
            // runs our stop method so that we can deconstruct anything we've built thus far
            // this.stop();
            // tells the user about the issue we've run into
            throw e;
        }
    }

    public void start(Scanner in, PrintWriter out)
    {
        this.model.addObserver(this);
        this.serverConn.start();

        this.go = true;

        new Thread(this::run);
    }

    public void update(Observable o, Object tile)
    {
        // update
    }

    public void stop()
    {

    }


    public void pause()
    {

    }


    public void run()
    {
        while(this.go())
        {

        }
    }

    // this builds our HashMap of color choices that the bot can use before running main
    static
    {
        // creates a new HashMap of color choices
        colorChoices = new HashMap<>();
        // loops through each possible color
        for(PlaceColor color : PlaceColor.values())
            colorChoices.put(color.toString(), color);
    }

    public static void main(String[] args) {
        // we need exactly 4 arguments
        if(args.length != 4)
        {
            System.err.println("Please run the bot as: ");
            System.err.println("$ java FillBot host port username color");
            return;
        }
        // launch the application
        BotApplication.launch(FillBot.class, args);
    }
}
