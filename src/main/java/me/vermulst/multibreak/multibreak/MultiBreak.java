package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.CompassDirection;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.Matrix4x4;
import me.vermulst.multibreak.figure.VectorTransformer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MultiBreak {

    private final Player p;
    private final Block block;
    private final Vector playerDirection;
    private int progressTicks;
    private ArrayList<MultiBlock> multiBlocks = new ArrayList<>();
    private final int destroySpeedInTicks;
    private boolean ended = false;
    private final boolean fair_mode;
    private final List<Material> ignoredMaterials;


    public MultiBreak(Player p, Block block, Figure figure, Vector playerDirection, boolean fair_mode, List<Material> ignoredMaterials) {
        this.p = p;
        this.block = block;
        this.playerDirection = playerDirection;
        this.fair_mode = fair_mode;
        this.ignoredMaterials = ignoredMaterials;
        this.initBlocks(figure, playerDirection);
        float breakSpeed = this.getBlock().getBreakSpeed(this.getPlayer());
        this.checkValid(breakSpeed);
        this.destroySpeedInTicks = (int) (0.0001 + (1 / breakSpeed));
    }

    public void setFigure(Figure figure) {
        this.initBlocks(figure, this.playerDirection);
        float breakSpeed = this.getBlock().getBreakSpeed(this.getPlayer());
        this.checkValid(breakSpeed);
    }

    public void checkValid(float breakSpeed) {
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getType().equals(Material.AIR));
        if (!this.fair_mode) return;
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getBreakSpeed(this.getPlayer()) < breakSpeed);
    }


    public void initBlocks(Figure figure, Vector playerDirection) {
        this.multiBlocks = new ArrayList<>();
        if (figure == null) return;
        CompassDirection compassDirection = CompassDirection.getCompassDir(this.getPlayer().getLocation());

        VectorTransformer vectorTransformer = new VectorTransformer(playerDirection, compassDirection);
        boolean rotated = figure.getRotationWidth() != 0.0 || figure.getRotationHeight() != 0.0 || figure.getRotationDepth() != 0.0;
        HashSet<Vector> blockVectors = figure.getVectors(rotated);
        if (rotated) {
            HashSet<Vector> rotatedVectors = new HashSet<>();
            Matrix4x4 rotationMatrix = new Matrix4x4();
            rotationMatrix.setRotationX(figure.getRotationWidth() * (Math.PI / 180));
            rotationMatrix.setRotationY(figure.getRotationHeight() * (Math.PI / 180));
            rotationMatrix.setRotationZ(figure.getRotationDepth() * (Math.PI / 180));
            for (Vector vector : blockVectors) {
                rotationMatrix.transform(vector);
                vector.setX(Math.round(vector.getX()));
                vector.setY(Math.round(vector.getY()));
                vector.setZ(Math.round(vector.getZ()));
                rotatedVectors.add(new Vector(Math.round(vector.getX()), Math.round(vector.getY()), Math.round(vector.getZ())));
            }
            blockVectors = rotatedVectors;
        }
        for (Vector vector : blockVectors) {
            vectorTransformer.rotateVector(vector);
        }
        blockVectors.remove(new Vector(0, 0, 0));
        Location loc = this.getBlock().getLocation();
        for (Vector vector : blockVectors) {
            Block block1 = loc.clone().add(vector).getBlock();
            if (this.ignoredMaterials.contains(block1.getType())) continue;
            MultiBlock multiBlock1 = new MultiBlock(block1);
            this.getMultiBlocks().add(multiBlock1);
        }
    }

    public void tick(Main plugin, Block blockMining) {
        if (!blockMining.equals(this.getBlock())) {
            this.end(false, plugin);
        }
        this.progressTicks++;
        this.updateAnimations(this.progressTicks % 2 == 0);
        this.scheduleCancel(plugin, blockMining);
    }

    public void tick() {
        this.progressTicks++;
        this.updateAnimations(this.progressTicks % 2 == 0);
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
        ItemStack tool = this.getPlayer().getInventory().getItemInMainHand();
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
            if (multiBlock.getDrops() == null) {
                for (ItemStack drop : block.getDrops(tool)) {
                    world.dropItemNaturally(location, drop);
                }
            } else {
                for (ItemStack drop : multiBlock.getDrops()) {
                    world.dropItemNaturally(location, drop);
                }
            }
            world.playSound(location, blockData.getSoundGroup().getBreakSound(), volume, 1F);
            if (multiBlock.hasAdjacentAir()) {
                particleBuilder.data(new ItemStack(blockType));
                particleBuilder.location(location.add(0.5, 0, 0.5))
                        .spawn();
            }
        }
    }


    public void updateAnimations(boolean particles) {
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.BLOCK_CRACK)
                .offset(0.375, 0, 0.375);
        HashMap<Material, BlockData> blockDataHashMap = new HashMap<>();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (multiBlock.getBlock().equals(this.getBlock())) continue;
            if (!multiBlock.hasAdjacentAir()) continue;
            Block block1 = multiBlock.getBlock();
            if (!particles) continue;
            BlockData blockData = blockDataHashMap.computeIfAbsent(block1.getType(), data -> block1.getType().createBlockData());
            particleBuilder.location(block1.getLocation()
                    .add(0.5, 1, 0.5))
                    .data(blockData)
                    .spawn();
        }
        updateBlockAnimationPacket();
    }

    public void updateBlockAnimationPacket() {
        float stage = ((float) this.getProgressTicks() / (float) this.getDestroySpeedInTicks());
        stage = Math.min(stage, 1);
        stage = Math.max(stage, 0);
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (multiBlock.getBlock().equals(this.getBlock())) continue;
            if (!multiBlock.hasAdjacentAir()) continue;
            multiBlock.writeStage(this.getPlayer(), stage);
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
                    if (!multiBlock.hasAdjacentAir()) continue;
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

    public ArrayList<MultiBlock> getMultiBlocks() {
        return multiBlocks;
    }

    public ArrayList<Block> getBlocks() {
        ArrayList<Block> blocks = new ArrayList<>();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            blocks.add(multiBlock.getBlock());
        }
        return blocks;
    }

    public int getDestroySpeedInTicks() {
        return destroySpeedInTicks;
    }

    public boolean hasEnded() {
        return ended;
    }

    @Override
    public String toString() {
        return "MultiBreak{" +
                "p=" + p +
                ", block=" + block +
                ", playerDirection=" + playerDirection +
                ", progressTicks=" + progressTicks +
                ", multiBlocks=" + multiBlocks +
                ", destroySpeedInTicks=" + destroySpeedInTicks +
                ", ended=" + ended +
                '}';
    }
}
