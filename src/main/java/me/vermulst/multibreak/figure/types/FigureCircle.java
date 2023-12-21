package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.FigureIterable;
import me.vermulst.multibreak.figure.VectorTransformer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;

public class FigureCircle extends FigureIterable {


    public FigureCircle(int width, int height, int depth) {
        super(width, height, depth);
    }

    @Override
    public HashSet<Vector> getVectors(boolean rotated) {

        Vector startPos = new Vector(this.getOffSetWidth(), this.getOffSetHeight(), this.getOffSetDepth());
        double a = getWidth() / 2.0;
        double b = getHeight() / 2.0;
        double c = getDepth() / 2.0;

        int resolution = 30;  // You can adjust this based on the desired number of points
        double du = 2.0 * Math.PI / (resolution - 1);
        double dv = Math.PI / (resolution - 1);

        HashSet<Vector> boundingVectors = new HashSet<>();
        for (int i = 0; i < resolution; i++) {
            double u = i * du;
            for (int j = 0; j < resolution; j++) {
                double v = j * dv;

                Vector vector = this.generateEllipsoidPoint(u, v, a, b, c);
                vector.add(startPos);
                boundingVectors.add(vector);
            }
        }
        HashSet<Vector> vectors = this.iterateOverBoundingBox(boundingVectors, rotated);
        vectors.removeIf(vector -> !this.isInsideEllipsoid(vector, a, b, c));
        return vectors;
    }



    private boolean isInsideEllipsoid(Vector vector, double a, double b, double c) {
        double x = vector.getX() / a;
        double y = vector.getY() / b;
        double z = vector.getZ() / c;
        return (x * x + y * y + z * z) <= 1.0;
    }


    private Vector generateEllipsoidPoint(double u, double v, double a, double b, double c) {
        double cu = Math.cos(u);
        double su = Math.sin(u);
        double cv = Math.cos(v);
        double sv = Math.sin(v);

        int x = (int) Math.round(a * cu * sv);
        int y = (int) Math.round(b * su * sv);
        int z = (int) Math.round(c * cv);
        return new Vector(x, y, z);
    }

    @Override
    public FigureType getFigureType() {
        return FigureType.CIRCULAR;
    }

}
