package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.VectorTransformer;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class FigureLinear extends Figure {


    public FigureLinear(int width, int height, int depth) {
        super(width, height, depth);
    }

    @Override
    public HashSet<Vector> getVectors(boolean rotated) {
        HashSet<Vector> vectors = new HashSet<>();

        double step = rotated ? 0.3 : 1;
        int[] widthBound = getBoundPair(this.getWidth(), this.getOffSetWidth());
        int[] heightBound = getBoundPair(this.getHeight(), this.getOffSetHeight());
        int[] depthBound = getBoundPair(this.getDepth(), this.getOffSetDepth());

        for (double width = widthBound[0]; width <= widthBound[1]; width += step) {
            for (double height = heightBound[0]; height <= heightBound[1]; height += step) {
                for (double depth = depthBound[0]; depth <= depthBound[1]; depth += step) {
                    Vector vector = new Vector(width, height, depth);
                    vectors.add(vector);
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
