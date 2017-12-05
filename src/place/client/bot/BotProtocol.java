package place.client.bot;

import place.PlaceColor;

/**
 * The protocol that a Bot must utilize.
 */
public interface BotProtocol {
    /**
     * The PROMPT String that is posted every time a user should provide input.
     */
    String PROMPT = ">>> ";

    /**
     * The HELP command is used to print the command help manual.
     */
    String HELP = "help";

    /**
     * The EXIT command is used to exit the client.
     */
    String EXIT = "exit";

    /**
     * The QUIT command is the same as the exit command.
     */
    String QUIT = "quit";

    /**
     * The PAUSE command causes the Bot to pause where it is.
     */
    String PAUSE = "pause";

    /**
     * The STOP is the same as the PAUSE.
     *
     * It is intentionally a misnomer, but just in case a user wants to PAUSE and they type "stop" it still works.
     */
    String STOP = "stop";

    /**
     * The RESUME command resumes the Bot's action.
     */
    String RESUME = "resume";

    /**
     * The PLAY command does the same action as the RESUME command.
     */
    String PLAY = "play";

    /**
     * The SPEED command changes the number of milliseconds the Bot waits between each PlaceTile place.
     */
    String SPEED = "speed";

    /**
     * The maximum color we can choose (located at index TOTAL_COLORS - 1)
     */
    int MAX_COLOR = PlaceColor.TOTAL_COLORS - 1;

    /**
     * The minimum number of milliseconds to wait between each place of a PlaceTile.
     */
    int MAX_SPEED = 3600;

    /**
     * The default number of milliseconds to wait between each place of a PlaceTile.
     */
    int DEFAULT_SPEED = 1000;

    /**
     * The maximum number of milliseconds to wait between each place of a PlaceTile.
     */
    int MIN_SPEED = 600;
}
