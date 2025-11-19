package me.vermulst.multibreak.multibreak.runnables;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.multibreak.MultiBlock;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class WriteParticleRunnable extends BukkitRunnable {

    private final MultiBlock[] multiBlocks;
    private final Block block;
    private final ParticleBuilder particleBuilder;
    private final double sideOffsetX;
    private final double sideOffsetY;
    private final double sideOffsetZ;
    private final Location loc;
    public WriteParticleRunnable(MultiBlock[] multiBlocks, Block block, ParticleBuilder particleBuilder, double sideOffsetX, double sideOffsetY, double sideOffsetZ, Location loc) {
        this.multiBlocks = multiBlocks;
        this.block = block;
        this.particleBuilder = particleBuilder;
        this.sideOffsetX = sideOffsetX;
        this.sideOffsetY = sideOffsetY;
        this.sideOffsetZ = sideOffsetZ;
        this.loc = loc;
    }

    @Override
    public void run() {
        for (MultiBlock multiBlock : multiBlocks) {
            if (multiBlock.getBlock().equals(block)) continue;
            if (!multiBlock.isVisible()) continue;

            Block block = multiBlock.getBlock();
            loc.set(block.getX() + 0.5, block.getY() + 0.5, block.getZ() + 0.5);
            loc.add(sideOffsetX, sideOffsetY, sideOffsetZ);

            particleBuilder.location(loc)
                    .data(block.getBlockData())
                    .spawn();
        }
    }
}
