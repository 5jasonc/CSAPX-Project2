package place.client.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import place.PlaceColor;
import place.PlaceException;
import place.PlaceTile;
import place.client.PlaceClient;
import place.PlaceBoardObservable;

public class PlaceGUI extends Application implements Observer {

    private BorderPane root;

    private GridPane mainGrid;

    private List<String> parameters;

    private PlaceClient client;

    private String username;

    private PlaceBoardObservable model;

    private int rectSize = 90;

    private PlaceColor selectedColor = PlaceColor.AQUA;

    private boolean go = false;

    /**
     * Initializes the place.client before a build of the GUI.
     */
    @Override
    public void init() throws Exception
    {
        super.init();
        this.parameters = super.getParameters().getRaw();

        // 0 => host, 1 => port, 2 => username

        String host = parameters.get(0);
        int port = Integer.parseInt(parameters.get(1));
        this.username = parameters.get(2);

        this.model = new PlaceBoardObservable();

        this.client = new PlaceClient(host, port, username, model);
    }

    /**
     * Constructs our GUI and then displays it at the end
     *
     * @param mainStage the stage that we are showing our GUI upon.
     */
    @Override
    public void start(Stage mainStage) throws Exception
    {
        this.root = new BorderPane();

        this.mainGrid = buildMainGrid(this.model.getDIM(), this.model.getDIM(), this.rectSize);

        // sets just a rectangle
        this.root.setCenter(mainGrid);

        this.model.addObserver(this);

        // sets our scene
        mainStage.setScene(new Scene(this.root));

        // we have now completed building our GUI, we can show it
        mainStage.show();
    }

    public GridPane buildMainGrid(int rows, int cols, int size)
    {
        GridPane mainGrid = new GridPane();

        for(int row = 0; row < rows; ++row)
            for(int col = 0; col < cols; ++col)
            {
                PlaceTile representative = this.model.getTile(row, col);
                int r = representative.getColor().getRed();
                int g = representative.getColor().getGreen();
                int b = representative.getColor().getBlue();

                Rectangle tile = new Rectangle(size, size, Color.rgb(r, g, b));

                int tileRow = row;
                int tileCol = col;

                tile.setOnMouseClicked( (ActionEvent) -> this.client.sendTile(new PlaceTile(tileRow, tileCol, this.username, this.selectedColor, System.nanoTime())));

                mainGrid.add(tile, col, row);
            }
        return mainGrid;
    }

    public void update(Observable o, Object tile)
    {
        assert o == this.model : "Update call from non-model";

        if(tile instanceof PlaceTile)
            this.refresh( (PlaceTile) tile);
        else
            System.err.println("Did not receive a tile to update... going to redraw board.");
    }

    private void refresh(PlaceTile tile)
    {
        int row = tile.getRow();
        int col = tile.getCol();

        int r = tile.getColor().getRed();
        int g = tile.getColor().getGreen();
        int b = tile.getColor().getBlue();

        Rectangle newTile = new Rectangle(this.rectSize, this.rectSize, Color.rgb(r,g,b));

        javafx.application.Platform.runLater( () -> mainGrid.add(newTile, col, row));
    }

    public void stop() throws Exception
    {
        super.stop();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
