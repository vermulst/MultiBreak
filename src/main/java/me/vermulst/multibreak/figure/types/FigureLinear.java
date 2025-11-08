package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.FigureIterable;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class FigureLinear extends Figure {

    public FigureLinear(int width, int height, int depth) {
        super(width, height, depth);
    }

    @Override
    public Set<Vector> getVectors(boolean rotated) {
        Set<Vector> vectors = new HashSet<>();

        int[] widthBound = getBoundPair(this.getWidth(), this.getOffSetWidth());
        int[] heightBound = getBoundPair(this.getHeight(), this.getOffSetHeight());
        int[] depthBound = getBoundPair(this.getDepth(), this.getOffSetDepth());

        if (rotated) {
            double step = 0.3;
            for (double width = widthBound[0]; width <= widthBound[1]; width += step) {
                for (double height = heightBound[0]; height <= heightBound[1]; height += step) {
                    for (double depth = depthBound[0]; depth <= depthBound[1]; depth += step) {
                        Vector vector = new Vector(width, height, depth);
                        vectors.add(vector);
                    }
                }
            }
        } else {
            for (int width = widthBound[0]; width <= widthBound[1]; width++) {
                for (int height = heightBound[0]; height <= heightBound[1]; height++) {
                    for (int depth = depthBound[0]; depth <= depthBound[1]; depth++) {
                        vectors.add(new Vector(width, height, depth));
                    }
                }
            }
        }
        return vectors;
    }

    public int[] getBoundPair(int length, int offSetRight) {
        double bound = (double) (length - 1) / 2;
        int lowerBound = (int) Math.ceil(bound);
        int higherBound = (int) Math.floor(bound);
        return new int[]{-lowerBound + offSetRight, higherBound + offSetRight};
    }

    @Override
    public FigureType getFigureType() {
        return FigureType.LINEAR;
    }

}
