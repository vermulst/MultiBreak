package me.vermulst.multibreak.figure;

import me.vermulst.multibreak.CompassDirection;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class Figure {


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

    public ArrayList<Vector> getVectors(Vector playerDirVector, CompassDirection playerDirection) {
        ArrayList<Vector> vectors = new ArrayList<>();

        int[] widthBound = getBoundPair(width, offSetWidth);
        int[] heightBound = getBoundPair(height, offSetHeight);
        int[] depthBound = getBoundPair(depth, offSetDepth);

        for (int width = widthBound[0]; width <= widthBound[1]; width++) {
            for (int height = heightBound[0]; height <= heightBound[1]; height++) {
                for (int depth = depthBound[0]; depth <= depthBound[1]; depth++) {
                    Vector newVector = new Vector(width, height, depth);
                    if (newVector.equals(new Vector(0, 0, 0))) continue;

                    // Rotate the vector based on player's direction
                    newVector = rotateVector(newVector, playerDirVector);
                    if (Math.abs(playerDirVector.getY()) == 1) {
                        newVector.rotateAroundY(-playerDirection.getAngle());
                    }
                    if (Math.abs(playerDirVector.getX()) == 1) {
                        newVector.rotateAroundX(playerDirection.getAngle());
                    }
                    vectors.add(newVector);
                }
            }
        }
        return vectors;
    }


    private Vector rotateVector(Vector vector, Vector playerDirVector) {
        // Calculate the rotation angle around the Y-axis
        double angleY = -Math.atan2(playerDirVector.getX(), playerDirVector.getZ());

        // Create a rotation matrix for Y-axis rotation
        Matrix4x4 rotationMatrixY = new Matrix4x4();
        rotationMatrixY.setRotationY(angleY);

        // Apply the Y-axis rotation to the vector
        vector = rotationMatrixY.transform(vector);

        // Calculate the rotation angle around the X-axis
        double angleX = -Math.asin(playerDirVector.getY());

        // Create a rotation matrix for X-axis rotation
        Matrix4x4 rotationMatrixX = new Matrix4x4();
        rotationMatrixX.setRotationX(angleX);

        // Apply the X-axis rotation to the vector
        vector = rotationMatrixX.transform(vector);

        return vector;
    }

    public int[] getBoundPair(int length, int offSetRight) {
        double bound = (double) (length - 1) / 2;
        int lowerBound = (int) Math.ceil(bound);
        int higherBound = (int) Math.floor(bound);
        return new int[]{-lowerBound + offSetRight, higherBound + offSetRight};
    }

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
