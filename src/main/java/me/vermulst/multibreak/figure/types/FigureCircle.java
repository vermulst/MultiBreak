package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.figure.Figure;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;

public class FigureCircle extends Figure {


    public FigureCircle(int width, int height, int depth) {
        super(width, height, depth);
    }

    @Override
    public HashSet<Vector> getVectors(Vector playerDirVector, CompassDirection playerDirection) {
        HashSet<Vector> vectors = new HashSet<>();

        Vector startPos = new Vector(this.getOffSetWidth(), this.getOffSetHeight(), this.getOffSetDepth());
        double a = getWidth() / 2.0;
        double b = getHeight() / 2.0;
        double c = getDepth() / 2.0;

        // Number of points for each dimension
        int resolution = 40;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {
                    double u = i * 2.0 * Math.PI / (resolution - 1);
                    double v = j * Math.PI / (resolution - 1);
                    double w = k * 2.0 * Math.PI / (resolution - 1);

                    // Parametric equations for the ellipsoid in x, y, and z
                    double x = a * Math.cos(u) * Math.sin(v) * Math.cos(w);
                    double y = b * Math.sin(u) * Math.sin(v) * Math.cos(w);
                    double z = c * Math.cos(v) * Math.sin(w);

                    double errorX = 0.4 - (Math.abs(x) * 0.04) >= 0 ? 0.4 - (Math.abs(x) * 0.04) : 0;
                    double errorY = 0.4 - (Math.abs(y) * 0.04) >= 0 ? 0.4 - (Math.abs(y) * 0.04) : 0;
                    double errorZ = 0.4 - (Math.abs(z) * 0.04) >= 0 ? 0.4 - (Math.abs(z) * 0.04) : 0;
                    int signX = (x >= 0) ? 1 : -1;
                    int signY = (y >= 0) ? 1 : -1;
                    int signZ = (z >= 0) ? 1 : -1;

                    int xRounded = (int) Math.round(x - (errorX * signX));
                    int yRounded = (int) Math.round(y - (errorY * signY));
                    int zRounded = (int) Math.round(z - (errorZ * signZ));
                    Vector vector = new Vector(xRounded, yRounded, zRounded);
                    vector.add(startPos);

                    vectors.add(rotateVector(vector, playerDirVector, playerDirection));
                }
            }
        }

        return vectors;
    }

    @Override
    public FigureType getFigureType() {
        return FigureType.CIRCULAR;
    }

}
