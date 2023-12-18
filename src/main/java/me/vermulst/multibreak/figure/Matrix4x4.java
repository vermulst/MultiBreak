package me.vermulst.multibreak.figure;


import org.bukkit.util.Vector;

public class Matrix4x4 {
    private final double[][] matrix;

    public Matrix4x4() {
        this.matrix = new double[4][4];
        setIdentity();
    }

    public void setIdentity() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matrix[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }
    }

    public void setRotationX(double angle) {
        setIdentity();
        matrix[1][1] = Math.cos(angle);
        matrix[1][2] = -Math.sin(angle);
        matrix[2][1] = Math.sin(angle);
        matrix[2][2] = Math.cos(angle);
    }

    public void setRotationY(double angle) {
        setIdentity();
        matrix[0][0] = Math.cos(angle);
        matrix[0][2] = Math.sin(angle);
        matrix[2][0] = -Math.sin(angle);
        matrix[2][2] = Math.cos(angle);
    }

    public Vector transform(Vector vector) {
        double x = matrix[0][0] * vector.getX() + matrix[0][1] * vector.getY() + matrix[0][2] * vector.getZ();
        double y = matrix[1][0] * vector.getX() + matrix[1][1] * vector.getY() + matrix[1][2] * vector.getZ();
        double z = matrix[2][0] * vector.getX() + matrix[2][1] * vector.getY() + matrix[2][2] * vector.getZ();

        return new Vector(x, y, z);
    }
}