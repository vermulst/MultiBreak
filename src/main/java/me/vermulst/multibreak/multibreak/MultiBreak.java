package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.utils.CompassDirection;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.Matrix4x4;
import me.vermulst.multibreak.figure.VectorTransformer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
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


    public MultiBreak(Player p, Block block, Vector playerDirection, Figure figure) {
        this.p = p;
        this.block = block;
        this.playerDirection = playerDirection;
        this.initBlocks(figure);
        float progressPerTick = this.getBlock().getBreakSpeed(this.getPlayer());
        float destroySpeedTicks = 0.000001f + (1 / progressPerTick);
        this.progressBroken = ((float) 1) / destroySpeedTicks;
    }

    public void setFigure(Figure figure) {
        this.initBlocks(figure);
    }

    public boolean isValid(EnumSet<Material> includedMaterials, EnumSet<Material> excludedMaterials) {
        Material mainBlockType = this.getBlock().getType();
        if (includedMaterials != null && !includedMaterials.isEmpty() && !includedMaterials.contains(mainBlockType)) {
            return false;
        }
        if (excludedMaterials != null && !excludedMaterials.isEmpty() && excludedMaterials.contains(mainBlockType)) {
            return false;
        }
        return true;
    }

    public void checkValid(float progressPerTick, EnumSet<Material> includedMaterials, EnumSet<Material> excludedMaterials) {
        if (includedMaterials != null && !includedMaterials.isEmpty()) {
            this.getMultiBlocks().removeIf(multiBlock -> !includedMaterials.contains(multiBlock.getBlock().getType()));
        }
        if (excludedMaterials != null && !excludedMaterials.isEmpty()) {
            this.getMultiBlocks().removeIf(multiBlock -> excludedMaterials.contains(multiBlock.getBlock().getType()));
        }
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getType().equals(Material.AIR));
        ConfigManager config = Main.getInstance().getConfigManager();
        boolean fairMode = config.getOptions()[0];

        if (!fairMode) return;
        List<MultiBlock> toRemove = new ArrayList<>();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            float blockProgressPerTick = multiBlock.getBlock().getBreakSpeed(this.getPlayer());
            if (progressPerTick == Float.POSITIVE_INFINITY && blockProgressPerTick < progressPerTick) {
                toRemove.add(multiBlock);
            } else if (blockProgressPerTick == Float.POSITIVE_INFINITY) {
                multiBlock.setVisible(false);
            }
        }
        this.getMultiBlocks().removeAll(toRemove);
    }



    public void initBlocks(Figure figure) {
        this.multiBlocks = new ArrayList<>();
        if (figure == null) return;
        Set<Block> blocks = figure.getBlocks(p, this.getBlock());
        for (Block block : blocks) {
            MultiBlock multiBlock = new MultiBlock(block);
            this.getMultiBlocks().add(multiBlock);
        }
    }

    public void tick() {
        this.progressTicks++;
        this.checkRemove();
        if (this.progressTicks % 2 == 0) this.playParticles();
        updateBlockAnimationPacket();
    }

    public void checkRemove() {
        List<MultiBlock> blocksToRemove = this.getMultiBlocks().stream()
                .filter(MultiBlock::mismatchesType)
                .toList();

        for (MultiBlock multiBlock : blocksToRemove) {
            multiBlock.writeStage(this.getPlayer(), 0);
        }

        this.getMultiBlocks().removeAll(blocksToRemove);
    }

    public void end(boolean finished, Main plugin) {
        this.ended = true;
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            multiBlock.writeStage(this.getPlayer(), 0);
        }
        if (!finished) return;
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.BLOCK)
                .count(16)
                .offset(0.5, 0.5, 0.5);
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
                particleBuilder.location(location.add(0.5, 0.5, 0.5))
                        .data(blockData)
                        .spawn();
            }
        }
    }


    public void playParticles() {
        Vector finalOffset = new Vector(0.45, 0.45, 0.45);
        if (this.getPlayerDirection().getX() != 0) {
            finalOffset.setX(0.0);
        }
        if (this.getPlayerDirection().getY() != 0) {
            finalOffset.setY(0.0);
        }
        if (this.getPlayerDirection().getZ() != 0) {
            finalOffset.setZ(0.0);
        }
        ParticleBuilder particleBuilder = new ParticleBuilder(Particle.BLOCK_CRUMBLE)
                .offset(finalOffset.getX(), finalOffset.getY(), finalOffset.getZ());
        Vector playerDirectionTimesHalf = this.playerDirection.clone().multiply(0.5001);
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (multiBlock.getBlock().equals(this.getBlock())) continue;
            if (!multiBlock.isVisible()) continue;
            Block block1 = multiBlock.getBlock();
            BlockData blockData = block1.getBlockData();
            particleBuilder.location(block1.getLocation()
                    .add(0.5, 0.5, 0.5).add(playerDirectionTimesHalf))
                    .data(blockData)
                    .extra(0.2)
                    .spawn();
        }
    }

    public void updateBlockAnimationPacket() {
        float breakSpeed = this.getBlock().getBreakSpeed(this.getPlayer());
        float destroySpeed = 0.000001f + (1 / breakSpeed);
        this.progressBroken += ((float) 1) / destroySpeed;
        this.progressBroken = Math.min(this.progressBroken, 1.0f);
        this.progressBroken = Math.max(this.progressBroken, 0.0f);
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            if (multiBlock.getBlock().equals(this.getBlock())) continue;
            if (!multiBlock.isVisible()) continue;
            multiBlock.writeStage(this.getPlayer(), this.progressBroken);
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

    public List<MultiBlock> getMultiBlocks() {
        return multiBlocks;
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

    public Vector getPlayerDirection() {
        return playerDirection;
    }

    @Override
    public String toString() {
        return "MultiBreak{" +
                "p=" + p +
                ", block=" + block +
                ", progressTicks=" + progressTicks +
                ", multiBlocks=" + multiBlocks +
                ", ended=" + ended +
                '}';
    }



    /*public void slowDown(Player p, float slowDownFactor, ItemStack tool) {
        double currentAttributeTotal = p.getAttribute(Attribute.BLOCK_BREAK_SPEED).getValue();
        double newAttributeTotal = currentAttributeTotal * slowDownFactor;
        double currentAttributePlayer = p.getAttribute(Attribute.BLOCK_BREAK_SPEED).getBaseValue();
        double newAttributePlayer = currentAttributePlayer - (currentAttributePlayer - newAttributeTotal);

        // store original player attribute
        p.setMetadata("multibreak-original-break-speed", new FixedMetadataValue(Main.getInstance(), currentAttributePlayer));
        AttributeInstance attribute = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        AttributeModifier slowDownModifier = new AttributeModifier(
                new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"),
                -(currentAttributePlayer - newAttributeTotal),
                AttributeModifier.Operation.ADD_NUMBER
        );
        attribute.addModifier(slowDownModifier);
    }

    public float getSlowDownFactor(float baseProgressPerTick) {
        float lowestProgressPerTick = baseProgressPerTick;
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            float progressPerTick = multiBlock.getBlock().getBreakSpeed(this.getPlayer());
            if (progressPerTick < baseProgressPerTick) {
                lowestProgressPerTick = progressPerTick;
            }
        }
        return lowestProgressPerTick / baseProgressPerTick;
    }

    public void restore(Player p) {
        if (!p.hasMetadata("multibreak-original-break-speed")) return;
        AttributeInstance attribute = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        AttributeModifier modifierToRemove = attribute.getModifier(new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"));
        if (modifierToRemove != null) {
            attribute.removeModifier(modifierToRemove);
        }

        /*double originalValue = p.getMetadata("multibreak-original-break-speed").get(0).asDouble();
        p.getAttribute(Attribute.PLAYER_BLOCK_BREAK_SPEED).setBaseValue(originalValue);//
        p.removeMetadata("multibreak-original-break-speed", Main.getInstance());
    }*/
}
