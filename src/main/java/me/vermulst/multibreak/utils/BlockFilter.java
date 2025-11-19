package me.vermulst.multibreak.utils;

import org.bukkit.Material;

import java.util.EnumSet;

public class BlockFilter {
    public static boolean isExcluded(Material material, EnumSet<Material> included, EnumSet<Material> ignored) {
        if (included != null && !included.isEmpty() && !included.contains(material)) {
            return true;
        }
        if (ignored != null && !ignored.isEmpty() && ignored.contains(material)) {
            return true;
        }
        return material == Material.AIR;
    }
}
