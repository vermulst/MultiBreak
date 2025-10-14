package me.vermulst.multibreak.utils;

import org.bukkit.Location;

public enum CompassDirection {

    WEST,
    EAST,
    NORTH,
    SOUTH;

    public double getAngle() {
        switch (this) {
            case EAST -> {
                return Math.PI / 2;
            }
            case SOUTH -> {
                return Math.PI;
            }
            case WEST -> {
                return -Math.PI / 2;
            }
        }
        return 0;
    }

    public static CompassDirection getCompassDir(Location playerLocation) {
        double yaw = playerLocation.getYaw();
        if (yaw >= -135 && yaw <= -45) return CompassDirection.EAST;
        if (yaw >= 45 && yaw <= 135) return CompassDirection.WEST;
        if ((yaw >= 135 && yaw <= 180) || (yaw >= -180 && yaw <= -135)) return CompassDirection.NORTH;
        return CompassDirection.SOUTH;
    }

}
