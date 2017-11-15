package place.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class PlaceClient extends Application {

    private BorderPane root;
    private GridPane mainGrid;

    /**
     * Initializes the place.client before a build of the GUI.
     */
    @Override
    public void init() throws Exception
    {
        super.init();

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

        this.mainGrid = buildMainGrid(10, 10, 90);

        // sets just a rectangle
        this.root.setCenter(mainGrid);

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
                mainGrid.add(new Rectangle(size, size, genRandomColor()), row, col);
            }
        return mainGrid;
    }

    private Color genRandomColor()
    {
        String first = Integer.toHexString((int) (Math.random() * 32));
        String color = Integer.toHexString((int) (Math.random() * 32)) + Integer.toHexString((int) (Math.random() * 32))
                + Integer.toHexString((int) (Math.random() * 32));

        return Color.web('#'+color);
    }

    public void stop() throws Exception
    {
        super.stop();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
