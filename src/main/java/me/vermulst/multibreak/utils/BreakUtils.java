package me.vermulst.multibreak.utils;

import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;


public class BreakUtils {

    private static int getRange(Player p) {
        double range = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue();;
        return (int) Math.ceil(range);
    }

    public static BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(getRange(p));
    }

    public static Block getTargetBlock(Player p) {
        return p.getTargetBlockExact(getRange(p));
    }

    public static RayTraceResult getRayTraceResult(Player p) {
        return p.rayTraceBlocks(getRange(p));
    }
}
