package place.server;

import place.PlaceBoard;

import java.util.Observable;

/**
 * Wraps the PlaceBoard so it can be an Observable
 */
public class PlaceBoardObservable extends Observable
{
    private PlaceBoard placeBoard;

    public PlaceBoardObservable(int dim)
    {
        // creates a new PlaceBoard
        this.placeBoard = new PlaceBoard(dim);
    }

    public PlaceBoard getPlaceBoard()
    {
        return this.placeBoard;
    }
}
