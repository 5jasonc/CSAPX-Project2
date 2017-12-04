package place.client.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Observer;
import java.util.Observable;

import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.NetworkClient;
import place.PlaceBoardObservable;

public class PlaceGUI extends Application implements Observer {

    private static final int RECT_SIZE = 70;

    private static final int BOTTOM_GAPS = 7;

    private Scene scene;

    private GridPane mainGrid;

    private Map< String, String > parameters;

    private NetworkClient serverConn;

    private String username;

    private PlaceBoardObservable model;

    private PlaceColor selectedColor = PlaceColor.BLACK;

    private String getParamNamed( String name ) throws PlaceException
    {
        // if we have yet to set our parameters, we get those here
        if ( this.parameters == null )
            this.parameters = super.getParameters().getNamed();
        // checks ot make sure we have the parameter, throws an error if it is missing
        if ( !parameters.containsKey(name) )
            throw new PlaceException("Can't find parameter named " + name);
        // otherwise we return the one as named.
        else
            // gets the parameter with name name
            return parameters.get( name );
    }


    /**
     * Initializes the client before a build of the GUI.
     *
     * @throws Exception If any sort of exception is hit while trying to initialize the GUI.
     */
    @Override
    public void init() throws Exception
    {
        // calls the super class' init() method
        super.init();

        // gets our parameters that we need (host, port, username)
        String host = getParamNamed("host");
        int port = Integer.parseInt(getParamNamed("port"));
        this.username = getParamNamed("user");

        // sets our model to a new blank PlaceBoardObservable
        this.model = new PlaceBoardObservable();

        // this is the most uncertain part, we wrap this in a try so that if it fails we can exit without opening the program
        try
        {
            // sets our network client, this is the last thing we do to minimize time between receiving the board and
            // opening the GUI
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

    /**
     * Constructs our GUI and then displays it at the end
     *
     * @param primaryStage the stage that we are showing our GUI upon.
     */
    @Override
    public void start(Stage primaryStage)
    {
        // creates a blank BorderPane
        BorderPane root = new BorderPane();

        // sets our mainGrid in place
        root.setCenter( this.mainGrid = buildMainGrid( this.model.getDIM(), this.model.getDIM() ) );

        // builds our color selection FlowPane
        root.setTop( buildColorBar() );

        // add ourselves as an observer of the model
        this.model.addObserver(this);

        // starts the thread that NetworkClient uses to listen to the server
        // this allows the client to start up completely before any tile changes that occurred between launch(args) and
        // addObserver(this) are acknowledged
        // essentially this creates a sort of Queue in the ObjectInputStream from NetworkClient of any "TILE_CHANGED"s
        // the updates will begin immediately after this
        this.serverConn.start();

        // saves our scene
        this.scene = new Scene(root);

        // sets our scene
        primaryStage.setScene(this.scene);

        // sets the title of our window
        primaryStage.setTitle("k/Place: " + this.username);

        // sets our stage to be without the OS dependent control system
        // we can now use our own style
        // primaryStage.initStyle(StageStyle.UNDECORATED);

        // makes it so the user cannot rescale the window
        primaryStage.setResizable(false);

        // we have now completed building our GUI, we can show it
        // anything we change in the GUI after this MUST be used with runLater( ... )
        primaryStage.show();
    }


    private GridPane buildMainGrid(int rows, int cols)
    {
        // creates a new GridPane that will house our board
        GridPane mainGrid = new GridPane();

        // sets the background color to black so we have a nice faux border around us
        mainGrid.setStyle("-fx-background-color:#000;");

        // sets the padding of the main GridPane so there is a border to it (makes it look nice)
        mainGrid.setPadding( new Insets(0, 15, 15, 25) );

        // goes through each tile since we are setting up our board for the first time
        for(int row = 0; row < rows; ++row)
        {
            for (int col = 0; col < cols; ++col)
            {
                // gets our row and column (so they are effectively final)
                int tileRow = row;
                int tileCol = col;

                PlaceTile tile = this.model.getTile(row, col);

                // gets the color of our tile that we are currently setting
                PlaceColor tileColor = tile.getColor();

                // generates our tile as a rectangle
                Rectangle tileRectangle = new Rectangle(RECT_SIZE, RECT_SIZE, Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue()));

                // formats the date
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
                String date = dateFormat.format(new Date(tile.getTime()));
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                String time = timeFormat.format(new Date(tile.getTime()));
                Tooltip t = new Tooltip("("+ tileRow + ", " + tileCol + ")\n" + tile.getOwner() +
                        "\n" + date + "\n" + time);
                // installs the tooltip on the rectangle
                Tooltip.install(tileRectangle, t);

                // sets an event listener to our rectangle to listen for clicks on this tile
                tileRectangle.setOnMouseClicked(
                        (ActionEvent) -> this.serverConn.sendTile(
                            new PlaceTile(tileRow, tileCol, this.username, this.selectedColor, System.currentTimeMillis())
                        )
                );


                // add our tile to the mainGrid ( USING JAVA'S STUPID COLUMN, ROW SYSTEM WHO THE HECK DOES THAT?!?!?! )
                mainGrid.add(tileRectangle, col, row);
            }
        }
        // returns our constructed mainGrid
        return mainGrid;
    }

    private FlowPane buildColorBar()
    {
        FlowPane colorBar = new FlowPane(BOTTOM_GAPS, BOTTOM_GAPS);

        colorBar.setStyle("-fx-background-color: #000;");

        colorBar.setAlignment(Pos.CENTER);

        colorBar.setPadding(new Insets(10, 5, 10, 5));

        // builds all of our color choices into rectangles
        for( PlaceColor color : PlaceColor.values() )
        {
            // creates a new Rectangle Object with size RECT_SIZE/2, RECT_SIZE/2, and bg color according to color
            Rectangle colorChoice = new Rectangle(RECT_SIZE/2, RECT_SIZE/2,
                    Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
            // sets the border stroke to LIGHTGREY
            colorChoice.setStroke(Color.LIGHTGREY);
            // sets the corners to have an arc of 10px
            colorChoice.setArcHeight(5);
            colorChoice.setArcWidth(5);
            // when the color is changes, our selected color is changed to color
            colorChoice.setOnMouseClicked( (EventAction) -> this.setColor(color) );

            // change our cursor to be a POINTER as it's known in the CSS world
            colorChoice.setOnMouseEntered( (EventAction) -> scene.setCursor(Cursor.HAND) );
            // change our cursor back to default when the mouse leaves
            colorChoice.setOnMouseExited( (EventAction) -> scene.setCursor(Cursor.DEFAULT) );

            // adds the rectangle to the colorBar
            colorBar.getChildren().add(colorChoice);
        }

        return colorBar;
    }

    private void setColor(PlaceColor color)
    {
        this.selectedColor = color;
    }

    public void update(Observable o, Object tile)
    {
        // makes sure our Observable is the one we want (if it's not then we are screwed)
        assert o == this.model : "We got updated from the wrong spot.";

        // checks to make sure our tile object is actually a tile (if it's not we redraw the entire board)
        // this is a HUGE time saver so we don't need to re-draw our board every time, just replace a tile.
        if(tile instanceof PlaceTile)
            // if we're all set to do tile-update actions, we perform them now
            this.changeTile( (PlaceTile) tile);
        else
            // otherwise we alert the user that we are going to redraw the board
            System.err.println("Hmmm... Our board has sent us something and we don't know what it is. " +
                    "We will now recreate our board to make sure everything is correct. It should only take a second");
    }

    private void changeTile(PlaceTile tile)
    {
        // get the color of our tile so we can set it properly
        PlaceColor tileColor = tile.getColor();

        // create our new rectangle to be put in place of the old one
        Rectangle changedTile = new Rectangle(RECT_SIZE, RECT_SIZE, Color.rgb(tileColor.getRed(),tileColor.getGreen(),tileColor.getBlue()));

        // set a new event handler
        changedTile.setOnMouseClicked(
                (ActionEvent) -> this.serverConn.sendTile(
                        new PlaceTile(tile.getRow(), tile.getCol(), this.username, this.selectedColor, System.currentTimeMillis())
                )
        );

        // using runLater to join this method with the JavaFX thread
        // set our tile in its correct place
        javafx.application.Platform.runLater( () -> mainGrid.add(changedTile, tile.getCol(), tile.getRow()));
    }

    public void stop() throws Exception
    {
        // when the program closes we close our NetworkClient so it knows to stop executing and log us out
        super.stop();
        // indicates to serverConn that it should close
        this.serverConn.close();
    }

    public static void main(String[] args)
    {
        // makes sure we were given the proper number of arguments, if not, tell the user the proper way to start it
        //
        if(args.length != 3)
        {
            System.err.println("Please run the program as:");
            System.err.println("$ java PlaceGUI --host=(host IP/URL) --port=(port) --user=(username)");
        }
        else
        {
            try
            {
                // attempts to launch the client
                Application.launch(args);
            }
            catch(Exception e)
            {
                System.err.println("We've hit an unrecoverable issue. Please try to launch again.");
                System.err.println( e.getMessage() );
            }
        }
    }
}
