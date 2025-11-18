package me.vermulst.multibreak.utils;

import org.bukkit.util.Vector;

public record IntVector(int x, int y, int z) {
    public static IntVector of(Vector v) {
        return new IntVector((int) v.getX(), (int) v.getY(), (int) v.getZ());
    }

    public boolean equalsVector(Vector v) {
        // Perform the comparison logic directly
        return this.x == (int) v.getX() &&
                this.y == (int) v.getY() &&
                this.z == (int) v.getZ();
    }
}
