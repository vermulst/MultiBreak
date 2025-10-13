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

    private final Plugin plugin;
    public MultiBreakCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item.getItemMeta() == null) {
                p.sendMessage(Component.text("You are not holding an item").color(TextColor.color(255, 85, 85)));
                return true;
            }
            FigureType figureType;
            try {
                figureType = FigureType.valueOf(args[0]);
            } catch (Exception e) {
                p.sendMessage(Component.text("Please enter a valid figure type").color(TextColor.color(255, 85, 85)));
                return true;
            }

            String[] failMessages = new String[]{
                    "Please enter valid width, height and depth",
                    "Please enter valid width, height and depth",
                    "Please enter valid width, height and depth"
            };
            int[] dimensions = this.parseInts(args, 1, 3, failMessages, p);
            int width = dimensions != null ? dimensions[0] : 0;
            int height = dimensions != null ? dimensions[1] : 0;
            int depth = dimensions != null ? dimensions[2] : 0;


            failMessages = new String[]{
                    "Not a valid rotation for Width",
                    "Not a valid rotation for Height",
                    "Not a valid rotation for Depth"
            };
            short[] rotations = this.parseShorts(args, 4, 6, failMessages, p);
            short rotationWidth = rotations != null ? rotations[0] : 0;
            short rotationHeight = rotations != null ? rotations[1] : 0;
            short rotationDepth = rotations != null ? rotations[2] : 0;

            failMessages = new String[]{
                    "Not a valid offset for Width",
                    "Not a valid offset for Height",
                    "Not a valid offset for Depth"
            };
            int[] offsets = this.parseInts(args, 7, 9, failMessages, p);
            int offsetWidth = offsets != null ? offsets[0] : 0;
            int offsetHeight = offsets != null ? offsets[1] : 0;
            int offsetDepth = offsets != null ? offsets[2] : 0;

            Figure figure = figureType.build(width, height, depth);
            figure.setRotations(rotationWidth, rotationHeight, rotationDepth);
            figure.setOffsets(offsetWidth, offsetHeight, offsetDepth);
            FigureItemDataType.FigureItemInfo figureItemInfo = new FigureItemDataType.FigureItemInfo(figure);
            FigureItemDataType figureItemDataType = new FigureItemDataType(this.plugin);
            p.getInventory().setItemInMainHand(figureItemDataType.set(item, figureItemInfo));
            FigureMessages.sendApplyMessage(p, figure, false, item.getType());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            return true;
        }
        return false;
    }


    private int[] parseInts(String[] args, int start, int end, String[] failMessages, Player p) {
        int[] parsedInts = new int[end - start];
        for (int i = start; i <= end; i++) {
            if (args.length < i) continue;
            String arg = args[i];
            try {
                parsedInts[i] = Integer.parseInt(arg);
            } catch(Exception e) {
                p.sendMessage(Component.text(failMessages[i]).color(TextColor.color(255, 85, 85)));
                return null;
            }
        }
        return parsedInts;
    }

    private short[] parseShorts(String[] args, int start, int end, String[] failMessages, Player p) {
        short[] parsedShorts = new short[end - start];
        for (int i = start; i <= end; i++) {
            if (args.length < i) continue;
            String arg = args[i];
            try {
                parsedShorts[i] = Short.parseShort(arg);
            } catch(Exception e) {
                p.sendMessage(Component.text(failMessages[i]).color(TextColor.color(255, 85, 85)));
                return null;
            }
        }
        return parsedShorts;
    }



}
