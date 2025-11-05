package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.figure.Figure;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
        // check once, if block changed since from tick 0 -> 1.
        if (!init) {
            if (block.getType().isAir() && !BreakUtils.getTargetBlock(p).equals(block)) {
                cancel();
                return;
            }
            init = true;
        }
        MultiBreak multiBreak = breakManager.getMultiBreak(p);
        if (multiBreak == null) {
            Block blockMining = BreakUtils.getTargetBlock(p);
            multiBreak = breakManager.initMultiBreak(p, blockMining, this.figure);
            if (multiBreak == null) {
                cancel();
                return;
            }
        }
        BlockFace blockFace = BreakUtils.getBlockFace(p);
        if (blockFace == null) {
            cancel();
            return;
        }
        // check if direction changed
        Vector direction = blockFace.getDirection();
        if (!multiBreak.getPlayerDirection().equals(direction)) {
            multiBreak = replaceMultiBreak(p, multiBreak);
            if (multiBreak == null) {
                cancel();
                return;
            }
            breakManager.scheduleMultiBreak(p, this.figure);
        }
        multiBreak.tick();
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
