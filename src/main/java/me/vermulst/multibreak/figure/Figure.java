package me.vermulst.multibreak.figure;

import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.figure.types.FigureType;
import org.bukkit.util.Vector;

import java.util.HashSet;

public abstract class Figure {

    private final int width;
    private final int height;
    private final int depth;

    private int offSetWidth = 0;
    private int offSetHeight = 0;
    private int offSetDepth = 0;


    public Figure(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    public abstract HashSet<Vector> getVectors(Vector playerDirVector, CompassDirection playerDirection);
    public abstract FigureType getFigureType();


    public void setOffsets(int offSetWidth, int offSetHeight, int offSetDepth) {
        this.offSetWidth = offSetWidth;
        this.offSetHeight = offSetHeight;
        this.offSetDepth = offSetDepth;
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
}
