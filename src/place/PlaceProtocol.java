package place;

/**
 * The {@link PlaceProtocol} interface provides constants for all of the
 * messages that are communicated between the place.client's and the
 * place.server.
 *
 * @author Kevin Becker
 */
public interface PlaceProtocol {
    /**
     * Sent from CLIENT to SERVER when they are attempting to login
     *
     * Utilized as "LOGIN <i>username</i>"
     */
    String LOGIN = "LOGIN";

    /**
     * Sent from SERVER to CLIENT when the login attempt was SUCCESSFUL
     *
     * Utilized as "LOGIN_SUCCESSFUL <i>username</i>"
     */
    String LOGIN_SUCCESSFUL = "LOGIN_SUCCESSFUL";

    /**
     * Sent from SERVER to CLIENT when the login attempt was <u>NOT</u> SUCCESSFUL
     *
     * Utilized as "LOGIN_FAILED <i>username</i>"
     */
    String LOGIN_FAILED = "LOGIN_FAILED";

    /**
     * Sent from CLIENT to SERVER when they are exiting so that the server can remove them.
     *
     * Utilized as "EXIT <i>username</i>"
     */
    String LOGOUT = "LOGOUT";

    /**
     * Sent from SERVER to CLIENT after login was successful with the most recent Board so it can generate the
     * correct view.
     *
     * Utilizes the serialization of objects to send the current Board object.
     */
    String BOARD = "BOARD";

    /**
     * Sent from CLIENT to SERVER when it is attempting to change a tile.
     *
     * Utilized as "CHANGE_TILE <i>Serialized PlaceTile object</i>"
     */
    String CHANGE_TILE = "CHANGE_TILE";

    /**
     * Sent from SERVER to CLIENT when it is attempting to change a tile.
     *
     * Utilized as "TILE_CHANGED <i>Serialized PlaceTile object</i>"
     */
    String TILE_CHANGED = "TILE_CHANGED";

    /**
     * Sent from SERVER to CLIENT as a last resort in case there is any FATAL ERROR in the server that requires
     * immediate shutdown.
     */
    String ERROR = "ERROR";
}
