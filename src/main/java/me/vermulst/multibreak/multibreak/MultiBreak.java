package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.runnables.WriteParticleRunnable;
import me.vermulst.multibreak.multibreak.runnables.WriteStageRunnable;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;

public class MultiBreak {

    public static record IntVector(int x, int y, int z) {
        public static IntVector of(Vector v) {
            return new IntVector((int) v.getX(), (int) v.getY(), (int) v.getZ());
        }
    }

    private final UUID playerUUID;
    private Set<UUID> nearbyPlayers;
    private Block block;
    private IntVector playerDirection;
    private int progressTicks; // ticks
    private float progressBroken; // 0 - 1.0
    private int lastStage = -1;
    private List<MultiBlock> multiBlocks;
    private boolean ended = false;

    private final ParticleBuilder particleBuilder = new ParticleBuilder(Particle.BLOCK_CRUMBLE).extra(0.2);

    public MultiBreak(Player p, Block block, Vector playerDirection, Figure figure) {
        this.playerUUID = p.getUniqueId();
        this.nearbyPlayers = getNearbyPlayerUUIDs(block.getLocation());
        this.updateParticleBuilderReceivers(this.nearbyPlayers);
        this.block = block;
        this.playerDirection = IntVector.of(playerDirection);
        this.initBlocks(p, figure);
        this.progressBroken = this.getBlock().getBreakSpeed(p);
    }

    public void reset(Player p, Block block, Vector playerDirection, Figure figure) {
        this.nearbyPlayers = getNearbyPlayerUUIDs(block.getLocation());
        this.updateParticleBuilderReceivers(this.nearbyPlayers);
        this.progressTicks = 0;
        this.ended = false;

        this.block = block;
        this.playerDirection = IntVector.of(playerDirection);

        this.multiBlocks.clear();
        this.initBlocks(p, figure);

        this.progressBroken = block.getBreakSpeed(p);
        this.lastStage = -1;
    }

    public void initBlocks(Player p, Figure figure) {
        if (figure == null) return;
        int capacity = figure.getFigureType().getSize(figure.getWidth(), figure.getHeight(), figure.getDepth());
        this.multiBlocks = new ArrayList<>(capacity);
        Set<Block> blocks = figure.getBlocks(p, this.getBlock());
        for (Block block : blocks) {
            MultiBlock multiBlock = new MultiBlock(block);
            this.getMultiBlocks().add(multiBlock);
        }
    }

    public void tick() {
        Player p = this.getPlayer();
        if (p == null) {
            this.end(p, false);
            return;
        }

        this.progressTicks++;
        if (this.progressTicks % 20 == 0) this.checkPlayers();
        this.checkRemove();

        List<MultiBlock> multiBlockSnapshot = new ArrayList<>(this.multiBlocks);
        if (this.progressTicks % 2 == 0) this.playParticles(multiBlockSnapshot);
        updateBlockAnimationPacket(p, multiBlockSnapshot);
    }

    public void end(Player p, boolean finished) {
        this.ended = true;
        this.writeStage(-1, this.multiBlocks);
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
            boolean broken = p.breakBlock(block);
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

    public void updateBlockAnimationPacket(Player p, List<MultiBlock> multiBlockSnapshot) {
        float breakSpeed = this.getBlock().getBreakSpeed(p);
        this.progressBroken += breakSpeed;
        this.progressBroken = Math.min(this.progressBroken, 1.0f);
        this.progressBroken = Math.max(this.progressBroken, 0.0f);

        /*int tickDelay = (p.getPing() / 50);

        // adjust by 1 tick in the future
        float adjustedProgress = this.progressBroken + (tickDelay * progress);
        adjustedProgress = Math.min(adjustedProgress, 1.0f);*/
        int stage = (int) (9 * progressBroken);
        if (lastStage == -1 || stage != this.lastStage) {
            this.lastStage = stage;
            this.writeStage(stage, multiBlockSnapshot);
        }
    }

    public void writeStage(int stage, List<MultiBlock> multiBlockSnapshot) {
        this.writeStage(this.nearbyPlayers, stage, multiBlockSnapshot);
    }

    public void writeStage(Collection<UUID> uuids, int stage, List<MultiBlock> multiBlockSnapshot) {
        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID uuid : uuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                onlinePlayers.add(player);
            }
        }
        List<MultiBlock> multiBlocksSnapshot = new ArrayList<>(multiBlocks);
        WriteStageRunnable writeStageRunnable = new WriteStageRunnable(this.playerUUID, multiBlocksSnapshot, this.getBlock(), stage, onlinePlayers);
        writeStageRunnable.runTaskAsynchronously(Main.getInstance());
    }

    public void checkPlayers() {
        Set<UUID> oldNearbyPlayers = this.nearbyPlayers;
        Location blockLoc = this.block.getLocation();

        Set<UUID> newNearbyPlayers = this.getNearbyPlayerUUIDs(blockLoc);
        this.nearbyPlayers.addAll(newNearbyPlayers);

        if (newNearbyPlayers.size() != oldNearbyPlayers.size()) {
            this.nearbyPlayers = newNearbyPlayers;
            this.updateParticleBuilderReceivers(this.nearbyPlayers);
        }
    }

    private void updateParticleBuilderReceivers(Set<UUID> playerUUIDs) {
        List<Player> onlinePlayers = new ArrayList<>();
        for (UUID uuid : playerUUIDs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }
        this.particleBuilder.receivers(onlinePlayers);
    }

    public void checkRemove() {
        Iterator<MultiBlock> iterator = this.multiBlocks.iterator();
        List<MultiBlock> blocksToStageZero = null;

        Map<Location, Integer> multiblockMap = BreakManager.getInstance().getMultiblockMap();
        while (iterator.hasNext()) {
            MultiBlock multiBlock = iterator.next();
            if (!multiblockMap.containsKey(multiBlock.getLocation())) {
                if (blocksToStageZero == null) {
                    blocksToStageZero = new ArrayList<>();
                }
                blocksToStageZero.add(multiBlock);
                iterator.remove(); // The fastest way to remove during iteration
            }
        }

        if (blocksToStageZero != null && !blocksToStageZero.isEmpty()) {
            this.writeStage(this.nearbyPlayers, -1, blocksToStageZero);
        }
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

    public void checkValid(Player p, float progressPerTick, EnumSet<Material> includedMaterials, EnumSet<Material> excludedMaterials) {
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
            float blockProgressPerTick = multiBlock.getBlock().getBreakSpeed(p);
            if (progressPerTick == Float.POSITIVE_INFINITY && blockProgressPerTick < progressPerTick) {
                toRemove.add(multiBlock);
            } else if (blockProgressPerTick == Float.POSITIVE_INFINITY) {
                multiBlock.setVisible(false);
            }
        }
        this.getMultiBlocks().removeAll(toRemove);
    }


    public void playParticles(List<MultiBlock> multiBlockSnapshot) {
        boolean playerDirectionX = (playerDirection.x == 1);
        boolean playerDirectionY = (playerDirection.y == 1);
        boolean playerDirectionZ = (playerDirection.z == 1);
        double offsetX = 0.45, offsetY = 0.45, offsetZ = 0.45;
        if (playerDirectionX) offsetX = 0.0;
        if (playerDirectionY) offsetY = 0.0;
        if (playerDirectionZ) offsetZ = 0.0;
        particleBuilder.offset(offsetX, offsetY, offsetZ);

        double sideOffsetX = (playerDirectionX) ? 0.5 : 0;
        double sideOffsetY = (playerDirectionY) ? 0.5 : 0;
        double sideOffsetZ = (playerDirectionZ) ? 0.5 : 0;
        Location loc = new Location(this.getBlock().getWorld(), 0, 0, 0);
        WriteParticleRunnable writeParticleRunnable = new WriteParticleRunnable(multiBlockSnapshot, this.getBlock(), particleBuilder, sideOffsetX, sideOffsetY, sideOffsetZ, loc);
        writeParticleRunnable.runTaskAsynchronously(Main.getInstance());
    }

    public Set<UUID> getNearbyPlayerUUIDs(Location blockLoc) {
        Set<UUID> uuids = new HashSet<>();
        int centerChunkX = blockLoc.getChunk().getX();
        int centerChunkZ = blockLoc.getChunk().getZ();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Chunk chunk = blockLoc.getWorld().getChunkAt(centerChunkX + dx, centerChunkZ + dz);
                if (chunk.isLoaded()) {
                    for (org.bukkit.entity.Entity entity : chunk.getEntities()) {
                        if (entity instanceof Player) {
                            uuids.add(entity.getUniqueId());
                        }
                    }
                }
            }
        }
        return uuids;
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

    public Set<UUID> getNearbyPlayers() {
        return nearbyPlayers;
    }

    public IntVector getPlayerDirection() {
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
