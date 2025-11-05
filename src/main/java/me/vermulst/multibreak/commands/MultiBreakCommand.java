package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import me.vermulst.multibreak.item.FigureItemDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class MultiBreakCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            return false;
        }
        if (args.length == 0) {
            p.sendMessage(Component.text("Usage: /multibreak <type|remove> [dimensions...]").color(TextColor.color(255, 85, 85)));
            return true;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getItemMeta() == null) {
            p.sendMessage(Component.text("You are not holding an item").color(TextColor.color(255, 85, 85)));
            return true;
        }

        if ("remove".equals(args[0])) {
            this.handleRemoveCommand(p, item);
        } else {
            this.handleSetCommand(p, item, args);
        }

        return true;
    }

    private void handleRemoveCommand(Player p, ItemStack item) {
        FigureItemDataType figureItemDataType = new FigureItemDataType();
        if (!figureItemDataType.has(item)) {
            p.sendMessage(Component.text("The item in your hand is not a MultiBreak item").color(TextColor.color(255, 85, 85)));
            return;
        }
        p.getInventory().setItemInMainHand(figureItemDataType.remove(item));
        p.sendMessage(Component.text("Removed MultiBreak data from the item").color(TextColor.color(85, 255, 85)));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }

    private void handleSetCommand(Player p, ItemStack item, String[] args) {
        FigureType figureType;
        try {
            figureType = FigureType.valueOf(args[0]);
        } catch (Exception e) {
            p.sendMessage(Component.text("Please enter a valid figure type").color(TextColor.color(255, 85, 85)));
            return;
        }

        /** Dimensions */
        CommandArgParser commandArgParser = new CommandArgParser(p);
        String[] failMessages = new String[]{
                "Please enter valid width, height and depth",
                "Please enter valid width, height and depth",
                "Please enter valid width, height and depth"
        };
        int[] dimensions = commandArgParser.parseInts(args, 1, 3, failMessages);
        int width = dimensions != null ? dimensions[0] : 0;
        int height = dimensions != null ? dimensions[1] : 0;
        int depth = dimensions != null ? dimensions[2] : 0;


        /** Rotations */
        failMessages = new String[]{
                "Not a valid rotation for Width",
                "Not a valid rotation for Height",
                "Not a valid rotation for Depth"
        };
        short[] rotations = commandArgParser.parseShorts(args, 4, 6, failMessages);
        short rotationWidth = rotations != null ? rotations[0] : 0;
        short rotationHeight = rotations != null ? rotations[1] : 0;
        short rotationDepth = rotations != null ? rotations[2] : 0;

        /** Offsets */
        failMessages = new String[]{
                "Not a valid offset for Width",
                "Not a valid offset for Height",
                "Not a valid offset for Depth"
        };
        int[] offsets = commandArgParser.parseInts(args, 7, 9, failMessages);
        int offsetWidth = offsets != null ? offsets[0] : 0;
        int offsetHeight = offsets != null ? offsets[1] : 0;
        int offsetDepth = offsets != null ? offsets[2] : 0;

        Figure figure = figureType.build(width, height, depth);
        figure.setRotations(rotationWidth, rotationHeight, rotationDepth);
        figure.setOffsets(offsetWidth, offsetHeight, offsetDepth);
        FigureItemDataType figureItemDataType = new FigureItemDataType();
        p.getInventory().setItemInMainHand(figureItemDataType.set(item, figure));
        FigureMessages.sendApplyMessage(p, figure, false, item.getType());
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
    }

}
