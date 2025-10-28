package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.utils.CompassDirection;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.Matrix4x4;
import me.vermulst.multibreak.figure.VectorTransformer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class MultiBreak {

    private final Player p;
    private final Block block;
    private final Vector playerDirection;
    private int progressTicks;
    private float progressBroken;
    private List<MultiBlock> multiBlocks = new ArrayList<>();
    private boolean ended = false;


    public MultiBreak(Player p, Block block, Figure figure, Vector playerDirection) {
        this.p = p;
        this.block = block;
        this.playerDirection = playerDirection;
        this.initBlocks(figure, playerDirection);
        float breakSpeed = this.getBlock().getBreakSpeed(this.getPlayer());
        float destroySpeed = 0.000001f + (1 / breakSpeed);
        this.progressBroken = ((float) 1) / destroySpeed;
    }

    public void setFigure(Figure figure) {
        this.initBlocks(figure, this.playerDirection);
    }

    public void checkValid(float breakSpeed, EnumSet<Material> includedMaterials, EnumSet<Material> excludedMaterials) {
        if (includedMaterials != null && !includedMaterials.isEmpty()) {
            this.getMultiBlocks().removeIf(multiBlock -> !includedMaterials.contains(multiBlock.getBlock().getType()));
        }
        if (excludedMaterials != null && !excludedMaterials.isEmpty()) {
            this.getMultiBlocks().removeIf(multiBlock -> excludedMaterials.contains(multiBlock.getBlock().getType()));
        }
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getType().equals(Material.AIR));
        ConfigManager config = Main.getInstance().getConfigManager();
        boolean fairMode = config.getOptions()[1];
        if (!fairMode) return;
        float fairModeLeeway = Main.getInstance().getConfigManager().getFairModeTicksLeeway() * 0.05f;
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getBreakSpeed(this.getPlayer()) + fairModeLeeway < breakSpeed);
    }

    public void initBlocks(Figure figure, Vector playerDirection) {
        this.multiBlocks = new ArrayList<>();
        if (figure == null) return;
        CompassDirection compassDirection = CompassDirection.getCompassDir(this.getPlayer().getLocation());

        boolean rotated = figure.getRotationWidth() != 0.0 || figure.getRotationHeight() != 0.0 || figure.getRotationDepth() != 0.0;
        Set<Vector> blockVectors = figure.getVectors(rotated);

        // apply figure rotation
        if (rotated) {
            blockVectors = figure.applyRotation(blockVectors, figure);
        }

        // apply player rotation
        VectorTransformer vectorTransformer = new VectorTransformer(playerDirection, compassDirection);
        for (Vector vector : blockVectors) {
            vectorTransformer.rotateVector(vector);
        }

        // exclude center block - (Important after rotation)
        blockVectors.remove(new Vector(0, 0, 0));
        Location loc = this.getBlock().getLocation();

        for (Vector vector : blockVectors) {
            Block block = loc.clone().add(vector).getBlock();
            Material type = block.getType();
            if (block.isLiquid() || !type.isItem()) continue;
            MultiBlock multiBlock = new MultiBlock(block);
            this.getMultiBlocks().add(multiBlock);
        }
    }

    @Deprecated
    public void tick(Main plugin, Block blockMining) {
        if (!blockMining.equals(this.getBlock())) {
            this.end(false, plugin);
        }

        this.progressTicks++;
        if (this.progressTicks % 2 == 0) this.playParticles();
        updateBlockAnimationPacket();
        this.scheduleCancel(plugin, blockMining);
    }

    public void tick() {
        this.progressTicks++;
        if (this.progressTicks % 2 == 0) this.playParticles();
        updateBlockAnimationPacket();
    }

    public void end(boolean finished, Main plugin) {
        this.ended = true;
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            multiBlock.writeStage(this.getPlayer(), 0);
        }
        if (!finished) return;
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.ITEM_CRACK)
                .count(4)
                .offset(1, 0.1, 0.1)
                .extra(0.1);
        World world = this.getBlock().getWorld();
        int size = this.getMultiBlocks().size() - 1;
        float volume = (float) (1 / Math.log(((size) + 1) * Math.E));
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (!multiBlock.breakThisBlock()) continue;
            Block block = multiBlock.getBlock();
            Material blockType = block.getType();
            BlockData blockData = block.getBlockData().clone();
            Location location = block.getLocation();

            if (plugin.getConfigManager().getIgnoredMaterials().contains(blockType)) continue;
            block.setMetadata("multi-broken", new FixedMetadataValue(plugin, true));
            boolean broken = this.getPlayer().breakBlock(block);
            if (!broken) continue;
            if (multiBlock.getDrops() != null) {
                for (ItemStack drop : multiBlock.getDrops()) {
                    world.dropItemNaturally(location, drop);
                }
            }
            world.playSound(location, blockData.getSoundGroup().getBreakSound(), volume, 1F);
            if (multiBlock.isVisible()) {
                particleBuilder.data(new ItemStack(blockType));
                particleBuilder.location(location.add(0.5, 0, 0.5))
                        .spawn();
            }
        }
    }


    public void playParticles() {
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.BLOCK_CRACK)
                .offset(0.375, 0, 0.375);
        HashMap<Material, BlockData> blockDataHashMap = new HashMap<>();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (multiBlock.getBlock().equals(this.getBlock())) continue;
            if (!multiBlock.isVisible()) continue;
            Block block1 = multiBlock.getBlock();
            BlockData blockData = blockDataHashMap.computeIfAbsent(block1.getType(), data -> block1.getType().createBlockData());
            particleBuilder.location(block1.getLocation()
                    .add(0.5, 1, 0.5))
                    .data(blockData)
                    .spawn();
        }
    }

    public void updateBlockAnimationPacket() {
        float breakSpeed = this.getBlock().getBreakSpeed(this.getPlayer());
        float destroySpeed = 0.000001f + (1 / breakSpeed);
        this.progressBroken += ((float) 1) / destroySpeed;
        this.progressBroken = Math.min(this.progressBroken, 1);
        this.progressBroken = Math.max(this.progressBroken, 0);
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (multiBlock.getBlock().equals(this.getBlock())) continue;
            if (!multiBlock.isVisible()) continue;
            multiBlock.writeStage(this.getPlayer(), this.progressBroken);
        }
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
                    if (!multiBlock.isVisible()) continue;
                    multiBlock.writeStage(getPlayer(), 0);
                }
                end(false, plugin);
            }
        }.runTaskLater(plugin, 2);
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

    public List<MultiBlock> getMultiBlocks() {
        return multiBlocks;
    }

    public Vector getPlayerDirection() {
        return playerDirection;
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            blocks.add(multiBlock.getBlock());
        }
        return blocks;
    }

    public boolean hasEnded() {
        return ended;
    }

    public void setProgressTicks(int progressTicks) {
        this.progressTicks = progressTicks;
    }

    public void setProgressBroken(float progressBroken) {
        this.progressBroken = progressBroken;
    }

    public float getProgressBroken() {
        return progressBroken;
    }

    @Override
    public String toString() {
        return "MultiBreak{" +
                "p=" + p +
                ", block=" + block +
                ", playerDirection=" + playerDirection +
                ", progressTicks=" + progressTicks +
                ", multiBlocks=" + multiBlocks +
                ", ended=" + ended +
                '}';
    }
}
