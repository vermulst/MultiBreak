package me.vermulst.multibreak.figure;

import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class FigureIterable extends Figure {

    public FigureIterable(int width, int height, int depth) {
        super(width, height, depth);
    }


    public Set<Vector> iterateOverBoundingBox(Collection<Vector> boundingVectors, boolean rotated) {
        double[] boundingBox = calculateBoundingBox(boundingVectors);

        double step = rotated ? 0.5 : 1;
        double minX = boundingBox[0];
        double minY = boundingBox[1];
        double minZ = boundingBox[2];
        double maxX = boundingBox[3];
        double maxY = boundingBox[4];
        double maxZ = boundingBox[5];

        // Iterate over the bounding box
        Set<Vector> vectors = new HashSet<>();
        for (double x = minX; x <= maxX; x += step) {
            for (double y = minY; y <= maxY; y += step) {
                for (double z = minZ; z <= maxZ; z += step) {
                    Vector vector = new Vector(x, y, z);
                    vectors.add(vector);
                }
            }
        }
        return vectors;
    }


    private double[] calculateBoundingBox(Collection<Vector> vectors) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double maxZ = Double.MIN_VALUE;

        for (Vector vector : vectors) {
            double x = vector.getX();
            double y = vector.getY();
            double z = vector.getZ();

            // Update minimum values
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);

            // Update maximum values
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        return new double[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

}
