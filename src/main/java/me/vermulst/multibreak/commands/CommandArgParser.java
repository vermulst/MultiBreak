package me.vermulst.multibreak.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

/**
 * Utility class to handle safe parsing of command arguments and provide feedback to the player.
 */
public class CommandArgParser {

    private static final TextColor ERROR_COLOR = TextColor.color(255, 85, 85);
    private final Player p;

    public CommandArgParser(Player p) {
        this.p = p;
    }

    /**
     * Parses a sequence of integer arguments from the args array.
     *
     * @param args The full command arguments array.
     * @param start The starting index (inclusive).
     * @param end The ending index (inclusive).
     * @param failMessages Custom error messages for each possible index failure.
     * @return An array of parsed integers, or null if parsing fails.
     */
    public int[] parseInts(String[] args, int start, int end, String[] failMessages) {
        int length = end - start + 1;
        int[] parsedInts = new int[length];

        for (int i = start; i <= end; i++) {
            // Check for missing arguments
            if (args.length <= i) {
                // If the mandatory dimensions (1, 2, 3) are missing, send a generic error
                if (i <= 3) {
                    p.sendMessage(Component.text("Missing required arguments for dimensions: <width> <height> <depth>").color(ERROR_COLOR));
                    return null;
                }
                // Stop parsing optional arguments if they are missing
                break;
            }

            String arg = args[i];
            try {
                // Index correction: The result array index should be i - start
                parsedInts[i - start] = Integer.parseInt(arg);
            } catch(NumberFormatException e) {
                // Fail messages index must also be i - start
                p.sendMessage(Component.text(failMessages[i - start]).color(ERROR_COLOR));
                return null;
            }
        }
        return parsedInts;
    }

    /**
     * Parses a sequence of short arguments from the args array.
     *
     * @param args The full command arguments array.
     * @param start The starting index (inclusive).
     * @param end The ending index (inclusive).
     * @param failMessages Custom error messages for each possible index failure.
     * @return An array of parsed shorts, or null if parsing fails.
     */
    public short[] parseShorts(String[] args, int start, int end, String[] failMessages) {
        int length = end - start + 1;
        short[] parsedShorts = new short[length];

        for (int i = start; i <= end; i++) {
            if (args.length <= i) break;

            String arg = args[i];
            try {
                // Index correction: The result array index should be i - start
                parsedShorts[i - start] = Short.parseShort(arg);
            } catch(NumberFormatException e) {
                // Fail messages index must also be i - start
                p.sendMessage(Component.text(failMessages[i - start]).color(ERROR_COLOR));
                return null;
            }
        }
        return parsedShorts;
    }
}
