package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.network.NetworkClient;

import java.io.PrintWriter;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

public class FillBot extends BotApplication implements Observer {

    private NetworkClient serverConn;

    private String username;

    private PlaceBoardObservable model;

    private PlaceColor selectedColor;

    private boolean go;

    private boolean pause;

    public void init()
    {
        List<String> arguments = super.getArguments();

        String host = arguments.get(0);
        int port = Integer.parseInt(arguments.get(1));
        this.username = arguments.get(2);
        String color = arguments.get(3);
    }

    public void start(Scanner in, PrintWriter out)
    {

    }

    public void update(Observable o, Object t)
    {

    }

    public void stop()
    {

    }


    public void pause()
    {

    }

    public void run()
    {

    }

    public static void main(String[] args) {
        if(args.length != 4)
        {
            System.err.println("Please run the bot as: ");
            System.err.println("$ java FillBot host port username color");
            return;
        }
        BotApplication.launch(FillBot.class, args);
        System.out.println("hello");
    }
}
