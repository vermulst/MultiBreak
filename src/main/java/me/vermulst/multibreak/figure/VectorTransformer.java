package me.vermulst.multibreak.figure;

import me.vermulst.multibreak.CompassDirection;
import org.bukkit.util.Vector;

public class VectorTransformer {

    public VectorTransformer(Vector playerDirectionVector, CompassDirection playerDirection) {
        this.playerDirectionVector = playerDirectionVector;
        this.playerDirection = playerDirection;
        double angleY = -Math.atan2(playerDirectionVector.getX(), playerDirectionVector.getZ());
        double angleX = -Math.asin(playerDirectionVector.getY());

        Matrix4x4 rotationMatrixY = new Matrix4x4();
        rotationMatrixY.setRotationY(angleY);
        this.rotationMatrixY = rotationMatrixY;

        Matrix4x4 rotationMatrixX = new Matrix4x4();
        rotationMatrixX.setRotationX(angleX);
        this.rotationMatrixX = rotationMatrixX;
    }

    private final Vector playerDirectionVector;
    private final CompassDirection playerDirection;
    private final Matrix4x4 rotationMatrixY;
    private final Matrix4x4 rotationMatrixX;

    public Vector rotateVector(Vector vector) {
        vector = this.getRotationMatrixY().transform(vector);
        vector = this.getRotationMatrixX().transform(vector);
        if (Math.abs(this.getPlayerDirectionVector().getY()) == 1) {
            vector.rotateAroundY(-this.getPlayerDirection().getAngle());
        } else {
            vector.rotateAroundY(this.getPlayerDirection().getAngle() * 2);
        }
        return vector;
    }

    public Vector getPlayerDirectionVector() {
        return playerDirectionVector;
    }

    public CompassDirection getPlayerDirection() {
        return playerDirection;
    }

    public Matrix4x4 getRotationMatrixY() {
        return rotationMatrixY;
    }

    public Matrix4x4 getRotationMatrixX() {
        return rotationMatrixX;
    }
}
