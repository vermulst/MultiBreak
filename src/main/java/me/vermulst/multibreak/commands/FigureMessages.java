package me.vermulst.multibreak.commands;

import me.vermulst.multibreak.figure.Figure;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
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
        TextComponent widthN = Component.text(width, NamedTextColor.YELLOW);
        TextComponent heightN = Component.text(height, NamedTextColor.YELLOW);
        TextComponent depthN = Component.text(depth, NamedTextColor.YELLOW);
        TextComponent.Builder widthText = Component.text()
                .content("Width: ")
                .color(NamedTextColor.GRAY)
                .append(widthN);
        TextComponent heightText = Component.text(", Height: ", NamedTextColor.GRAY).append(heightN);
        TextComponent depthText = Component.text(", Depth: ", NamedTextColor.GRAY).append(depthN);
        TextComponent.Builder dimensions = widthText.append(heightText).append(depthText);

        TextComponent comma = Component.text(", ", NamedTextColor.GRAY);

        TextComponent widthR = Component.text(rotationWidth, NamedTextColor.YELLOW);
        TextComponent heightR = Component.text(rotationHeight, NamedTextColor.YELLOW);
        TextComponent depthR = Component.text(rotationDepth, NamedTextColor.YELLOW);
        TextComponent RText = Component.text("Rotations: ", NamedTextColor.GRAY);
        TextComponent rotations = newLine.toBuilder().append(RText).append(widthR).append(comma).append(heightR).append(comma).append(depthR).build();

        TextComponent widthON = Component.text(offSetWidth, NamedTextColor.YELLOW);
        TextComponent heightON = Component.text(offSetHeight, NamedTextColor.YELLOW);
        TextComponent depthON = Component.text(offSetDepth, NamedTextColor.YELLOW);
        TextComponent offsetsText = Component.text("Offsets: ", NamedTextColor.GRAY);
        TextComponent offsets = newLine.toBuilder().append(offsetsText).append(widthON).append(comma).append(heightON).append(comma).append(depthON).build();

        return dimensions.append(rotations).append(offsets).build();
    }

    public static void sendApplyMessage(Player p, Figure figure, boolean global, Material material) {
        TextComponent newLine = Component.text("\n");
        TextComponent successMessage = Component.text("Applied the following multi break attributes:", NamedTextColor.GREEN);

        TextComponent typeN = global
                ? Component.text("all items of type " + material.name(), NamedTextColor.GREEN)
                : Component.text("holding item (" + material.name() + ")", NamedTextColor.YELLOW);
        TextComponent.Builder typeText = Component.text()
                .content("Type: ")
                .color(NamedTextColor.GRAY)
                .append(typeN);

        TextComponent figureInfo = getFigureInfoText(figure);

        TextComponent total = newLine.toBuilder()
                .append(successMessage).append(newLine).append(newLine)
                .append(typeText).append(newLine).append(newLine)
                .append(figureInfo).build();

        p.sendMessage(total);
    }

    public static void sendCreateMessage(Player p, Figure figure, String name) {
        TextComponent newLine = Component.text("\n");
        TextComponent successMessage = Component.text("Created a new config with the following attributes:", NamedTextColor.GREEN);

        TextComponent nameN = Component.text(name, NamedTextColor.YELLOW);
        TextComponent.Builder nameText = Component.text()
                .content("Name: ")
                .color(NamedTextColor.GRAY)
                .append(nameN);

        TextComponent figureInfo = getFigureInfoText(figure);

        TextComponent total = newLine.toBuilder()
                .append(successMessage).append(newLine).append(newLine)
                .append(nameText).append(newLine).append(newLine)
                .append(figureInfo).build();

        p.sendMessage(total);
    }

    public static void sendDeleteMessage(Player p, Figure figure, String name) {
        TextComponent newLine = Component.text("\n");
        TextComponent successMessage = Component.text("Deleted config named \"" + name + "\":", NamedTextColor.RED);
        TextComponent figureInfo = getFigureInfoText(figure);
        TextComponent total = newLine.toBuilder()
                .append(successMessage).append(newLine).append(newLine)
                .append(figureInfo).build();
        p.sendMessage(total);
    }

}
