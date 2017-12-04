package place.client.bot;

import place.PlaceBoardObservable;
import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.NetworkClient;

import java.util.*;

import static java.lang.Thread.sleep;

public class FillBot extends BotApplication implements Observer {

    private static List<PlaceColor> colorChoices;

    private final static String PROMPT = "> ";

    private NetworkClient serverConn;

    private String username;

    private PlaceBoardObservable model;

    private int currentColor = 0;

    private boolean sticky;

    private boolean go;

    private boolean pause;

    @Override
    public synchronized boolean go()
    {
        return this.go;
    }

    private synchronized boolean paused()
    {
        return this.pause;
    }


    /**
     * Initializes the bot.
     *
     * @throws Exception if any exception is run into
     */
    @Override
    public void init() throws Exception
    {
        // gets our arguments from BotApplication
        List<String> arguments = super.getArguments();

        // sets our required elements
        String host = arguments.get(0);
        int port = Integer.parseInt(arguments.get(1));
        this.username = arguments.get(2);

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

    @Override
    public void start()
    {
        System.out.println("Finished building FillBot. Beginning fill.");
        this.model.addObserver(this);
        this.serverConn.start();

        this.go = true;

        new Thread(this::run).start();
    }

    @Override
    public void startCmdListening(Scanner in)
    {
        while(this.go())
        {
            System.out.print(PROMPT);
            // gets the next command (first full word)
            String command = in.next();

            // gets the rest of the line and throws it into a tokens array (used for sticky)
            String [] tokens = in.nextLine().trim().split(" ");

            switch(command)
            {
                // handles multiple versions that might be passed (help is the only one advertised).
                case "help":
                case "h":
                case "-help":
                case "-h":
                    printHelp();
                    break;
                case "quit":
                case "q":
                case "-quit":
                case "-q":
                    quit();
                    break;
                case "pause":
                case "p":
                case "-pause":
                case "-p":
                    pause();
                    break;
                case "resume":
                case "r":
                case "-resume":
                case "-r":
                    resume();
                    break;
                case "sticky":
                case "s":
                case "-sticky":
                case "-s":
                    if(tokens.length != 0)
                    {
                        try
                        {
                            System.out.println(tokens.length);
                            sticky(Integer.parseInt(tokens[0]));
                        }
                        catch(NumberFormatException | PlaceException e)
                        {
                            badCommand();
                        }
                    }
                    else
                        sticky();
                    break;
                case "cycle":
                case "c":
                case "-cycle":
                case "-c":
                    cycle();
                    break;
                default:
                    badCommand();
            }
        }
    }

    private void printHelp()
    {
        System.out.println("Commands:\n" +
                "help: display this information again.\n" +
                "quit: exits the bot cleanly.\n" +
                "pause: pauses the bot at its current tile.\n" +
                "resume: resumes the bots cycle at its current tile.\n"+
                "sticky [color] : keeps the bot on a single color.\n" +
                "\t (note: the color is optional, it must be an integer 0-15.)\n" +
                "cycle: returns bot to cycle mode");
    }

    private void pause()
    {
        this.pause = true;
    }

    private void resume()
    {
        this.pause = false;
        notifyAll();
    }

    private void quit()
    {
        this.go = false;
    }

    private void sticky()
    {
        this.sticky = true;
    }

    private void sticky(int color) throws PlaceException
    {
        if(color < 0 || color > 15)
            throw new PlaceException("Color number is not valid.");
        this.currentColor = color;
        this.sticky = true;
    }

    private void cycle()
    {
        this.sticky = false;
    }

    private void badCommand()
    {
        System.out.println("Bad command.");
    }



    public void update(Observable o, Object tile)
    {
        // update
    }

    @Override
    public void stop()
    {
        // when the program closes we close our NetworkClient so it knows to stop executing and log us out
        super.stop();
        // indicates to serverConn that it should close
        this.serverConn.close();
    }


    private void run()
    {
        int currentRow = 0;
        int currentCol = 0;
        int maxRow = this.model.getDIM();
        int maxCol = this.model.getDIM();

        while(this.go())
        {
            if(this.paused())
            {
                try { wait(); } catch(InterruptedException ie){/* do nothing just continue forward */}
            }
            else
            {
                try
                {
                    this.serverConn.sendTile(
                            new PlaceTile(currentRow, currentCol, this.username,
                                    colorChoices.get(this.currentColor), System.currentTimeMillis())
                    );
                    // changes our row
                    currentCol = (currentCol + 1 >= maxCol ) ? 0 : currentCol + 1;

                    // if row was reset, add to column
                    if( currentCol == 0 )
                        currentRow = (currentRow + 1 >= maxRow ) ? 0 : currentRow + 1;

                    if(currentRow == 0 && currentCol == 0 && !this.sticky)
                        this.currentColor = (currentColor + 1 == PlaceColor.values().length) ? 0 : currentColor + 1;

                    // 2-second sleep
                    sleep(20);
                } catch(InterruptedException ie){/* do nothing we don't care */}
            }
        }
    }


    public static void main(String[] args) {
        // we need exactly 4 arguments
        if(args.length != 4)
        {
            System.err.println("Please run the bot as: ");
            System.err.println("$ java FillBot host port username");
            return;
        }

        // creates a new List of color choices
        colorChoices = Arrays.asList(PlaceColor.values());

        // launch the application
        BotApplication.launch(FillBot.class, args);
    }
}
