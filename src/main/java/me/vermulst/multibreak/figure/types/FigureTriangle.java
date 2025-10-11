package me.vermulst.multibreak.figure.types;

import me.vermulst.multibreak.figure.FigureIterable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FigureTriangle extends FigureIterable {


    public FigureTriangle(int width, int height, int depth) {
        super(width, height, depth);
    }

    @Override
    public Set<Vector> getVectors(boolean rotated) {
        int[] widthBound = getBoundPair(this.getWidth(), this.getOffSetWidth());
        int[] heightBound = getBoundPair(this.getHeight(), this.getOffSetHeight());
        int[] depthBound = getBoundPair(this.getDepth(), this.getOffSetDepth());

        List<Vector> triangles = new ArrayList<>();
        Vector vertexTop = new Vector(this.getOffSetWidth(), heightBound[1], this.getOffSetDepth());
        Vector vertexLeft = new Vector(widthBound[0], heightBound[0], this.getOffSetDepth());
        Vector vertexRight = new Vector(widthBound[1], heightBound[0], this.getOffSetDepth());
        for (int i = depthBound[0]; i <= depthBound[1]; i++) {
            triangles.add(vertexTop.clone().add(new Vector(0, 0, i)));
            triangles.add(vertexLeft.clone().add(new Vector(0, 0, i)));
            triangles.add(vertexRight.clone().add(new Vector(0, 0, i)));
        }
        Set<Vector> vectors = this.iterateOverBoundingBox(triangles, rotated);
        vectors.removeIf(vector -> !this.isPointInAnyTriangle(vector, triangles));
        return vectors;
    }

    private boolean isPointInAnyTriangle(Vector point, List<Vector> triangles) {
        for (int i = 0; i < triangles.size(); i += 3) {
            if (point.getZ() != triangles.get(i).getZ()) continue;
            if (isPointInTriangle(point, triangles.get(i), triangles.get(i + 1), triangles.get(i + 2))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPointInTriangle(Vector point, Vector v1, Vector v2, Vector v3) {
        double x = point.getX();
        double y = point.getY();
        double x1 = v1.getX();
        double y1 = v1.getY();
        double x2 = v2.getX();
        double y2 = v2.getY();
        double x3 = v3.getX();
        double y3 = v3.getY();

        double denominator = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3);
        double alpha = ((y2 - y3) * (x - x3) + (x3 - x2) * (y - y3)) / denominator;
        double beta = ((y3 - y1) * (x - x3) + (x1 - x3) * (y - y3)) / denominator;
        double gamma = 1 - alpha - beta;
        // Check if all barycentric coordinates are in the range [0, 1]
        return alpha >= 0 && beta >= 0 && gamma >= 0 && alpha <= 1 && beta <= 1 && gamma <= 1;
    }

    public int[] getBoundPair(int length, int offSetRight) {
        double bound = (double) (length - 1) / 2;
        int lowerBound = (int) Math.ceil(bound);
        int higherBound = (int) Math.floor(bound);
        return new int[]{-lowerBound + offSetRight, higherBound + offSetRight};
    }

    @Override
    public FigureType getFigureType() {
        return FigureType.TRIANGULAR;
    }
}
