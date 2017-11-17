package place;

public interface PlaceProtocol {

    /**
     * The {@link ReversiProtocol} interface provides constants for all of the
     * messages that are communicated between the reversi.server and the
     * reversi.client's.
     *
     * Changes suggested for the future by JEH
     * <ul>
     *     <li>Include player no. in the MOVE messages.</li>
     *     <li>Instead of reporting a player's move, report board updates.</li>
     *     <li>This includes initial setup moves.</li>
     * </ul>
     *
     * @author Robert St Jacques @ RIT SE
     * @author Sean Strout @ RIT CS
     */
    public interface ReversiProtocol {
        /**
         * Request sent from the reversi.server to the client after the client initially
         * opens a {@link java.net.Socket} connection to the reversi.server. This is the
         * first part of the handshake used to establish that the client
         * understands the {@link ReversiProtocol protocol}.  The dimensions
         * of the board are sent in the request.<P>
         *
         *  For example if there were 6 rows and 7 columns: CONNECT 6 7\n
         */
        public static final String CONNECT = "CONNECT";

        /**
         * Request sent from the reversi.server to the client when it is the client's turn
         * to make a move.
         */
        public static final String MAKE_MOVE = "MAKE_MOVE";

        /**
         * Response sent from the client to the reversi.server in response to a
         * {@link #MAKE_MOVE} request. The response should include the row and column
         * number into which the player would like to move.<P>
         *
         * For example (to move in (3,2)): MOVE 3 2\n
         */
        public static final String MOVE = "MOVE";

        /**
         * Request sent from the reversi.server to the client when either player has moved.
         * The request will include the row and column in which the player moved.<P>
         *
         * For example (if a move was made in (3,2)): MOVE_MADE 3 2\n
         */
        public static final String MOVE_MADE = "MOVE_MADE";

        /**
         * Request sent from the reversi.server to the client when the client has won the
         * game.
         */
        public static final String GAME_WON = "GAME_WON";

        /**
         * Request sent from the reversi.server to the client when the client has lost the
         * game.
         */
        public static final String GAME_LOST = "GAME_LOST";

        /**
         * Request sent from the reversi.server to the client when the client has tied the
         * game.
         */
        public static final String GAME_TIED = "GAME_TIED";

        /**
         * Request sent from the reversi.server to the client when any kind of error has
         * resulted from a bad client response. No response is expected from the
         * client and the connection is terminated (as is the game).
         */
        public static final String ERROR = "ERROR";
    }

}
