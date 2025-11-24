package me.vermulst.multibreak.multibreak.runnables;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.BreakManager;
import me.vermulst.multibreak.multibreak.MultiBreak;
import me.vermulst.multibreak.multibreak.MultiBreakType;
import me.vermulst.multibreak.utils.BreakUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MultiBreakRunnable extends BukkitRunnable {

    private final Player p;
    private final Block block;
    private boolean init = false;
    private final @NotNull Figure figure;
    private final BreakManager breakManager;
    private final MultiBreakType multiBreakType;


    public MultiBreakRunnable(Player p, Block block, @NotNull Figure figure, BreakManager breakManager, MultiBreakType multiBreakType) {
        this.p = p;
        this.block = block;
        this.figure = figure;
        this.breakManager = breakManager;
        this.multiBreakType = multiBreakType;
    }

    // Starts at tick 1, not at tick 0.
    @Override
    public void run() {
        UUID uuid = p.getUniqueId();
        MultiBreak multiBreak = breakManager.getMultiBreak(p);
        boolean isInitializedStaticBreak = multiBreak != null && multiBreakType.isStatic();
        boolean hasMoved = !init;
        if (!hasMoved && breakManager.getMovedPlayers().containsKey(uuid) && !isInitializedStaticBreak) {
            int movedTick = breakManager.getMovedPlayers().get(uuid);
            int difference = Bukkit.getCurrentTick() - movedTick;
            int potentialRaytraceDelayTicks = 2 + (p.getPing() + 49) / 50;
            if (difference <= potentialRaytraceDelayTicks) {
                hasMoved = true;
            } else {
                breakManager.getMovedPlayers().remove(uuid);
            }
        }
        RayTraceResult rayTraceResult = hasMoved ? BreakUtils.getRayTraceResult(p) : null;

        if (hasMoved) {
            if (rayTraceResult == null) {
                cancelMultiBreak(multiBreak);
                return;
            }

            Block blockMining = rayTraceResult.getHitBlock();
            BlockFace blockFace = rayTraceResult.getHitBlockFace();

            // check once, if block changed since from tick 0 -> 1.
            if (!init) {
                if (block.getType().isAir() || !blockMining.equals(block)) {
                    cancelMultiBreak(multiBreak);
                    return;
                }
                init = true;
            }

            if (multiBreak == null) {
                multiBreak = breakManager.initMultiBreak(p, blockMining, this.figure, this.multiBreakType);
                if (multiBreak == null) {
                    cancelMultiBreak(null);
                    return;
                }
                if (this.multiBreakType.isStatic()) {
                    multiBreak.setLastTick(Bukkit.getCurrentTick());
                }
            }

            // check if direction changed
            Vector direction = blockFace.getDirection();
            if (!multiBreak.getPlayerDirection().equalsVector(direction)) {
                multiBreak = replaceMultiBreak(p, multiBreak);
                if (multiBreak == null) {
                    cancelMultiBreak(null);
                    return;
                }
                breakManager.scheduleMultiBreak(p, this.figure, this.block, this.multiBreakType);
            }
        }

        multiBreak.tick();
    }


    public void cancelMultiBreak(MultiBreak multiBreak) {
        breakManager.endMultiBreak(p, multiBreak, false);
        if (!this.isCancelled()) cancel();
    }

    /** Replaces multibreak while preserving progress
     *
     * @param p - player breaking
     * @param multiBreak - multibreak to replaced
     * @return replaced multibreak
     */
    public MultiBreak replaceMultiBreak(Player p, MultiBreak multiBreak) {
        this.breakManager.endMultiBreak(p, multiBreak, false);
        float progressBroken = multiBreak.getProgressBroken();
        int progressTicks = multiBreak.getProgressTicks();
        multiBreak = this.breakManager.initMultiBreak(p, this.block, this.figure, this.multiBreakType);
        if (multiBreak == null) return null;
        multiBreak.setProgressBroken(progressBroken);
        multiBreak.setProgressTicks(progressTicks);
        return multiBreak;
    }
}
