package place.client.gui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.*;

import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.network.NetworkClient;
import place.PlaceBoardObservable;

/**
 * A GUI client which connects to a PlaceServer.
 *
 * Run on the command line using the following:
 *     <pre>$ java PlaceGUI host port username</pre>
 * to properly log in.
 *
 * @author Kevin Becker (kjb2503)
 */
public class PlaceGUI extends Application implements Observer {
    /**
     * The arc size that is used to curve corners on any rectangle.
     */
    private static final int ARC_SIZE = 5;

    /**
     * The gaps that should be put on the FlowPane between each color choice.
     */
    private static final int BOTTOM_GAPS = 7;

    /**
     * The spacing that is used on the left-hand VBox.
     */
    private static final int LEFT_VBOX_SPACING = 3;

    /**
     * The minimum size of the GridPane.
     */
    private static final int MIN_GRID_SIZE = 850;

    /**
     * The size of all the control items (color picker, selected color, tile info)
     */
    private static final int TILE_PREVIEW_SIZE = 70;

    /**
     * The size of all the control items (color picker, selected color, tile info)
     */
    private static final int COLOR_CONTROL_SIZE = TILE_PREVIEW_SIZE/2;

    /**
     * The radius of the currently selected color preview circle.
     */
    private static final int SELECTED_COLOR_RADIUS = COLOR_CONTROL_SIZE;

    /**
     * The padding Insets for the color bar on the top.
     */
    private static final Insets COLOR_BAR_INSETS = new Insets(10, 5, 10,5);

    /**
     * The padding Insets for the left-hand VBox.
     */
    private static final Insets LEFT_VBOX_INSETS = new Insets(15,0,15,15);

    /**
     * The padding Insets for the main grid.
     */
    private static final Insets MAIN_GRID_INSETS = new Insets(0, 10, 10, 15);

    //===========================================

    /**
     * The username that this user wishes to have.
     */
    private String username;

    /**
     * The model which is used to house the board.
     */
    private PlaceBoardObservable model;

    /**
     * The connection to the server through a NetworkClient.
     */
    private NetworkClient serverConn;

    /**
     * The scene of our GUI.
     */
    private Scene scene;

    /**
     * The main GridPane of rectangles that represent all of the PlaceTiles in the game.
     */
    private GridPane mainGrid;

    /**
     * The currently selected PlaceColor that will be used to send to the server if a PlaceTile is clicked on.
     */
    private int currentColor = 0;

    /**
     * The size that each rectangle should be on screen. It is set to 70px for 70px.
     */
    private int rectSize;

    // SELECTED COLOR =============================

    /**
     * The text that is used to show the name of the currently selected color in the VBox.
     */
    private Text selectedColorName;

    /**
     * A Rectangle which has a preview of the currently selected color.
     */
    private Circle selectedColorPreview;

    // MOST RECENT TILE ==============================

    /**
     * A Rectangle that gets updated with every new tile that is placed
     */
    private Rectangle mostRecentTile;

    /**
     * A Text object that has the location of the most recently placed tile.
     */
    private Text mostRecentLocationInfo;

    /**
     * The Text object that has the owner of the most recently placed tile.
     */
    private Text mostRecentOwnerInfo;

    /**
     * The Text object that has the date on which the most recently placed tile was placed.
     */
    private Text mostRecentCreateDateInfo;

    /**
     * The Text object that has the time at which the most recently placed tile was placed.
     */
    private Text mostRecentCreateTimeInfo;

    // TILE PREVIEW ===============================
    /**
     * The preview circle that is the color of the PlaceTile that the mouse is currently hovering over.
     */
    private Rectangle tilePreview;

    /**
     * The location of the PlaceTile that the mouse is currently over.
     */
    private Text tileLocationInfo;

    /**
     * The owner of the PlaceTile that the mouse is currently over.
     */
    private Text tileOwnerInfo;

    /**
     * The date on which the PlaceTile that the mouse is currently over was set.
     */
    private Text tileCreateDateInfo;

    /**
     * The time when the PlaceTile that the mouse is currently over was set.
     */
    private Text tileCreateTimeInfo;


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

        // gets the raw parameters
        List < String > parameters = super.getParameters().getRaw();

        // gets our parameters that we need (host, port, username)
        String host = parameters.get(0);
        int port = Integer.parseInt(parameters.get(1));
        this.username = parameters.get(2);

        // sets our model to a new blank PlaceBoardObservable
        this.model = new PlaceBoardObservable();

        // this is the most uncertain part, we wrap this in a try so that if it fails we can exit without opening the program
        try
        {
            // sets our network client, this is the last thing we do to minimize time between receiving the board and
            // opening the GUI
            this.serverConn = new NetworkClient(host, port, this.username, getClass().getSimpleName(), this.model);
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
        // add ourselves as an observer of the model
        this.model.addObserver(this);

        this.rectSize = MIN_GRID_SIZE / this.model.getDIM();
    }

    /**
     * Constructs our GUI and then displays it at the end.
     *
     * @param primaryStage the stage that we are showing our GUI upon.
     */
    @Override
    public void start(Stage primaryStage)
    {
        // creates a blank BorderPane
        BorderPane root = new BorderPane();

        // builds our color selection FlowPane
        root.setTop( buildColorBar() );

        // sets our mainGrid in place
        root.setCenter( this.mainGrid = buildMainGrid() );

        // sets the left VBox which houses the selected color, status, and
        root.setLeft( buildLeftVBox() );

        /*
        starts the thread that NetworkClient uses to listen to the server
        this allows the client to start up completely before any tile changes that occurred between launch(args) and
        addObserver(this) are acknowledged
        essentially this creates a sort of Queue in the ObjectInputStream from NetworkClient of any "TILE_CHANGED"s
        the updates will begin immediately after this
        */
        this.serverConn.start();

        // saves our scene
        this.scene = new Scene(root);

        // sets our scene
        primaryStage.setScene(this.scene);

        // sets the title of our window
        primaryStage.setTitle("k/Place: " + this.username);

        // makes it so the user cannot rescale the window
        primaryStage.setResizable(false);

        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("PlaceIcon.png")));
        // we have now completed building our GUI, we can show it
        // anything we change in the GUI after this MUST be used with runLater( ... )
        primaryStage.show();
    }


    /**
     * Builds a GridPane of Rectangle objects which are a visual representation of each PlaceTile.
     *
     * @return a GridPane which is DIM x DIM housing a visual representation of each PlaceTile.
     */
    private GridPane buildMainGrid()
    {
        // creates a new GridPane that will house our board
        GridPane mainGrid = new GridPane();

        // sets the padding of the main GridPane so there is a border to it (makes it look nice)
        mainGrid.setPadding(MAIN_GRID_INSETS);

        // sets the background color to black so we have a nice faux border around us
        mainGrid.setStyle("-fx-background-color:#000;");

        // goes through each tile since we are setting up our board for the first time
        for(int row = 0; row < this.model.getDIM(); ++row)
        {
            for (int col = 0; col < this.model.getDIM(); ++col)
            {
                // gets the tile we are interested in
                PlaceTile tile = this.model.getTile(row, col);

                // builds a rectangle based on the PlaceTile
                Rectangle tileRectangle = buildSingleTile(tile);

                // add our tile to the mainGrid ( USING JAVA'S STUPID COLUMN, ROW SYSTEM WHO THE HECK DOES THAT?!?!?! )
                mainGrid.add(tileRectangle, col, row);
            }
        }
        // returns our constructed mainGrid
        return mainGrid;
    }

    /**
     * Builds a Rectangle object that is a visual representation for a single PlaceTile.
     *
     * @param tile The tile we want to represent.
     *
     * @return A Rectangle that represents the PlaceTile passed.
     */
    private Rectangle buildSingleTile(PlaceTile tile)
    {
        // gets our row and column
        int row = tile.getRow();
        int col = tile.getCol();

        // CREATING NEW TILE ==========================
        // gets the color of our tile that we are currently setting
        PlaceColor tileColor = tile.getColor();

        // generates our tile as a rectangle
        Rectangle tileRectangle = new Rectangle(this.rectSize, this.rectSize,
                Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue()));


        // CREATING EVENT LISTENERS ========================
        // creates a formatter for the date so that it appears as MM/DD/YY on the tile information center
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        // formats the actual information
        String date = dateFormat.format(new Date(tile.getTime()));
        // creates a formatter for the time so that it appears as HH:MM:SS in 24-hour time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        // formats the actual time information
        String time = timeFormat.format(new Date(tile.getTime()));

        // sets an event listener to change the information panel to preview the information of this tile
        tileRectangle.setOnMouseEntered(
                (ActionEvent) ->
                        javafx.application.Platform.runLater(() ->
                        {
                            tileRectangle.setFill(getCurrentColor());
                            this.tilePreview.setFill(
                                    Color.rgb(tileColor.getRed(),tileColor.getGreen(),tileColor.getBlue())
                            );
                            this.tileLocationInfo.setText("(" + row +
                                    "," + col + ")");
                            this.tileOwnerInfo.setText(tile.getOwner());
                            this.tileCreateDateInfo.setText(date);
                            this.tileCreateTimeInfo.setText(time);
                        })
        );

        tileRectangle.setOnMouseExited(
                (ActionEvent) -> javafx.application.Platform.runLater(() ->
                        tileRectangle.setFill(Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue()))
                )
        );

        // sets an event listener to our rectangle to listen for clicks on this tile
        tileRectangle.setOnMouseClicked(
                (ActionEvent) -> this.serverConn.sendTile(
                        new PlaceTile(row, col, this.username, PlaceColor.values()[this.currentColor], System.currentTimeMillis())
                )
        );

        // returns the Rectangle that has been set up to reflect the PlaceTile on the board
        return tileRectangle;
    }

    /**
     * A small method to return a Color object of the currently selected color.
     *
     * @return a Color object of the currently selected color.
     */
    private Color getCurrentColor()
    {
        // gets our currently selected color
        PlaceColor currentSelected = PlaceColor.values()[this.currentColor];
        // returns our current color
        return Color.rgb(currentSelected.getRed(), currentSelected.getGreen(), currentSelected.getBlue());
    }

    /**
     * Builds a color bar which allows the user to select the color they want to send when they click a PlaceTile.
     *
     * @return A FlowPane which houses the color bar.
     */
    private FlowPane buildColorBar()
    {
        // creates a new FlowPane that will house the color choices
        FlowPane colorBar = new FlowPane(BOTTOM_GAPS, BOTTOM_GAPS);

        // sets the padding to be 10, 5, 10, 5
        colorBar.setPadding(COLOR_BAR_INSETS);

        // sets the background of the FlowPane to black (to match the rest of the UI bg)
        colorBar.setStyle("-fx-background-color: #000;");

        // aligns everything true center
        colorBar.setAlignment(Pos.CENTER);

        // builds all of our color choices into rectangles
        for( PlaceColor color : PlaceColor.values())
        {
            // creates a new Rectangle Object with size RECT_SIZE/2, RECT_SIZE/2, and bg color according to color
            Rectangle colorChoice = new Rectangle(COLOR_CONTROL_SIZE, COLOR_CONTROL_SIZE,
                    Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
            // sets the border stroke to DARKGREY
            colorChoice.setStroke(Color.DARKGREY);
            // sets the corners to have an arc of 5px
            colorChoice.setArcHeight(ARC_SIZE);
            colorChoice.setArcWidth(ARC_SIZE);
            // when the color is changes, our selected color is changed to color
            colorChoice.setOnMouseClicked( (EventAction) -> this.setColor(color.getNumber()) );

            // change our cursor to be a POINTER as it's known in the CSS world when entering the rectangle
            colorChoice.setOnMouseEntered( (EventAction) -> scene.setCursor(Cursor.HAND) );
            // change our cursor back to default when the mouse leaves
            colorChoice.setOnMouseExited( (EventAction) -> scene.setCursor(Cursor.DEFAULT) );

            // adds the rectangle to the colorBar
            colorBar.getChildren().add(colorChoice);
        }

        // return our color bar
        return colorBar;
    }

    /**
     * Changes the selected color when a user clicks on a color in the color bar.
     *
     * @param color The color that the user selected.
     */
    private void setColor(int color)
    {
        // sets our current color
        this.currentColor = color;

        // sets our new color so we can use it later
        PlaceColor selectedColor = PlaceColor.values()[color];

        // updates our selected color information
        javafx.application.Platform.runLater(
                () ->
                {
                    this.selectedColorName.setText(selectedColor.name());
                    this.selectedColorPreview.setFill(
                            Color.rgb(selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue())
                    );
                }
         );
    }

    /**
     * Builds the preview VBox which houses the preview of the PlaceTile the mouse is over as well as the currently
     * selected color.
     *
     * @return A VBox which houses the preview of the PlaceTile the mouse is hovering over.
     */
    private VBox buildLeftVBox()
    {
        // creates a new blank VBox
        VBox leftVBox = new VBox();

        // sets the spacing between items in the VBox
        leftVBox.setSpacing( LEFT_VBOX_SPACING );

        // sets the padding on the VBox
        leftVBox.setPadding( LEFT_VBOX_INSETS );

        // sets the background color of the VBox
        leftVBox.setStyle("-fx-background-color:#000;");

        // sets the alignment to top center
        leftVBox.setAlignment(Pos.TOP_CENTER);


        // SELECTED COLOR =======================
        // creates a text object that holds "Selected color" and sets it to white fill
        Text selectedColorPre = new Text("Selected color");

        // builds the selected color portion
        PlaceColor selected = PlaceColor.values()[this.currentColor];
        this.selectedColorName = new Text(selected.name());
        this.selectedColorPreview = new Circle(
                SELECTED_COLOR_RADIUS, Color.rgb(selected.getRed(), selected.getGreen(), selected.getBlue())
        );

        // stylizes our items
        selectedColorPre.setFill(Color.WHITE);
        this.selectedColorName.setFill(Color.WHITE);
        this.selectedColorPreview.setStroke(Color.DARKGREY);
        this.selectedColorPreview.setStrokeWidth(1.5);


        // SPACER ============================
        // builds a new spacer and sets its priority to always
        Region spacer1 = new Region();
        VBox.setVgrow(spacer1, Priority.ALWAYS);


        // CONNECTION STATUS =========================
        // builds our most recent tile indicator
        // creates our text identifying the region
        Text mostRecentHeader = new Text("Most recent (?)");
        // creates a tooltip about the most recent header
        Tooltip mostRecentAbout = new Tooltip("Most recent shows the most recently placed tile."
        );
        // installs the tooltip
        Tooltip.install(mostRecentHeader, mostRecentAbout);

        // creates our most recent tile rectangle
        this.mostRecentTile = new Rectangle(
                TILE_PREVIEW_SIZE, TILE_PREVIEW_SIZE, Color.rgb(selected.getRed(), selected.getGreen(), selected.getBlue())
        );
        // creates our information
        this.mostRecentLocationInfo = new Text("No new");
        this.mostRecentOwnerInfo = new Text("tiles yet.");
        // these two are empty and are only filled once a changed tile has been read in.
        this.mostRecentCreateDateInfo = new Text("");
        this.mostRecentCreateTimeInfo = new Text("");

        // stylizes each of the information bits
        mostRecentHeader.setFill(Color.WHITE);
        this.mostRecentTile.setStroke(Color.DARKGREY);
        this.mostRecentTile.setStrokeWidth(1.5);
        this.mostRecentLocationInfo.setFill(Color.WHITE);
        this.mostRecentOwnerInfo.setFill(Color.WHITE);
        this.mostRecentCreateDateInfo.setFill(Color.WHITE);
        this.mostRecentCreateTimeInfo.setFill(Color.WHITE);


        // SPACER 2 ==========================
        // builds a new spacer and sets its priority to always
        Region spacer2 = new Region();
        VBox.setVgrow(spacer2, Priority.ALWAYS);


        // TILE INFO ============================
        // builds the tile preview
        Text tileInfoHeader = new Text("Tile info (?)");
        // creates a tooltip about the most recent header
        Tooltip tileInfoAbout = new Tooltip("Tile info displays information about the Place tile your mouse is over.");
        // installs the tooltip
        Tooltip.install(tileInfoHeader, tileInfoAbout);

        // creates a new tile preview rectangle.
        this.tilePreview = new Rectangle(
                TILE_PREVIEW_SIZE, TILE_PREVIEW_SIZE, Color.rgb(selected.getRed(), selected.getGreen(), selected.getBlue())
        );
        // creates our tile preview information texts
        this.tileLocationInfo = new Text("(0,0)");
        this.tileOwnerInfo = new Text("Owner");
        this.tileCreateDateInfo = new Text("12/31/69");
        this.tileCreateTimeInfo = new Text("19:00:00");

        // stylizes each of the information bits
        tileInfoHeader.setFill(Color.WHITE);
        this.tilePreview.setStroke(Color.DARKGREY);
        this.tilePreview.setStrokeWidth(1.5);
        this.tileLocationInfo.setFill(Color.WHITE);
        this.tileOwnerInfo.setFill(Color.WHITE);
        this.tileCreateDateInfo.setFill(Color.WHITE);
        this.tileCreateTimeInfo.setFill(Color.WHITE);


        // ADDING TO VBOX ==============================
        leftVBox.getChildren().addAll(
                selectedColorPre,
                this.selectedColorPreview,
                this.selectedColorName,
                spacer1,
                mostRecentHeader,
                this.mostRecentTile,
                this.mostRecentLocationInfo,
                this.mostRecentOwnerInfo,
                this.mostRecentCreateDateInfo,
                this.mostRecentCreateTimeInfo,
                spacer2,
                tileInfoHeader,
                this.tilePreview,
                this.tileLocationInfo,
                this.tileOwnerInfo,
                this.tileCreateDateInfo,
                this.tileCreateTimeInfo
        );

        // return the preview VBox
        return leftVBox;
    }

    /**
     * The update method that is called by an Observable when it has a change it needs to report.
     *
     * @param o The Observable (it attaches itself to make sure we are being updated from the correct model.
     * @param tile The tile that is being sent for update.
     */
    public void update(Observable o, Object tile)
    {
        // makes sure our Observable is the one we want (if it's not then we are screwed)
        assert o == this.model : "We got updated from the wrong Observable.";

        // checks to make sure our tile object is actually a tile (if it's not we redraw the entire board)
        // this is a HUGE time saver so we don't need to re-draw our board every time, just replace a tile.
        if(tile instanceof PlaceTile)
        {
            // if we're all set to do tile-update actions, we perform them now
            changeTile((PlaceTile) tile);
        }
        else
        {
            // in the VERY unlikely event we're sent something weird from PlaceBoardObservable, we redraw the entire board.
            this.serverConn.logErr("Hmmm... Our board has sent us something unusual.\n" +
                    "We will now recreate our board to make sure everything is correct. It should only take a second");
            redrawGrid();
        }
    }

    /**
     * When the update method is called, it is set a PlaceTile which is updated on the game board.
     *
     * @param tile The tile that was changed on the board.
     */
    private void changeTile(PlaceTile tile)
    {
        // CHANGING THE TILE ON SCREEN ===========================
        // using runLater to join this method with the JavaFX thread
        // set our tile in its correct place
        updateMostRecent(tile);
        javafx.application.Platform.runLater(
                () -> this.mainGrid.add(buildSingleTile(tile), tile.getCol(), tile.getRow())
        );
    }

    /**
     * This redraws the entire GridPane in the event the update method is sent something that isn't a PlaceTile.
     */
    private void redrawGrid()
    {
        // goes through each tile since we are setting up our board for the first time
        for (int row = 0; row < this.model.getDIM(); ++row)
        {
            for (int col = 0; col < this.model.getDIM(); ++col)
            {
                // gets the tile we are interested in
                PlaceTile tile = this.model.getTile(row, col);

                // add our tile to the mainGrid ( USING JAVA'S STUPID COLUMN, ROW SYSTEM WHO THE HECK DOES THAT?!?!?! )
                javafx.application.Platform.runLater(() -> mainGrid.add(buildSingleTile(tile), tile.getCol(), tile.getRow()));
            }
        }
    }

    /**
     * Updates the most recently placed tile in the GUI left VBox.
     *
     * @param tile The tile that was most recently placed.
     */
    private void updateMostRecent(PlaceTile tile)
    {
        // gets our color
        PlaceColor tileColor = tile.getColor();

        // creates a formatter for the date so that it appears as MM/DD/YY on the tile information center
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
        // formats the actual information
        String date = dateFormat.format(new Date(tile.getTime()));
        // creates a formatter for the time so that it appears as HH:MM:SS in 24-hour time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        // formats the actual time information
        String time = timeFormat.format(new Date(tile.getTime()));

        javafx.application.Platform.runLater(
                () ->
                {
                    this.mostRecentTile.setFill(Color.rgb(tileColor.getRed(), tileColor.getGreen(), tileColor.getBlue()));
                    this.mostRecentLocationInfo.setText("(" + tile.getRow() + ", " + tile.getCol() + ")");
                    this.mostRecentOwnerInfo.setText(tile.getOwner());
                    this.mostRecentCreateDateInfo.setText(date);
                    this.mostRecentCreateTimeInfo.setText(time);
                }
        );
    }

    /**
     * When the GUI is closed, the stop method is called which closes the connection to the server.
     *
     * @throws Exception If there is any issue while trying to stop the GUI.
     */
    public void stop() throws Exception
    {
        // when the program closes we close our NetworkClient so it knows to stop executing and log us out
        super.stop();
        // indicates to serverConn that it should close
        this.serverConn.close();
    }

    /**
     * Launches a new PlaceGUI.
     *
     * @param args The arguments that the GUI should be built with.
     *             args should have: host, port, and username in that order.
     */
    public static void main(String[] args)
    {
        // makes sure we were given the proper number of arguments, if not, tell the user the proper way to start it
        if(args.length != 3)
        {
            System.err.println("Please run the GUI as:");
            System.err.println("$ java PlaceGUI host port username");
            return;
        }

        try
        {
            // attempts to launch the client
            Application.launch(args);
        }
        catch(Exception e)
        {
            // tells the user if we hit something totally unrecoverable
            System.err.println("We've hit an unrecoverable issue. Please try to launch again.");
        }
    }
}
