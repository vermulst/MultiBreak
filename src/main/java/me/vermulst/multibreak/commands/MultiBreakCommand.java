package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.item.FigureItemInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
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
            int width;
            int height;
            int depth;
            try {
                width = Integer.parseInt(args[1]);
                height = Integer.parseInt(args[2]);
                depth = Integer.parseInt(args[3]);
            } catch(Exception e) {
                p.sendMessage(Component.text("Please enter valid width, height and depth").color(TextColor.color(255, 85, 85)));
                return true;
            }
            short rotationWidth = 0;
            short rotationHeight = 0;
            short rotationDepth = 0;
            if (args.length >= 5) {
                try {
                    rotationWidth = Short.parseShort(args[4]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid rotation for Width").color(TextColor.color(255, 85, 85)));
                    return true;
                }
            }
            if (args.length >= 6) {
                try {
                    rotationHeight = Short.parseShort(args[5]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid rotation for Height").color(TextColor.color(255, 85, 85)));
                    return true;
                }

            }
            if (args.length >= 7) {
                try {
                    rotationDepth = Short.parseShort(args[6]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid rotation for Depth").color(TextColor.color(255, 85, 85)));
                    return true;
                }
            }

            int offSetWidth = 0;
            int offSetHeight = 0;
            int offSetDepth = 0;
            if (args.length >= 8) {
                try {
                    offSetWidth = Integer.parseInt(args[7]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid offset for Width").color(TextColor.color(255, 85, 85)));
                    return true;
                }
            }
            if (args.length >= 9) {
                try {
                    offSetHeight = Integer.parseInt(args[8]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid offset for Height").color(TextColor.color(255, 85, 85)));
                    return true;
                }

            }
            if (args.length >= 10) {
                try {
                    offSetDepth = Integer.parseInt(args[9]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid offset for Depth").color(TextColor.color(255, 85, 85)));
                    return true;
                }
            }
            Figure figure = figureType.build(width, height, depth);
            figure.setRotations(rotationWidth, rotationHeight, rotationDepth);
            figure.setOffsets(offSetWidth, offSetHeight, offSetDepth);
            FigureItemInfo figureItemInfo = new FigureItemInfo(figure);
            FigureItemDataType figureItemDataType = new FigureItemDataType(this.plugin);
            p.getInventory().setItemInMainHand(figureItemDataType.set(item, figureItemInfo));
            FigureMessages.sendApplyMessage(p, figure, false, item.getType());
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            return true;
        }
        return false;
    }


}
