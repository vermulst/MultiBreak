package me.vermulst.multibreak.utils;

import me.vermulst.multibreak.multibreak.MultiBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class BreakUtils {

    private static final int INTERACTION_RANGE_LEEWAY = 2;

    private static double getRange(Player p) {
        double range = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue();;
        return range + INTERACTION_RANGE_LEEWAY;
    }

    public static RayTraceResult getRayTraceResultExact(Player p) {
        return p.rayTraceBlocks(getRange(p) - INTERACTION_RANGE_LEEWAY, FluidCollisionMode.NEVER);
    }

    public static RayTraceResult getRayTraceResult(Player p) {
        return p.rayTraceBlocks(getRange(p), FluidCollisionMode.NEVER);
    }

    public static BlockFace getBlockFace(Player p) {
        RayTraceResult rayTraceResult = getRayTraceResult(p);
        if (rayTraceResult == null) return null;
        return rayTraceResult.getHitBlockFace();
    }

    public static BlockFace getThickRaytraceBlockFace(Player p, Block targetBlock) {
        // Try 3 times with increasing thickness
        double[] thicknesses = {0.1, 0.2, 0.35}; // Progressive expansion
        double range = getRange(p);
        for (int attempt = 0; attempt < 3; attempt++) {
            BlockFace result = performThickRaycast(p, targetBlock, thicknesses[attempt], range);
            if (result != null) {
                return result;
            }
        }

        return null; // Give up after 3 attempts
    }

    private static BlockFace performThickRaycast(Player p, Block targetBlock, double thickness, double range) {
        Location eyeLoc = p.getEyeLocation();
        Vector direction = eyeLoc.getDirection();

        // Create perpendicular vectors for grid
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = direction.clone().crossProduct(right).normalize();

        // 3x3 grid of offsets
        double[] offsets = {-thickness, 0, thickness};

        for (double xOff : offsets) {
            for (double yOff : offsets) {
                Location origin = eyeLoc.clone()
                        .add(right.clone().multiply(xOff))
                        .add(up.clone().multiply(yOff));

                RayTraceResult rayResult = p.getWorld().rayTraceBlocks(
                        origin,
                        direction,
                        getRange(p),
                        FluidCollisionMode.NEVER,
                        false
                );

                if (rayResult != null && rayResult.getHitBlock() != null
                        && rayResult.getHitBlock().equals(targetBlock)) {
                    return rayResult.getHitBlockFace();
                }
            }
        }

        return null;
    }

    public static Block getTargetBlock(Player p) {
        RayTraceResult rayTraceResult = getRayTraceResult(p);
        if (rayTraceResult == null) return null;
        return rayTraceResult.getHitBlock();
    }

    public static float getDestroySpeed(Player p, MultiBreak multiBreak) {
        if (multiBreak == null) return -1f;
        multiBreak.invalidateHasCorrectToolCache();
        multiBreak.checkDestroySpeedChange(p);
        return multiBreak.getDestroySpeedMain(p);
    }

    public static float getDestroySpeed(ServerPlayer serverPlayer, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeedMain(serverPlayer);
    }

    public static float getDestroySpeed(ServerPlayer serverPlayer, BlockPos blockPos, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeed(serverPlayer, blockPos);
    }
}
