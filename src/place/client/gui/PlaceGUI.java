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

import place.client.PlaceClient;
import place.PlaceBoardObservable;

public class PlaceGUI extends Application implements Observer {

    private BorderPane root;

    private GridPane mainGrid;

    private List<String> parameters;

    private PlaceClient client;

    private String username;

    private PlaceBoardObservable model;

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

        this.mainGrid = buildMainGrid(this.model.getDIM(), this.model.getDIM(), 90);

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
                int red = this.model.getTile(row,col).getColor().getRed();
                int green = this.model.getTile(row,col).getColor().getGreen();
                int blue = this.model.getTile(row,col).getColor().getBlue();

                Rectangle tile = new Rectangle(size, size, Color.rgb(red, green, blue));

                mainGrid.add(tile, col, row);
            }
        return mainGrid;
    }

    public void update(Observable o, Object t)
    {
        assert o == this.model : "Update call from non-model";

        this.refresh();
    }

    private void refresh()
    {
        // squash
    }

    public void stop() throws Exception
    {
        super.stop();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
