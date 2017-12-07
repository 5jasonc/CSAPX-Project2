package place.client.bot;

import place.PlaceColor;

// fully commented

/**
 * The protocol that a Bot must utilize.
 */
public interface BotProtocol {

    // MESSAGES =============================

    /**
     * The PROMPT String that is posted every time a user should provide input.
     */
    String PROMPT = ">>> ";

    /**
     * A message that is sent when the setup of a Bot has been completed.
     */
    String SETUP_COMPLETE_MSG = "Setup is complete. Bot will now start.";

    /**
     * A message that is sent when a Bot is exiting.
     */
    String EXIT_MSG = "Exiting the Bot.";

    /**
     * A message that is sent when a bot is entering pause mode.
     */
    String PAUSE_MSG = "Pausing the Bot. To resume, use \"resume\".";

    /**
     * A message that is sent when a Bot is resuming from pause.
     */
    String RESUME_MSG = "Resuming the bot.";

    /**
     * A message that is sent at the end of an invalid command.
     */
    String INVALID_MSG = "is not a valid command.";

    /**
     * A message that is sent at the end of a bad format command.
     */
    String FORMAT_MSG = "was formatted incorrectly. Try again.";

    /**
     * A message that is sent after a command that isn't known.
     */
    String BAD_CMD_MSG = "is not recognized as a command.";

    // COMMANDS ===================================

    /**
     * The HELP command is used to print the command help manual.
     */
    String HELP = "help";

    /**
     * The EXIT command is used to exit the client.
     */
    String EXIT = "exit";

    /**
     * The WHERE command is used to display where the client is.
     */
    String ABOUT = "about";

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
     * The boolean used to tell if we should stick with the same color or not.
     *
     * If the thread should be in sticky mode, this is true; false otherwise.
     */
    String STICKY = "sticky";

    /**
     * The RAINBOW command is called to make the color which the Bot places change every tile to the next color.
     */
    String RAINBOW = "rainbow";

    /**
     * The RANDOM command is called to make the color which the Bot places random
     */
    String RANDOM = "random";

    // COLOR NUMBERS ====================================

    /**
     * The maximum color we can choose (located at index TOTAL_COLORS - 1)
     */
    int MAX_COLOR = PlaceColor.TOTAL_COLORS - 1;

    /**
     * The minimum color we can choose.
     */
    int MIN_COLOR = 0;

    // SPEED NUMBERS =====================================

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

    // PAUSE CHECKING ====================================

    /**
     * When the Bot is in pause mode, it must sleep for this many milliseconds and then check again.
     */
    int PAUSED_CHECK = 1000;
}
