package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;

public class MultiBreak {

    private final Player p;
    private final Block block;
    private int progressTicks;
    private ArrayList<MultiBlock> multiBlocks = new ArrayList<>();
    private boolean ended = false;


    public MultiBreak(Player p, Block block, Figure figure, Vector playerDirection) {
        this.p = p;
        this.block = block;
        this.initBlocks(figure, playerDirection);
        this.checkValid();
    }

    public void checkValid() {
        float breakSpeed = this.getBlock().getBreakSpeed(this.getPlayer());
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getType().equals(Material.AIR));
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getBreakSpeed(this.getPlayer()) < breakSpeed);
    }


    public void initBlocks(Figure figure, Vector playerDirection) {
        this.multiBlocks = new ArrayList<>();
        CompassDirection compassDirection = CompassDirection.getCompassDir(this.getPlayer().getLocation());
        HashSet<Vector> blockVectors = figure.getVectors(playerDirection, compassDirection);
        for (Vector vector : blockVectors) {
            Block block1 = this.getBlock().getLocation().add(vector).getBlock();
            MultiBlock multiBlock = new MultiBlock(block1);
            this.getMultiBlocks().add(multiBlock);
        }
    }

    public void tick(Main plugin, Block blockMining) {
        if (!blockMining.equals(this.getBlock())) {
            this.end(false);
        }
        this.progressTicks++;
        this.updateAnimations();
        this.scheduleCancel(plugin, blockMining);
    }

    public void end(boolean finished) {
        this.ended = true;
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.ITEM_CRACK)
                .count(8)
                .offset(1, 0.1, 0.1)
                .extra(0.1);
        World world = this.getBlock().getWorld();
        ItemStack tool = this.getPlayer().getInventory().getItemInMainHand();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            multiBlock.writeStage(-1);
            if (!finished) continue;
            Block block = multiBlock.getBlock();
            for (ItemStack drop : block.getDrops(tool)) {
                world.dropItemNaturally(block.getLocation(), drop);
            }
            if (multiBlock.hasAdjacentAir()) {
                particleBuilder
                        .data(new ItemStack(block.getType()))
                        .location(block.getLocation().add(0.5, 0, 0.5))
                        .spawn();
            }
            world.playSound(block.getLocation(), block.getBlockData().getSoundGroup().getBreakSound(), 1F, 1F);
            block.setType(Material.AIR);
        }
        this.progressTicks = 0;
    }

    public void scheduleCancel(Main plugin, Block targetBlock) {
        int currentProgress = this.progressTicks;
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean progress = currentProgress != getProgressTicks();
                boolean target = targetBlock == null
                        || targetBlock.equals(p.getLastTwoTargetBlocks(null, 10).get(1));
                if (target && progress) return;
                for (MultiBlock multiBlock : getMultiBlocks()) {
                    if (!multiBlock.hasAdjacentAir()) continue;
                    multiBlock.writeStage(-1);
                }
                end(false);
            }
        }.runTaskLater(plugin, 2);
    }

    public void updateAnimations() {
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.BLOCK_CRACK)
                .offset(0.375, 0, 0.375);
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (!multiBlock.hasAdjacentAir()) continue;
            particleBuilder.location(multiBlock.getBlock().getLocation()
                    .add(0.5, 1, 0.5))
                    .data(multiBlock.getBlock().getType().createBlockData())
                    .spawn();
        }
        updateBlockAnimationPacket();
    }

    public void updateBlockAnimationPacket() {
        int destroySpeedInTicks = (int) (0.0001 + (1 / this.getBlock().getBreakSpeed(this.getPlayer())));
        double progress = ((double) this.getProgressTicks() / (double) destroySpeedInTicks);
        int stage = (int) (progress * 9);
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (!multiBlock.hasAdjacentAir()) continue;
            multiBlock.writeStage(stage);
        }
    }

    public Player getPlayer() {
        return p;
    }

    public Block getBlock() {
        return block;
    }

    public int getProgressTicks() {
        return progressTicks;
    }

    public ArrayList<MultiBlock> getMultiBlocks() {
        return multiBlocks;
    }

    public boolean hasEnded() {
        return ended;
    }
}
