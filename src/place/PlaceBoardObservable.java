package place;

import java.util.Observable;

/**
 * Wraps the PlaceBoard so it can be an Observable
 */
public class PlaceBoardObservable extends Observable
{
    private PlaceBoard board;

    private int DIM;

    /**
     * Default constructor that just creates an instance of the PlaceBoardObservable.
     */
    public PlaceBoardObservable()
    {
        // haha you have been bamboozled into thinking there was something we needed to do here!
        // you were incorrect in that assumption
        // creates a new PlaceBoard with nothing set (gets set later)
    }

    /**
     * Initializes the board once it has been received by the NetworkClient.
     *
     * @param board The PlaceBoard that the server sends to the client.
     */
    public void initializeBoard(PlaceBoard board)
    {
        // sets our board
        this.board = board;
        // sets our DIM
        this.DIM = this.board.DIM;
    }

    /**
     * Getter method that the client uses to get the square dimension of the PlaceBoard.
     *
     * @return The square dimension of the PlaceBoard.
     */
    public int getDIM()
    {
        // returns the DIM
        return this.DIM;
    }

    /**
     * Getter method that returns a PlaceTile for that position on th board.
     *
     * @param row The row to get the tile from.
     * @param col The column to get the tile from.
     *
     * @return A PlaceTile that is located at (row, col) on the PlaceBoard.
     */
    public PlaceTile getTile(int row, int col)
    {
        // gets a tile at a certain row, col
        return this.board.getTile(row, col);
    }

    /**
     * When a PlaceTile gets changed this method is invoked so that observers can update their view. (The "model" aspect
     * of Place).
     *
     * @param tile The tile that was changed. It gets sent along with NotifyObservers so that the observers can update
     *             just a single tile rather than having to recreate their entire view. (Comes in as the Object in
     *             update(Observable, Object);
     */
    public void tileChanged(PlaceTile tile)
    {
        // SETTING PHASE ==============================
        // sets the new tile
        this.board.setTile(tile);

        // NOTIFICATION PHASE ==============================
        // set changed
        super.setChanged();
        // notify the observer that THIS SPECIFIC TILE HAS CHANGED (saves compute time)
        super.notifyObservers(tile);
    }
}
