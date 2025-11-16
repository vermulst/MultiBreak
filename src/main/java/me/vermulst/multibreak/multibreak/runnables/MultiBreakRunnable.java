package me.vermulst.multibreak.multibreak.runnables;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.BreakManager;
import me.vermulst.multibreak.multibreak.MultiBreak;
import me.vermulst.multibreak.utils.BreakUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.UUID;

public class MultiBreakRunnable extends BukkitRunnable {

    private final Player p;
    private final Block block;
    private boolean init = false;
    private final Figure figure;
    private final BreakManager breakManager;


    public MultiBreakRunnable(Player p, Block block, Figure figure, BreakManager breakManager) {
        this.p = p;
        this.block = block;
        this.figure = figure;
        this.breakManager = breakManager;
    }

    // Starts at tick 1, not at tick 0.
    @Override
    public void run() {
        UUID uuid = p.getUniqueId();
        boolean hasMoved = !init || breakManager.getMovedPlayers().contains(uuid);
        RayTraceResult rayTraceResult = hasMoved ? BreakUtils.getRayTraceResult(p) : null;

        // check once, if block changed since from tick 0 -> 1.
        MultiBreak multiBreak = breakManager.getMultiBreak(p);

        if (hasMoved) {
            breakManager.getMovedPlayers().remove(uuid);
            if (rayTraceResult == null) {
                cancelMultiBreak(multiBreak);
                return;
            }

            Block blockMining = rayTraceResult.getHitBlock();
            BlockFace blockFace = rayTraceResult.getHitBlockFace();

            if (!init) {
                if (block.getType().isAir() || !blockMining.equals(block)) {
                    cancelMultiBreak(multiBreak);
                    return;
                }
                init = true;
            }

            if (multiBreak == null) {
                multiBreak = breakManager.initMultiBreak(p, blockMining, this.figure);
                if (multiBreak == null) {
                    cancelMultiBreak(null);
                    return;
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
                breakManager.scheduleMultiBreak(p, this.figure, this.block);
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
        multiBreak = this.breakManager.initMultiBreak(p, this.block, this.figure);
        if (multiBreak == null) return null;
        multiBreak.setProgressBroken(progressBroken);
        multiBreak.setProgressTicks(progressTicks);
        return multiBreak;
    }
}
