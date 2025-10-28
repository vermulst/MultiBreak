package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.logging.Level;

public class MultiBreakRunnable extends BukkitRunnable {

    private Block block;
    private final Player p;
    private final BreakManager breakManager;

    public MultiBreakRunnable(Block block, Player p, BreakManager breakManager) {
        this.block = block;
        this.p = p;
        this.breakManager = breakManager;
    }

    // Starts at tick 1, not at tick 0.
    @Override
    public void run() {
        // check once, if block changed since from tick 0 -> 1.
        if (block != null) {
            if (block.getType().isAir() && !breakManager.getTargetBlock(p).equals(block)) {
                cancel();
                return;
            }
            block = null;
        }
        MultiBreak multiBreak = breakManager.getOrCreateMultiBreak(p);
        if (multiBreak == null) {
            cancel();
            return;
        }
        BlockFace blockFace = getBlockFace(p);
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
            breakManager.scheduleMultiBreak(p);
        }
        multiBreak.tick();
    }

    public BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(Main.getInstance().getConfigManager().getMaxRange());
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
        multiBreak = this.breakManager.getOrCreateMultiBreak(p);
        if (multiBreak == null) return null;
        multiBreak.setProgressBroken(progressBroken);
        multiBreak.setProgressTicks(progressTicks);
        return multiBreak;
    }

}
