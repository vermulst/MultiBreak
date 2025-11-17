package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.ParticleBuilder;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureType;
import me.vermulst.multibreak.multibreak.runnables.WriteParticleRunnable;
import me.vermulst.multibreak.multibreak.runnables.WriteStageRunnable;
import me.vermulst.multibreak.utils.BreakUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
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

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

public class MultiBreak {

    public static record IntVector(int x, int y, int z) {
        public static IntVector of(Vector v) {
            return new IntVector((int) v.getX(), (int) v.getY(), (int) v.getZ());
        }

        public boolean equalsVector(Vector v) {
            // Perform the comparison logic directly
            return this.x == (int) v.getX() &&
                    this.y == (int) v.getY() &&
                    this.z == (int) v.getZ();
        }
    }

    private final UUID playerUUID;
    private Set<UUID> nearbyPlayers;
    private List<ServerGamePacketListenerImpl> nearbyPlayerConnections;
    private Block block;
    private IntVector playerDirection;
    private int progressTicks; // ticks
    private float progressBroken; // 0 - 1.0
    private int lastStage = -1;
    private List<MultiBlock> multiBlocks;
    private volatile boolean ended = false;
    private final ReentrantLock packetLock = new ReentrantLock();

    private final Map<Material, Float> destroySpeedCache = new EnumMap<>(Material.class);
    private final Set<Material> hasCorrectToolCache = EnumSet.noneOf(Material.class);

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


    public MultiBreak(Player p, Block block, Vector playerDirection, Figure figure) {
        this.playerUUID = p.getUniqueId();
        this.nearbyPlayers = getNearbyPlayerUUIDs(block.getLocation());
        this.updateNearbyPlayerConnections();
        this.updateParticleBuilderReceivers(this.nearbyPlayers);
        this.block = block;
        this.playerDirection = IntVector.of(playerDirection);
        this.initBlocks(p, figure, playerDirection);

        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        this.serverLevel = ((CraftWorld)block.getWorld()).getHandle();
        this.blockPos = CraftLocation.toBlockPosition(block.getLocation());
        this.blockState = serverLevel.getBlockState(this.blockPos);
        this.isGrounded = p.isOnGround();
        this.isSubmerged = serverPlayer.isEyeInFluid(FluidTags.WATER);

        this.progressBroken = this.getDestroySpeedMain(serverPlayer);
    }

    public void reset(Player p, Block block, Vector playerDirection, Figure figure) {
        this.nearbyPlayers = getNearbyPlayerUUIDs(block.getLocation());
        this.updateNearbyPlayerConnections();
        this.updateParticleBuilderReceivers(this.nearbyPlayers);
        this.progressTicks = 0;
        this.ended = false;

        this.block = block;
        this.playerDirection = IntVector.of(playerDirection);

        this.multiBlocks.clear();
        this.initBlocks(p, figure, playerDirection);

        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        this.serverLevel = ((CraftWorld)block.getWorld()).getHandle();
        this.blockPos = CraftLocation.toBlockPosition(block.getLocation());
        this.blockState = serverLevel.getBlockState(this.blockPos);
        this.checkDestroySpeedChange(p);

        this.progressBroken = this.getDestroySpeedMain(serverPlayer);
        this.lastStage = -1;
    }


    public float getDestroySpeedMain(Player p) {
        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        return this.getDestroySpeedMain(serverPlayer);
    }

    public float getDestroySpeedMain(ServerPlayer serverPlayer) {
        Material material = blockState.getBukkitMaterial();
        if (destroySpeedCache.containsKey(material)) {
            return destroySpeedCache.get(material);
        }

        float destroySpeed = blockState.getDestroySpeed(serverLevel, blockPos);
        if (destroySpeed == -1.0F) {
            return 0.0F;
        } else {
            float baseSpeed = serverPlayer.getDestroySpeed(blockState);
            boolean hasCorrectTool = hasCorrectToolCache.contains(material);
            if (!hasCorrectTool) {
                hasCorrectTool = serverPlayer.hasCorrectToolForDrops(blockState);
                if (hasCorrectTool) hasCorrectToolCache.add(material);
            }
            int factor = hasCorrectTool ? 30 : 100;
            float finalSpeed = baseSpeed / destroySpeed / (float)factor;
            destroySpeedCache.put(material, finalSpeed);
            return finalSpeed;
        }
    }

    // override main block pos with other block pos
    public float getDestroySpeed(ServerPlayer serverPlayer, BlockPos blockPos) {
        BlockState blockState = serverLevel.getBlockState(blockPos);
        Material material = blockState.getBukkitMaterial();
        if (destroySpeedCache.containsKey(material)) {
            return destroySpeedCache.get(material);
        }

        float destroySpeed = blockState.getDestroySpeed(serverLevel, blockPos);
        if (destroySpeed == -1.0F) {
            return 0.0F;
        } else {
            float baseSpeed = serverPlayer.getDestroySpeed(blockState);
            boolean hasCorrectTool = hasCorrectToolCache.contains(material);
            if (!hasCorrectTool) {
                hasCorrectTool = serverPlayer.hasCorrectToolForDrops(blockState);
                if (hasCorrectTool) hasCorrectToolCache.add(material);
            }
            int factor = hasCorrectTool ? 30 : 100;
            float finalSpeed = baseSpeed / destroySpeed / (float)factor;
            destroySpeedCache.put(material, finalSpeed);
            return finalSpeed;
        }
    }

    public void initBlocks(Player p, Figure figure, Vector blockFaceDirection) {
        if (figure == null) return;

        int capacity = this.getCapacity(figure.getFigureType(), figure.getWidth(), figure.getHeight(), figure.getDepth());
        this.multiBlocks = new ArrayList<>(capacity);
        Set<Block> blocks = figure.getBlocks(p, this.getBlock(), blockFaceDirection);
        for (Block block : blocks) {
            MultiBlock multiBlock = new MultiBlock(block);
            this.getMultiBlocks().add(multiBlock);
        }
    }

    private int getCapacity(FigureType figureType, int width, int height, int depth) {
        switch (figureType) {
            case LINEAR -> {
                return width * height * depth;
            }
            case CIRCULAR -> {
                return (width * height * depth * 11) / 21;
            }
            case TRIANGULAR -> {
                return (width * height * depth) / 2;
            }
            default -> {
                return 0;
            }
        }
    }

    public void tick() {
        Player p = this.getPlayer();
        if (p == null) {
            this.end(p, false);
            return;
        }

        this.progressTicks++;
        if ((this.progressTicks & 31) == 0) this.checkPlayers(); // every 32 ticks
        this.checkDestroySpeedChange(p);
        //this.checkRemove();

        List<MultiBlock> multiBlockSnapshot = new ArrayList<>(this.multiBlocks);
        if ((this.progressTicks & 1) == 0) this.playParticles(multiBlockSnapshot); // every 2 ticks
        updateBlockAnimationPacket(p, multiBlockSnapshot);
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
        this.ended = true;
        List<MultiBlock> multiBlockSnapshot = new ArrayList<>(this.multiBlocks);
        this.writeStage(-1, multiBlockSnapshot);
        if (!finished) return;
        World world = this.getBlock().getWorld();
        int size = multiBlockSnapshot.size() - 1;
        float volume = (float) (1 / Math.log(((size) + 1) * Math.E));

        List<Block> blocksToBreak = new ArrayList<>();
        List<BlockPos> blockPosToBreak = new ArrayList<>();
        for (MultiBlock multiBlock : multiBlockSnapshot) {
            Block block = multiBlock.getBlock();
            Material blockType = block.getType();

            if (Config.getInstance().getIgnoredMaterials().contains(blockType)) continue;
            block.setMetadata("multi-broken", new FixedMetadataValue(Main.getInstance(), true));
            if (multiBlock.isVisible()) {
                block.setMetadata("isVisible", new FixedMetadataValue(Main.getInstance(), true));
            }
            blocksToBreak.add(block);
            blockPosToBreak.add(CraftLocation.toBlockPosition(block.getLocation()));
        }

        Map<Material, BlockData> blockDataCache = new EnumMap<>(Material.class);
        for (Block block : blocksToBreak) {
            BlockData blockData = blockDataCache.computeIfAbsent(block.getType(), Material::createBlockData);
            Location location = block.getLocation();
            boolean broken = p.breakBlock(block);
            if (!broken) continue;
            world.playSound(location, blockData.getSoundGroup().getBreakSound(), volume, 1F);
            if (block.hasMetadata("isVisible")) {
                block.removeMetadata("isVisible", Main.getInstance());
                breakParticleBuilder
                        .location(location.add(0.5, 0.5, 0.5))
                        .data(blockData)
                        .spawn();
            }
        }
    }

    public void updateBlockAnimationPacket(Player p, List<MultiBlock> multiBlockSnapshot) {
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
            int difference = stage - this.lastStage;
            if (Config.getInstance().isAsyncEnabled()) {
                for (int i = 0; i < difference; i++) {
                    this.writeStage(stage, multiBlockSnapshot);
                }
            } else {
                this.writeStage(stage, multiBlockSnapshot);
            }
            this.lastStage = stage;
        }
    }

    public void writeStage(int stage, List<MultiBlock> multiBlockSnapshot) {
        WriteStageRunnable writeStageRunnable = new WriteStageRunnable(multiBlockSnapshot, this.getBlock(), stage, this.nearbyPlayerConnections, this.packetLock, this);
        if (Config.getInstance().isAsyncEnabled()) {
            Main.getHighPriorityExecutor().submit(writeStageRunnable);
        } else {
            writeStageRunnable.run();
        }
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

    // todo: do this just when the block got rid of, linked to player
    /*public void checkRemove() {
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
    }*/

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

    public void checkValid(Player p, EnumSet<Material> includedMaterials, EnumSet<Material> excludedMaterials) {
        if (includedMaterials != null && !includedMaterials.isEmpty()) {
            this.getMultiBlocks().removeIf(multiBlock -> !includedMaterials.contains(multiBlock.getBlock().getType()));
        }
        if (excludedMaterials != null && !excludedMaterials.isEmpty()) {
            this.getMultiBlocks().removeIf(multiBlock -> excludedMaterials.contains(multiBlock.getBlock().getType()));
        }
        this.getMultiBlocks().removeIf(multiBlock -> multiBlock.getBlock().getType().equals(Material.AIR));
        boolean fairMode = Config.getInstance().isFairModeEnabled();
        if (!fairMode) return;

        ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
        for (MultiBlock multiBlock : this.getMultiBlocks()) {
            BlockPos blockPos = CraftLocation.toBlockPosition(multiBlock.getBlock().getLocation());
            float blockProgressPerTick = this.getDestroySpeed(serverPlayer, blockPos);
            if (blockProgressPerTick == Float.POSITIVE_INFINITY) {
                multiBlock.setVisible(false);
            }
        }
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

    private static final Predicate<net.minecraft.world.entity.Entity> isPlayer =
            (nmsEntity) -> nmsEntity.getType() == net.minecraft.world.entity.EntityType.PLAYER;

    // 2-6x faster than API version
    public Set<UUID> getNearbyPlayerUUIDs(Location blockLoc) {
        Set<UUID> uuids = new HashSet<>();
        CraftWorld craftWorld = (CraftWorld) blockLoc.getWorld();

        double x = blockLoc.getX();
        double y = blockLoc.getY();
        double z = blockLoc.getZ();
        double radius = 32.0;
        AABB aabb = new AABB(
                x - radius, y - radius, z - radius,
                x + radius, y + radius, z + radius
        );

        List<net.minecraft.world.entity.Entity> entityList = craftWorld.getHandle().getEntities((net.minecraft.world.entity.Entity) null, aabb, isPlayer);

        for (net.minecraft.world.entity.Entity entity : entityList) {
            Player p = (Player) entity.getBukkitEntity();
            uuids.add(p.getUniqueId());
        }
        return uuids;
    }

    public void updateNearbyPlayerConnections() {
        List<ServerGamePacketListenerImpl> connections = new ArrayList<>(this.nearbyPlayers.size());
        for (UUID uuid : this.nearbyPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                ServerGamePacketListenerImpl connection = ((CraftPlayer)p).getHandle().connection;
                connections.add(connection);
            }
        }
        this.nearbyPlayerConnections = connections;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(this.playerUUID);
    }

    public List<Block> getBlocks() {
        List<MultiBlock> multiBlocks = this.getMultiBlocks();
        List<Block> blocks = new ArrayList<>(multiBlocks.size());
        for (MultiBlock multiBlock : multiBlocks) {
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

    public void invalidateDestroySpeedCache() {
        this.destroySpeedCache.clear();
    }

    public void invalidateHasCorrectToolCache() {
        this.hasCorrectToolCache.clear();
    }
}
