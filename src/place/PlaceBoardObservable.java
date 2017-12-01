package place;

import java.util.Observable;

/**
 * Wraps the PlaceBoard so it can be an Observable
 */
public class PlaceBoardObservable extends Observable
{
    private PlaceBoard board;

    private int DIM;

    public PlaceBoardObservable()
    {
        // creates a new PlaceBoard with nothing set (gets set later)
    }

    public void initializeBoard(PlaceBoard board)
    {
        this.board = board;
        this.DIM = this.board.DIM;
    }

    public int getDIM()
    {
        return this.DIM;
    }

    public PlaceTile getTile(int row, int col)
    {
        return this.board.getTile(row, col);
    }

    public void tileChanged(PlaceTile tile)
    {
        this.board.setTile(tile);

        // set changed
        super.setChanged();
        // notify the observer that THIS SPECIFIC TILE HAS CHANGED (saves compute time)
        super.notifyObservers(tile);
    }

    public PlaceBoard getPlaceBoard()
    {
        return this.board;
    }
}
