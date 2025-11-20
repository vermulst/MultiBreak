package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import me.vermulst.multibreak.multibreak.runnables.WriteParticleRunnable;
import me.vermulst.multibreak.multibreak.runnables.WriteStageRunnable;
import me.vermulst.multibreak.utils.BlockFilter;
import me.vermulst.multibreak.utils.IntVector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class MultiBreak {

    private static final int CHECK_PLAYERS_INTERVAL = 32;
    private static final int PLAY_PARTICLES_INTERVAL = 2;
    private static final double CHECK_PLAYERS_RADIUS = 32;

    private final UUID playerUUID;
    private Set<UUID> nearbyPlayers;
    private ServerGamePacketListenerImpl[] nearbyPlayerConnections;
    private Block block;
    private IntVector playerDirection;
    private int progressTicks; // ticks
    private float progressBroken; // 0 - 1.0
    private int lastStage = -1;
    private MultiBlock[] multiBlocks;
    private final ReentrantLock packetLock = new ReentrantLock();
    private volatile int ended = -1; // tick at which it was broken, 0 = canceled

    // static break
    private boolean paused = false;
    private int lastTick = -1;

    private final Map<Material, Float> destroySpeedCache = new EnumMap<>(Material.class);
    private final Map<Material, Boolean> hasCorrectToolCache = new EnumMap<>(Material.class);

    private final ParticleBuilder particleBuilder = new ParticleBuilder(Particle.BLOCK_CRUMBLE)
            .extra(0.2);
    private final ParticleBuilder breakParticleBuilder = new ParticleBuilder(Particle.BLOCK)
            .count(16)
            .offset(0.5, 0.5, 0.5);

    // State caching
    private boolean isGrounded;
    private boolean isSubmerged;
    private ServerLevel serverLevel;
    private BlockPos blockPos;
    private BlockState blockState;

    public MultiBreak(UUID uuid) {
        this.playerUUID = uuid;
    }

    public MultiBreak(Player p, Block block, Vector playerDirection, @NotNull Figure figure, EnumSet<Material> includedMaterials, EnumSet<Material> ignoredMaterials) {
        this.serverLevel = ((CraftWorld)block.getWorld()).getHandle();
        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        this.blockPos = CraftLocation.toBlockPosition(block.getLocation());
        this.blockState = serverLevel.getBlockState(this.blockPos);
        this.isGrounded = p.isOnGround();
        this.isSubmerged = serverPlayer.isEyeInFluid(FluidTags.WATER);

        this.playerUUID = p.getUniqueId();
        this.nearbyPlayers = getNearbyPlayerUUIDs(block.getLocation());
        this.updateNearbyPlayerConnections();
        this.updateParticleBuilderReceivers(this.nearbyPlayers);
        this.block = block;
        this.playerDirection = IntVector.of(playerDirection);
        this.initBlocks(p, figure, playerDirection, includedMaterials, ignoredMaterials);

        this.progressBroken = this.getDestroySpeedMain(serverPlayer);
    }

    public void reset(Player p, Block block, Vector playerDirection, @NotNull Figure figure, EnumSet<Material> includedMaterials, EnumSet<Material> ignoredMaterials) {
        this.serverLevel = ((CraftWorld)block.getWorld()).getHandle();
        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        this.blockPos = CraftLocation.toBlockPosition(block.getLocation());
        this.blockState = serverLevel.getBlockState(this.blockPos);
        this.isGrounded = p.isOnGround();
        this.isSubmerged = serverPlayer.isEyeInFluid(FluidTags.WATER);

        this.nearbyPlayers = getNearbyPlayerUUIDs(block.getLocation());
        this.updateNearbyPlayerConnections();
        this.updateParticleBuilderReceivers(this.nearbyPlayers);
        this.progressTicks = 0;
        this.ended = -1;

        this.block = block;
        this.playerDirection = IntVector.of(playerDirection);
        this.initBlocks(p, figure, playerDirection, includedMaterials, ignoredMaterials);

        this.progressBroken = this.getDestroySpeedMain(serverPlayer);
        this.lastStage = -1;
        this.paused = false;
    }


    public float getDestroySpeedMain(Player p) {
        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        return this.getDestroySpeedMain(serverPlayer);
    }

    public float getDestroySpeedMain(ServerPlayer serverPlayer) {
        return this.getDestroySpeed(serverPlayer, this.blockPos, this.blockState);
    }

    // override main block pos with other block pos
    public float getDestroySpeed(ServerPlayer serverPlayer, BlockPos blockPos) {
        BlockState blockState = serverLevel.getBlockState(blockPos);
        return this.getDestroySpeed(serverPlayer, blockPos, blockState);
    }

    public float getDestroySpeed(ServerPlayer serverPlayer, BlockPos blockPos, BlockState blockState) {
        Material material = blockState.getBukkitMaterial();
        if (destroySpeedCache.containsKey(material)) {
            return destroySpeedCache.get(material);
        }

        float destroySpeed = blockState.getDestroySpeed(serverLevel, blockPos);
        if (destroySpeed == -1.0F) {
            return 0.0F;
        } else {
            float baseSpeed = serverPlayer.getDestroySpeed(blockState);
            boolean hasCorrectTool = hasCorrectToolCache.computeIfAbsent(material,
                    mat -> serverPlayer.hasCorrectToolForDrops(blockState));
            int factor = hasCorrectTool ? 30 : 100;
            float finalSpeed = baseSpeed / destroySpeed / (float)factor;
            destroySpeedCache.put(material, finalSpeed);
            return finalSpeed;
        }
    }

    public void initBlocks(Player p, @NotNull Figure figure, Vector blockFaceDirection, EnumSet<Material> includedMaterials, EnumSet<Material> ignoredMaterials) {
        boolean fairMode = Config.getInstance().isFairModeEnabled();
        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        float mainBlockProgressPerTick = this.getDestroySpeed(serverPlayer, this.blockPos);

        Set<Block> blocks = figure.getBlocks(p, this.getBlock(), blockFaceDirection);
        Set<MultiBlock> multiBlocks = new HashSet<>(blocks.size());
        int uuidHash = this.playerUUID.hashCode();
        int worldHash = this.block.getWorld().getUID().hashCode();
        int baseHash = getBaseHash(uuidHash, worldHash);
        for (Block block : blocks) {
            Material material = block.getType();
            if (BlockFilter.isExcluded(material, includedMaterials, ignoredMaterials)) continue;
            int sourceID = getSourceID(baseHash, block.getX(), block.getY(), block.getZ());
            MultiBlock multiBlock = new MultiBlock(block, sourceID);
            BlockPos blockPos = CraftLocation.toBlockPosition(block.getLocation());
            float blockProgressPerTick = this.getDestroySpeed(serverPlayer, blockPos);
            if (blockProgressPerTick == Float.POSITIVE_INFINITY) {
                multiBlock.setVisible(false); // dont show animation
            } else if (fairMode && mainBlockProgressPerTick == Float.POSITIVE_INFINITY) {
                continue; // skip if block takes > 1 ticks and main block is instant
            }
            multiBlocks.add(multiBlock);
        }
        this.multiBlocks = multiBlocks.toArray(new MultiBlock[0]);
    }

    public void tick() {
        Player p = this.getPlayer();
        if (p == null) {
            this.end(p, false);
            return;
        }
        this.checkPause();
        if (this.paused) return;

        this.progressTicks++;
        if ((this.progressTicks & (CHECK_PLAYERS_INTERVAL - 1)) == 0) this.checkPlayers();
        this.checkDestroySpeedChange(p);
        //this.checkRemove();

        MultiBlock[] multiBlockSnapshot = this.getMultiBlockSnapshot();
        if ((this.progressTicks & (PLAY_PARTICLES_INTERVAL - 1)) == 0) this.playParticles(multiBlockSnapshot);
        updateBlockAnimationPacket(p, multiBlockSnapshot);
    }


    public void checkPause() {
        if (this.isNotStatic()) return;
        int currentTick = Bukkit.getServer().getCurrentTick();
        this.paused = (currentTick - this.lastTick) > 1;
    }


    public void checkDestroySpeedChange(Player p) {
        boolean isGrounded = p.isOnGround();
        boolean isSubmerged = ((CraftPlayer)p).getHandle().isEyeInFluid(FluidTags.WATER);

        if (isGrounded != this.isGrounded || isSubmerged != this.isSubmerged) {
            this.isGrounded = isGrounded;
            this.isSubmerged = isSubmerged;
            this.invalidateDestroySpeedCache();
        }
    }


    public void end(Player p, boolean finished) {
        this.ended = finished ? Bukkit.getCurrentTick() : 0;
        MultiBlock[] multiBlockSnapshot = this.getMultiBlockSnapshot();
        this.writeStage(-1, multiBlockSnapshot);
        if (!finished) return;
        boolean playSound = p.getPing() < Config.getInstance().getPlaySoundPingTreshold();
        int size = multiBlockSnapshot.length - 1;
        World world = playSound ? this.getBlock().getWorld() : null;
        float volume = playSound ? (float) (1 / Math.log(((size) + 1) * Math.E)) : 0F;

        Map<Material, BlockData> blockDataCache = new EnumMap<>(Material.class);
        for (MultiBlock multiBlock : multiBlockSnapshot) {
            Block block = multiBlock.getBlock();
            Material blockType = block.getType();

            if (Config.getInstance().getIgnoredMaterials().contains(blockType)) continue;
            block.setMetadata("multi-broken", new FixedMetadataValue(Main.getInstance(), true));
            BlockData blockData = blockDataCache.computeIfAbsent(block.getType(), Material::createBlockData);
            Location location = block.getLocation();
            boolean broken = p.breakBlock(block);
            if (!broken) continue;
            if (playSound) {
                world.playSound(location, blockData.getSoundGroup().getBreakSound(), volume, 1F);
            }

            if (multiBlock.isVisible()) {
                breakParticleBuilder
                        .location(location.add(0.5, 0.5, 0.5))
                        .data(blockData)
                        .spawn();
            }
        }
    }

    public void updateBlockAnimationPacket(Player p, MultiBlock[] multiBlockSnapshot) {
        float breakSpeed = this.getDestroySpeedMain(p);
        this.progressBroken += breakSpeed;
        this.progressBroken = Math.min(this.progressBroken, 1.0f);
        this.progressBroken = Math.max(this.progressBroken, 0.0f);

        float tickDelay = ((float) p.getPing() / 50);

        // adjust by 1 tick in the future
        float adjustedProgress = this.progressBroken + (tickDelay * breakSpeed);
        adjustedProgress = Math.min(adjustedProgress, 1.0f);

        int stage = (int) (9 * adjustedProgress);

        if (lastStage == -1 || stage > this.lastStage) {
            this.writeStage(stage, multiBlockSnapshot);
            this.lastStage = stage;
        }
    }


    public void writeStage(int stage, MultiBlock[] multiBlockSnapshot) {
        WriteStageRunnable writeStageRunnable = new WriteStageRunnable(
                multiBlockSnapshot,
                this.getBlock(),
                stage,
                this.nearbyPlayerConnections,
                this.packetLock,
                this
        );
        Main.getHighPriorityExecutor().submit(writeStageRunnable);
    }

    public void checkPlayers() {
        Set<UUID> oldNearbyPlayers = this.nearbyPlayers;
        Location blockLoc = this.block.getLocation();

        Set<UUID> newNearbyPlayers = this.getNearbyPlayerUUIDs(blockLoc);
        this.nearbyPlayers.addAll(newNearbyPlayers);

        if (newNearbyPlayers.size() != oldNearbyPlayers.size()) {
            this.nearbyPlayers = newNearbyPlayers;
            this.updateNearbyPlayerConnections();
            this.updateParticleBuilderReceivers(this.nearbyPlayers);
        }
    }

    private void updateParticleBuilderReceivers(Set<UUID> playerUUIDs) {
        List<Player> onlinePlayers = new ArrayList<>(playerUUIDs.size());
        for (UUID uuid : playerUUIDs) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                onlinePlayers.add(player);
            }
        }
        this.particleBuilder.receivers(onlinePlayers);
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

    public void playParticles(MultiBlock[] multiBlocksSnapshot) {
        boolean playerDirectionX = (playerDirection.x() == 1);
        boolean playerDirectionY = (playerDirection.y() == 1);
        boolean playerDirectionZ = (playerDirection.z() == 1);
        double offsetX = 0.45, offsetY = 0.45, offsetZ = 0.45;
        if (playerDirectionX) offsetX = 0.0;
        if (playerDirectionY) offsetY = 0.0;
        if (playerDirectionZ) offsetZ = 0.0;
        particleBuilder.offset(offsetX, offsetY, offsetZ);

        double sideOffsetX = (playerDirectionX) ? 0.5 : 0;
        double sideOffsetY = (playerDirectionY) ? 0.5 : 0;
        double sideOffsetZ = (playerDirectionZ) ? 0.5 : 0;
        Location loc = new Location(this.getBlock().getWorld(), 0, 0, 0);
        WriteParticleRunnable writeParticleRunnable = new WriteParticleRunnable(multiBlocksSnapshot, this.getBlock(), particleBuilder, sideOffsetX, sideOffsetY, sideOffsetZ, loc);
        Main.getHighPriorityExecutor().submit(writeParticleRunnable);
    }

    private static final Predicate<net.minecraft.world.entity.Entity> isPlayer =
            (nmsEntity) -> nmsEntity.getType() == net.minecraft.world.entity.EntityType.PLAYER;

    // 2-6x faster than API version
    public Set<UUID> getNearbyPlayerUUIDs(Location blockLoc) {
        Set<UUID> uuids = new HashSet<>();

        double x = blockLoc.getX();
        double y = blockLoc.getY();
        double z = blockLoc.getZ();
        AABB aabb = new AABB(
                x - CHECK_PLAYERS_RADIUS, y - CHECK_PLAYERS_RADIUS, z - CHECK_PLAYERS_RADIUS,
                x + CHECK_PLAYERS_RADIUS, y + CHECK_PLAYERS_RADIUS, z + CHECK_PLAYERS_RADIUS
        );

        List<net.minecraft.world.entity.Entity> entityList = this.serverLevel.getEntities((net.minecraft.world.entity.Entity) null, aabb, isPlayer);

        for (net.minecraft.world.entity.Entity entity : entityList) {
            ServerPlayer serverPlayer = (ServerPlayer) entity;
            uuids.add(serverPlayer.getUUID());
        }
        return uuids;
    }

    public void updateNearbyPlayerConnections() {
        ServerGamePacketListenerImpl[] connections = new ServerGamePacketListenerImpl[this.nearbyPlayers.size()];
        int index = 0;
        for (UUID uuid : this.nearbyPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            connections[index++] = ((CraftPlayer)p).getHandle().connection;
        }
        this.nearbyPlayerConnections = connections;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.playerUUID);
    }

    public Block[] getBlocks() {
        MultiBlock[] multiBlocks = this.getMultiBlocks();
        Block[] blocks = new Block[multiBlocks.length];
        for (int i = 0; i < multiBlocks.length; i++) {
            blocks[i] = multiBlocks[i].getBlock();
        }
        return blocks;
    }

    public Block getBlock() {
        return block;
    }

    public int getProgressTicks() {
        return progressTicks;
    }

    public MultiBlock[] getMultiBlocks() {
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
        return ended != -1;
    }

    public int getEnded() {
        return ended;
    }

    public void setEnded(int ended) {
        this.ended = ended;
    }

    public boolean isNotStatic() {
        return lastTick == -1;
    }

    public void setProgressTicks(int progressTicks) {
        this.progressTicks = progressTicks;
    }

    public void setProgressBroken(float progressBroken) {
        this.progressBroken = progressBroken;
    }

    public void setLastTick(int lastTick) {
        this.lastTick = lastTick;
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

    public MultiBlock[] getMultiBlockSnapshot() {
        return Arrays.stream(this.multiBlocks)
                .filter(Objects::nonNull)
                .toArray(MultiBlock[]::new);
    }

    public void invalidateDestroySpeedCache() {
        this.destroySpeedCache.clear();
    }

    public void invalidateHasCorrectToolCache() {
        this.hasCorrectToolCache.clear();
    }

    private static int getBaseHash(int uuidHash, int worldHash) {
        int hash = 17;
        hash = 31 * hash + uuidHash;
        hash = 31 * hash + worldHash;
        return hash;
    }

    private static int getSourceID(int baseHash, int x, int y, int z) {
        int hash = baseHash;
        hash = 31 * hash + x;
        hash = 31 * hash + y;
        hash = 31 * hash + z;
        return hash;
    }
}
