package me.vermulst.multibreak.utils;

import me.vermulst.multibreak.multibreak.MultiBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class BreakUtils {

    private static final int INTERACTION_RANGE_LEEWAY = 2;

    private static double getRange(Player p) {
        double range = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue();;
        return range + INTERACTION_RANGE_LEEWAY;
    }

    public static BlockFace getBlockFace(Player p) {
        return p.rayTraceBlocks(getRange(p), FluidCollisionMode.NEVER).getHitBlockFace();
    }

    public static Block getTargetBlock(Player p) {
        return p.rayTraceBlocks(getRange(p), FluidCollisionMode.NEVER).getHitBlock();
    }

    public static RayTraceResult getRayTraceResultExact(Player p) {
        return p.rayTraceBlocks(getRange(p) - INTERACTION_RANGE_LEEWAY, FluidCollisionMode.NEVER);
    }

    public static RayTraceResult getRayTraceResult(Player p) {
        return p.rayTraceBlocks(getRange(p), FluidCollisionMode.NEVER);
    }

    public static float getDestroySpeed(Player p, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeedMain(p);
    }

    public static float getDestroySpeed(ServerPlayer serverPlayer, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeedMain(serverPlayer);
    }

    public static float getDestroySpeed(ServerPlayer serverPlayer, BlockPos blockPos, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeed(serverPlayer, blockPos);
    }
}
