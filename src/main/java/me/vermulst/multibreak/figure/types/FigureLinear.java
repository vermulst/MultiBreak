package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.figure.Figure;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class FigureLinear extends Figure {


    public FigureLinear(int width, int height, int depth) {
        super(width, height, depth);
    }

    @Override
    public HashSet<Vector> getVectors(Vector playerDirVector, CompassDirection playerDirection) {
        HashSet<Vector> vectors = new HashSet<>();

        int[] widthBound = getBoundPair(this.getWidth(), this.getOffSetWidth());
        int[] heightBound = getBoundPair(this.getHeight(), this.getOffSetHeight());
        int[] depthBound = getBoundPair(this.getDepth(), this.getOffSetDepth());

        for (int width = widthBound[0]; width <= widthBound[1]; width++) {
            for (int height = heightBound[0]; height <= heightBound[1]; height++) {
                for (int depth = depthBound[0]; depth <= depthBound[1]; depth++) {
                    Vector newVector = new Vector(width, height, depth);
                    if (newVector.equals(new Vector(0, 0, 0))) continue;

                    // Rotate the vector based on player's direction
                    newVector = rotateVector(newVector, playerDirVector, playerDirection);
                    vectors.add(newVector);
                }
            }
        }
        return vectors;
    }

    @Override
    public FigureType getFigureType() {
        return FigureType.LINEAR;
    }

}
