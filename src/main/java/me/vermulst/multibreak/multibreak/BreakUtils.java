package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.config.Config;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class BreakUtils {

    public static BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(Config.getInstance().getMaxRange());
    }

    public static Block getTargetBlock(Player p) {
        return p.getTargetBlockExact(Config.getInstance().getMaxRange());
    }
}
