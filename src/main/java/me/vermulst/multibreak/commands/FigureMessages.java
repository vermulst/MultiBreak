package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.figure.Figure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class FigureMessages {



    public static TextComponent getFigureInfoText(Figure figure) {
        int width = figure.getWidth();
        int height = figure.getHeight();
        int depth = figure.getDepth();
        short rotationWidth = figure.getRotationWidth();
        short rotationHeight = figure.getRotationHeight();
        short rotationDepth = figure.getRotationDepth();
        int offSetWidth = figure.getOffSetWidth();
        int offSetHeight = figure.getOffSetHeight();
        int offSetDepth = figure.getOffSetDepth();
        TextComponent newLine = Component.text("\n");
        TextComponent widthN = Component.text(width).color(TextColor.color(255, 255, 85));
        TextComponent heightN = Component.text(height).color(TextColor.color(255, 255, 85));
        TextComponent depthN = Component.text(depth).color(TextColor.color(255, 255, 85));
        TextComponent widthText = Component.text("Width: ").color(TextColor.color(170, 170, 170)).append(widthN);
        TextComponent heightText = Component.text(", Height: ").color(TextColor.color(170, 170, 170)).append(heightN);
        TextComponent depthText = Component.text(", Depth: ").color(TextColor.color(170, 170, 170)).append(depthN);
        TextComponent dimensions = widthText.append(heightText).append(depthText);

        TextComponent comma = Component.text(", ").color(TextColor.color(170, 170, 170));

        TextComponent widthR = Component.text(rotationWidth).color(TextColor.color(255, 255, 85));
        TextComponent heightR = Component.text(rotationHeight).color(TextColor.color(255, 255, 85));
        TextComponent depthR = Component.text(rotationDepth).color(TextColor.color(255, 255, 85));
        TextComponent RText = Component.text("Rotations: ").color(TextColor.color(170, 170, 170));
        TextComponent rotations = newLine.append(RText).append(widthR).append(comma).append(heightR).append(comma).append(depthR);

        TextComponent widthON = Component.text(offSetWidth).color(TextColor.color(255, 255, 85));
        TextComponent heightON = Component.text(offSetHeight).color(TextColor.color(255, 255, 85));
        TextComponent depthON = Component.text(offSetDepth).color(TextColor.color(255, 255, 85));
        TextComponent offsetsText = Component.text("Offsets: ").color(TextColor.color(170, 170, 170));
        TextComponent offsets = newLine.append(offsetsText).append(widthON).append(comma).append(heightON).append(comma).append(depthON);



        return dimensions.append(rotations).append(offsets);
    }

    public static void sendApplyMessage(Player p, Figure figure, boolean global, Material material) {
        TextComponent newLine = Component.text("\n");
        TextComponent successMessage = Component.text("Applied the following multi break attributes:").color(TextColor.color(85, 255, 85));

        TextComponent typeN = Component.text("holding item (" + material.name() + ")").color(TextColor.color(255, 255, 85));
        if (global) {
            typeN = Component.text("all items of type " + material.name()).color(TextColor.color(255, 255, 85));
        }
        TextComponent typeText = Component.text("Type: ").color(TextColor.color(170, 170, 170)).append(typeN);

        TextComponent figureInfo = getFigureInfoText(figure);

        TextComponent total = newLine.append(successMessage).append(newLine).append(newLine).append(typeText).append(newLine).append(newLine).append(figureInfo);

        p.sendMessage(total);
    }

    public static void sendCreateMessage(Player p, Figure figure, String name) {
        TextComponent newLine = Component.text("\n");
        TextComponent successMessage = Component.text("Created a new config with the following attributes:").color(TextColor.color(85, 255, 85));

        TextComponent nameN = Component.text(name).color(TextColor.color(255, 255, 85));
        TextComponent nameText = Component.text("Name: ").color(TextColor.color(170, 170, 170)).append(nameN);

        TextComponent figureInfo = getFigureInfoText(figure);

        TextComponent total = newLine.append(successMessage).append(newLine).append(newLine).append(nameText).append(newLine).append(newLine).append(figureInfo);

        p.sendMessage(total);
    }

    public static void sendDeleteMessage(Player p, Figure figure, String name) {
        TextComponent newLine = Component.text("\n");
        TextComponent successMessage = Component.text("Deleted config named \"" + name + "\":").color(TextColor.color(255, 85, 85));
        TextComponent figureInfo = getFigureInfoText(figure);
        TextComponent total = newLine.append(successMessage).append(newLine).append(newLine).append(figureInfo);
        p.sendMessage(total);
    }

}
