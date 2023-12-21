package me.vermulst.multibreak.figure;

import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.figure.types.FigureType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public abstract class Figure {

    private final int width;
    private final int height;
    private final int depth;

    private int offSetWidth = 0;
    private int offSetHeight = 0;
    private int offSetDepth = 0;

    private short rotationWidth = 0;
    private short rotationHeight = 0;
    private short rotationDepth = 0;

    public Figure(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public abstract HashSet<Vector> getVectors(boolean rotated);
    public abstract FigureType getFigureType();


    public void setOffsets(int offSetWidth, int offSetHeight, int offSetDepth) {
        this.offSetWidth = offSetWidth;
        this.offSetHeight = offSetHeight;
        this.offSetDepth = offSetDepth;
    }

    public void setRotations(short rotationWidth, short rotationHeight, short rotationDepth) {
        this.rotationWidth = rotationWidth;
        this.rotationHeight = rotationHeight;
        this.rotationDepth = rotationDepth;
    }

    public ArrayList<Component> getLore() {
        ArrayList<Component> lore = new ArrayList<>();


        Component widthC = Component.text("Width: ").color(TextColor.color(170, 170, 170));
        Component heightC = Component.text("Height: ").color(TextColor.color(170, 170, 170));
        Component depthC = Component.text("Depth: ").color(TextColor.color(170, 170, 170));

        lore.add(Component.empty());
        lore.add(Component.text("Size")
                .color(TextColor.color(255, 255, 255))
                .decoration(TextDecoration.UNDERLINED, true)
                .decoration(TextDecoration.BOLD, true));
        lore.add(widthC.append(Component.text(this.getWidth()).color(TextColor.color(255, 255, 85))));
        lore.add(heightC.append(Component.text(this.getHeight()).color(TextColor.color(255, 255, 85))));
        lore.add(depthC.append(Component.text(this.getDepth()).color(TextColor.color(255, 255, 85))));


        lore.add(Component.empty());
        lore.add(Component.text("Rotations")
                .color(TextColor.color(255, 255, 255))
                .decoration(TextDecoration.UNDERLINED, true));

        lore.add(widthC.append(Component.text(this.getRotationWidth()).color(TextColor.color(255, 255, 85))));
        lore.add(heightC.append(Component.text(this.getRotationHeight()).color(TextColor.color(255, 255, 85))));
        lore.add(depthC.append(Component.text(this.getRotationDepth()).color(TextColor.color(255, 255, 85))));

        lore.add(Component.empty());
        lore.add(Component.text("Offsets")
                .color(TextColor.color(255, 255, 255))
                .decoration(TextDecoration.UNDERLINED, true));

        lore.add(widthC.append(Component.text(this.getOffSetWidth()).color(TextColor.color(255, 255, 85))));
        lore.add(heightC.append(Component.text(this.getOffSetHeight()).color(TextColor.color(255, 255, 85))));
        lore.add(depthC.append(Component.text(this.getOffSetDepth()).color(TextColor.color(255, 255, 85))));

        return lore;
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public int getOffSetWidth() {
        return offSetWidth;
    }

    public int getOffSetHeight() {
        return offSetHeight;
    }

    public int getOffSetDepth() {
        return offSetDepth;
    }

    public short getRotationWidth() {
        return rotationWidth;
    }

    public short getRotationHeight() {
        return rotationHeight;
    }

    public short getRotationDepth() {
        return rotationDepth;
    }
}
