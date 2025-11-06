package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MultiBreak {

    private final UUID playerUUID;
    private Set<UUID> nearbyPlayers;
    private final Block block;
    private final Vector playerDirection;
    private int progressTicks;
    private float progressBroken;
    private List<MultiBlock> multiBlocks = new ArrayList<>();
    private boolean ended = false;

    public MultiBreak(Player p, Block block, Vector playerDirection, Figure figure) {
        this.playerUUID = p.getUniqueId();
        this.nearbyPlayers = getNearbyPlayers(block.getLocation());
        this.block = block;
        this.playerDirection = playerDirection;
        this.initBlocks(figure);
        float progressPerTick = this.getBlock().getBreakSpeed(this.getPlayer());
        float destroySpeedTicks = 0.000001f + (1 / progressPerTick);
        this.progressBroken = ((float) 1) / destroySpeedTicks;
    }

    public void initBlocks(Figure figure) {
        Player p = this.getPlayer();
        if (p == null) return;
        this.multiBlocks = new ArrayList<>();
        if (figure == null) return;
        Set<Block> blocks = figure.getBlocks(p, this.getBlock());
        for (Block block : blocks) {
            MultiBlock multiBlock = new MultiBlock(block);
            this.getMultiBlocks().add(multiBlock);
        }
    }

    public void tick() {
        if (this.getPlayer() == null) {
            this.end(false);
            return;
        }
        this.progressTicks++;
        if (this.progressTicks % 20 == 0) this.checkPlayers();
        this.checkRemove();
        if (this.progressTicks % 2 == 0) this.playParticles();
        updateBlockAnimationPacket();
    }

    public void end(boolean finished) {
        this.ended = true;
        this.writeStage(0);
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

            if (Config.getInstance().getIgnoredMaterials().contains(blockType)) continue;
            block.setMetadata("multi-broken", new FixedMetadataValue(Main.getInstance(), true));
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

    public void updateBlockAnimationPacket() {
        float breakSpeed = this.getBlock().getBreakSpeed(this.getPlayer());
        float destroySpeed = 0.000001f + (1 / breakSpeed);
        this.progressBroken += ((float) 1) / destroySpeed;
        this.progressBroken = Math.min(this.progressBroken, 1.0f);
        this.progressBroken = Math.max(this.progressBroken, 0.0f);
        this.writeStage(this.progressBroken);
    }

    public void writeStage(float stage) {
        this.writeStage(this.nearbyPlayers, stage);
    }

    public void writeStage(Collection<UUID> uuids, float stage) {
        Collection<Player> onlinePlayers = uuids.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (MultiBlock multiBlock : this.multiBlocks) {
            if (multiBlock.getBlock().equals(this.getBlock())) continue;
            if (!multiBlock.isVisible()) continue;
            multiBlock.writeStage(onlinePlayers, stage);
        }
    }

    public void checkPlayers() {
        Set<UUID> oldNearbyPlayers = this.nearbyPlayers;
        Location blockLoc = this.block.getLocation();

        Set<UUID> newNearbyPlayers = this.getNearbyPlayers(blockLoc);

        Set<UUID> departedPlayers = new HashSet<>(oldNearbyPlayers);
        departedPlayers.removeAll(newNearbyPlayers);

        this.writeStage(departedPlayers, 0);

        this.nearbyPlayers = newNearbyPlayers;
    }

    public void checkRemove() {
        List<MultiBlock> blocksToRemove = this.getMultiBlocks().stream()
                .filter(MultiBlock::mismatchesType)
                .toList();

        this.writeStage(0);

        this.getMultiBlocks().removeAll(blocksToRemove);
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
        boolean fairMode = Config.getInstance().isFairModeEnabled();
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

    public Set<UUID> getNearbyPlayers(Location blockLoc) {
        return blockLoc.getWorld().getNearbyPlayers(blockLoc, 64).stream().map(Player::getUniqueId).collect(Collectors.toSet());
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.playerUUID);
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            blocks.add(multiBlock.getBlock());
        }
        return blocks;
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

    public float getProgressBroken() {
        return progressBroken;
    }

    public Vector getPlayerDirection() {
        return playerDirection;
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

    @Override
    public String toString() {
        return "MultiBreak{" +
                "p=" + this.getPlayer() +
                ", block=" + block +
                ", progressTicks=" + progressTicks +
                ", multiBlocks=" + multiBlocks +
                ", ended=" + ended +
                '}';
    }
}
