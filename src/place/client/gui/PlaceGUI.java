package place.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

import place.PlaceBoard;
import place.network.PlaceRequest;
import place.network.PlaceRequest.RequestType;

public class PlaceGUI extends Application {

    private BorderPane root;
    private GridPane mainGrid;

    private List<String> parameters;

    private String username;

    private PlaceBoard board;

    private Socket serverConn;

    private ObjectInputStream in;

    private ObjectOutputStream out;

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

        // connects to the server
        this.serverConn = new Socket(host, port);

        // sets the in and out streams
        this.in = new ObjectInputStream( serverConn.getInputStream() );
        this.out = new ObjectOutputStream( serverConn.getOutputStream() );

        // send out our login place request immediately
        out.writeObject(new PlaceRequest<>(RequestType.LOGIN, this.username));

        try
        {
            // read in our first object (should be PlaceRequest<String> with username or ERROR)
            PlaceRequest<?> request = ( PlaceRequest<?> ) in.readObject();

            switch(request.getType())
            {
                case LOGIN_SUCCESS:
                    System.out.println("Successfully joined Place server as \"" + request.getData() + "\".");
                    this.go = true;
                    break;
                case ERROR:
                    System.out.println("Failed to join Place server. Reason: " + request.getData() + ". Terminating.");
                    Platform.exit();
                    break;
                default:
                    System.out.println("Unknown response received. Terminating.");
                    Platform.exit();
                    // unknown response, terminate
            }


            if(this.go)
            {
                // read in our next object
                request = ( PlaceRequest<?> ) in.readObject();

                // read in our board checking to make sure it really is a board
                if(request.getType() == RequestType.BOARD)
                    this.board = (PlaceBoard) request.getData();
            }

            // read our second object (should be PlaceRequest<PlaceBoard>)
        }
        catch (IOException e)
        {

        }

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

        this.mainGrid = buildMainGrid(this.board.DIM, this.board.DIM, 90);

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
                int red = this.board.getTile(row,col).getColor().getRed();
                int green = this.board.getTile(row,col).getColor().getGreen();
                int blue = this.board.getTile(row,col).getColor().getBlue();

                Rectangle tile = new Rectangle(size, size, Color.rgb(red, green, blue));

                mainGrid.add(tile, col, row);
            }
        return mainGrid;
    }

    public void stop() throws Exception
    {
        super.stop();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
