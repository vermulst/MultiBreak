package me.vermulst.multibreak.utils;

import me.vermulst.multibreak.multibreak.event.RefreshEvents;

import java.util.Objects;

public record SimpleLocation(double x, double y, double z, float yaw, float pitch) {
    public boolean isDifferent(org.bukkit.Location loc) {
        return this.x != loc.getX() ||
                this.y != loc.getY() ||
                this.z != loc.getZ() ||
                this.yaw != loc.getYaw() ||
                this.pitch != loc.getPitch();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SimpleLocation that = (SimpleLocation) o;
        return Double.compare(x, that.x) == 0 && Double.compare(y, that.y) == 0 && Double.compare(z, that.z) == 0 && Float.compare(yaw, that.yaw) == 0 && Float.compare(pitch, that.pitch) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, yaw, pitch);
    }
}
