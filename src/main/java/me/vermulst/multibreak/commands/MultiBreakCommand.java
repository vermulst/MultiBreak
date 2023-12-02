package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.item.FigureItemInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
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
            int width;
            int height;
            int depth;
            try {
                width = Integer.parseInt(args[0]);
                height = Integer.parseInt(args[1]);
                depth = Integer.parseInt(args[2]);
            } catch(Exception e) {
                p.sendMessage(Component.text("Please enter valid width, height and depth").color(TextColor.color(255, 85, 85)));
                return true;
            }
            int offSetWidth = 0;
            int offSetHeight = 0;
            int offSetDepth = 0;
            if (args.length >= 4) {
                try {
                    offSetWidth = Integer.parseInt(args[3]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid offset for Width").color(TextColor.color(255, 85, 85)));
                    return true;
                }
            }
            if (args.length >= 5) {
                try {
                    offSetHeight = Integer.parseInt(args[4]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid offset for Height").color(TextColor.color(255, 85, 85)));
                    return true;
                }

            }
            if (args.length >= 6) {
                try {
                    offSetDepth = Integer.parseInt(args[5]);
                } catch (Exception e) {
                    p.sendMessage(Component.text("Not a valid offset for Depth").color(TextColor.color(255, 85, 85)));
                    return true;
                }
            }
            Figure figure = new Figure(width, height, depth);
            figure.setOffsets(offSetWidth, offSetHeight, offSetDepth);
            FigureItemInfo figureItemInfo = new FigureItemInfo(figure);
            FigureItemDataType figureItemDataType = new FigureItemDataType(this.plugin);
            p.getInventory().setItemInMainHand(figureItemDataType.set(item, figureItemInfo));
            Component newLine = Component.text("\n");
            Component successMessage = Component.text("Applied the following multi break attributes:").color(TextColor.color(85, 255, 85));

            Component widthN = Component.text(width).color(TextColor.color(255, 255, 85));
            Component heightN = Component.text(height).color(TextColor.color(255, 255, 85));
            Component depthN = Component.text(depth).color(TextColor.color(255, 255, 85));
            Component widthText = Component.text("Width: ").color(TextColor.color(170, 170, 170)).append(widthN);
            Component heightText = Component.text(", Height: ").color(TextColor.color(170, 170, 170)).append(heightN);
            Component depthText = Component.text(", Depth: ").color(TextColor.color(170, 170, 170)).append(depthN);
            Component dimensions = newLine.append(widthText).append(heightText).append(depthText);

            Component widthON = Component.text(offSetWidth).color(TextColor.color(255, 255, 85));
            Component heightON = Component.text(offSetHeight).color(TextColor.color(255, 255, 85));
            Component depthON = Component.text(offSetDepth).color(TextColor.color(255, 255, 85));
            Component comma = Component.text(", ").color(TextColor.color(170, 170, 170));
            Component offsetsText = Component.text("Offsets: ").color(TextColor.color(170, 170, 170));
            Component offsets = newLine.append(offsetsText).append(widthON).append(comma).append(heightON).append(comma).append(depthON);

            p.sendMessage(newLine.append(successMessage).append(newLine).append(dimensions).append(offsets));
            return true;
        }
        return false;
    }
}
