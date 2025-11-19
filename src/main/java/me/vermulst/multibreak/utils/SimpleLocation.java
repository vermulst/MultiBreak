package me.vermulst.multibreak.utils;

public record SimpleLocation(double x, double y, double z, float yaw, float pitch) {
    private static final double POS_EPSILON = 0.005;
    private static final float ROT_EPSILON = 0.1f;

    public boolean isDifferent(org.bukkit.Location loc) {
        if (Math.abs(this.yaw - loc.getYaw()) > ROT_EPSILON) return true;
        if (Math.abs(this.pitch - loc.getPitch()) > ROT_EPSILON) return true;
        if (Math.abs(this.x - loc.getX()) > POS_EPSILON) return true;
        if (Math.abs(this.y - loc.getY()) > POS_EPSILON) return true;
        return Math.abs(this.z - loc.getZ()) > POS_EPSILON;
    }
}
