package me.vermulst.multibreak.utils;

import me.vermulst.multibreak.config.Config;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class BreakUtils {
    public static BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(Config.getInstance().getMaxRange());
    }

    public static Block getTargetBlock(Player p) {
        return p.getTargetBlockExact(Config.getInstance().getMaxRange());
    }

    public static RayTraceResult getRayTraceResult(Player p) {
        return p.rayTraceBlocks(Config.getInstance().getMaxRange());
    }
}
