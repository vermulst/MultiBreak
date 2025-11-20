package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.api.event.FetchFigureEvent;
import me.vermulst.multibreak.api.event.FilterBlocksEvent;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.api.event.MultiBreakStartEvent;
import me.vermulst.multibreak.multibreak.runnables.MultiBreakRunnable;
import me.vermulst.multibreak.utils.BlockFilter;
import me.vermulst.multibreak.utils.BreakUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BreakManager {
    private final Map<UUID, Integer> movedPlayers = new HashMap<>();
    private final Map<UUID, Integer> multiBreakTask = new HashMap<>();
    private final Map<UUID, Figure> figureCache = new HashMap<>();
    private final Map<UUID, Block> lastTargetBlock = new HashMap<>();
    private final Map<UUID, MultiBreak> multiBreakMap = new HashMap<>();

    private final Map<Location, Set<MultiBreak>> multiBreakLocationMap = new HashMap<>();


    private static final BreakManager breakManager = new BreakManager();

    public static BreakManager getInstance() {
        return breakManager;
    }

    public void refreshTool(Player p) {
        boolean fairMode = Config.getInstance().isFairModeEnabled();
        if (!fairMode) return;
        UUID uuid = p.getUniqueId();
        Figure previousFigure = figureCache.remove(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                Figure newFigure = getFigure(p);
                if (newFigure != null && newFigure.equals(previousFigure)) return;
                lastTargetBlock.remove(uuid);
                refreshBreakSpeed(p, newFigure);
                BreakUtils.interactionRangeCache.remove(uuid);
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    public void refreshBreakSpeed(Player p) {
        Figure figure = this.getFigure(p);
        this.refreshBreakSpeed(p, figure);
    }

    public void refreshBreakSpeed(Player p, Figure figure) {
        UUID uuid = p.getUniqueId();
        AttributeInstance attribute = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        AttributeModifier modifierToRemove = attribute.getModifier(new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"));

        if (figure == null) {
            this.removeModifier(attribute, modifierToRemove);
            this.invalidateDestroySpeedCache(uuid);
            return;
        }

        RayTraceResult rayTraceResult = BreakUtils.getRayTraceResult(p);
        if (rayTraceResult == null) {
            this.removeModifier(attribute, modifierToRemove);
            this.invalidateDestroySpeedCache(uuid);
            return;
        }
        Block targetBlock = rayTraceResult.getHitBlock();
        if (targetBlock == null) {
            this.removeModifier(attribute, modifierToRemove);
            this.invalidateDestroySpeedCache(uuid);
            return;
        }
        if (lastTargetBlock.containsKey(uuid) && targetBlock.equals(lastTargetBlock.get(uuid))) return;
        lastTargetBlock.put(uuid, targetBlock);

        this.removeModifier(attribute, modifierToRemove);
        this.invalidateDestroySpeedCache(uuid);

        Config config = Config.getInstance();
        EnumSet<Material> includedMaterials = config.getIncludedMaterials();
        EnumSet<Material> ignoredMaterials = config.getIgnoredMaterials();
        FilterBlocksEvent filterBlocksEvent = new FilterBlocksEvent(figure, p, p.getInventory().getItemInMainHand(), includedMaterials, ignoredMaterials);
        filterBlocksEvent.callEvent();

        Set<Block> blocks = figure.getBlocks(p, targetBlock, rayTraceResult.getHitBlockFace().getDirection());
        this.filter(blocks, filterBlocksEvent.getIncludedMaterials(), filterBlocksEvent.getExcludedMaterials());

        MultiBreak multiBreak = this.getMultiBreak(p);
        float baseProgressPerTick = BreakUtils.getDestroySpeed(p, multiBreak);
        if (baseProgressPerTick == -1f) baseProgressPerTick = targetBlock.getBreakSpeed(p);
        if (baseProgressPerTick == Float.POSITIVE_INFINITY) return;
        MultiBreak multiBreakOffState = this.getMultiBreakOffstate(p);
        float slowDownFactor = this.getSlowDownFactor(p, blocks, baseProgressPerTick, multiBreakOffState);
        this.invalidateDestroySpeedCache(uuid);
        if (slowDownFactor == 1.0f) return;
        double currentAttributeTotal = p.getAttribute(Attribute.BLOCK_BREAK_SPEED).getValue();
        double newAttributeTotal = currentAttributeTotal * slowDownFactor;
        double modifier = -(currentAttributeTotal - newAttributeTotal);
        this.setModifier(attribute, modifier);
    }

    private void removeModifier(AttributeInstance attribute, AttributeModifier modifier) {
        if (modifier != null) {
            attribute.removeModifier(modifier);
        }
    }

    private void invalidateDestroySpeedCache(UUID uuid) {
        if (multiBreakMap.containsKey(uuid)) {
            MultiBreak multiBreak = multiBreakMap.get(uuid);
            multiBreak.invalidateDestroySpeedCache();
        }
    }

    private void setModifier(AttributeInstance attribute, double newValue) {
        AttributeModifier newModifier = new AttributeModifier(
                new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"),
                newValue,
                AttributeModifier.Operation.ADD_NUMBER
        );
        attribute.addModifier(newModifier);
    }

    /** Schedules multibreak ticking task
     *
     * @param p - player breaking
     */
    public void scheduleMultiBreak(Player p, @NotNull Figure figure, Block block, boolean isStaticBreak) {
        if (multiBreakTask.containsKey(p.getUniqueId())) {
            endMultiBreak(p, this.getMultiBreak(p), false);
        }
        MultiBreak multiBreakOffState = this.getMultiBreakOffstate(p);
        if (multiBreakOffState != null && !isStaticBreak) multiBreakOffState.setLastTick(-1);
        MultiBreakRunnable multiBreakRunnable = new MultiBreakRunnable(p, block, figure, this, isStaticBreak);
        int taskID = multiBreakRunnable.runTaskTimer(Main.getInstance(), 1, 1).getTaskId();
        multiBreakTask.put(p.getUniqueId(), taskID);
    }

    public void endMultiBreak(Player p, MultiBreak multiBreak, boolean finished) {
        UUID uuid = p.getUniqueId();
        if (multiBreak != null) {
            multiBreak.end(p, finished);

            // if it did finish, multiblock is already removed by break event
            if (!finished) {
                for (MultiBlock multiBlock : multiBreak.getMultiBlocks()) {
                    if (multiBlock == null) continue;
                    Location location = multiBlock.getLocation();
                    if (!multiBreakLocationMap.containsKey(location)) continue;
                    Set<MultiBreak> multiBreaks = multiBreakLocationMap.get(location);
                    multiBreaks.remove(multiBreak);
                    if (multiBreaks.isEmpty()) {
                        multiBreakLocationMap.remove(location);
                    }
                }
            }
        }
        if (!multiBreakTask.containsKey(uuid)) return;
        Bukkit.getScheduler().cancelTask(multiBreakTask.get(uuid));
        multiBreakTask.remove(uuid);
        breakManager.getMovedPlayers().remove(uuid);
    }

    public MultiBreak initMultiBreak(Player p, Block block, @NotNull Figure figure) {
        if (block == null) return null;
        BlockFace blockFace = BreakUtils.getBlockFace(p);
        return this.initMultiBreak(p, block, figure, blockFace);
    }

    public MultiBreak initMultiBreak(Player p, Block block, @NotNull Figure figure, BlockFace blockFace) {
        if (blockFace == null) return null;
        Config config = Config.getInstance();
        EnumSet<Material> includedMaterials = config.getIncludedMaterials();
        EnumSet<Material> ignoredMaterials = config.getIgnoredMaterials();

        FilterBlocksEvent filterBlocksEvent = new FilterBlocksEvent(figure, p, p.getInventory().getItemInMainHand(), includedMaterials, ignoredMaterials);
        filterBlocksEvent.callEvent();
        includedMaterials = filterBlocksEvent.getIncludedMaterials();
        ignoredMaterials = filterBlocksEvent.getExcludedMaterials();

        MultiBreak multiBreak = multiBreakMap.get(p.getUniqueId());
        if (multiBreak != null) {
            multiBreak.reset(p, block, blockFace.getDirection(), figure, includedMaterials, ignoredMaterials);
        } else {
            multiBreak = new MultiBreak(p, block, blockFace.getDirection(), figure, includedMaterials, ignoredMaterials);
            multiBreakMap.put(p.getUniqueId(), multiBreak);
        }
        MultiBreakStartEvent event = new MultiBreakStartEvent(p, multiBreak, block);
        if (!event.callEvent()) return null;
        multiBreak = event.getMultiBreak();

        if (!multiBreak.isValid(includedMaterials, ignoredMaterials)) return null;

        MultiBlock[] multiBlocks =  multiBreak.getMultiBlocks();
        for (MultiBlock mb : multiBlocks) {
            if (mb == null) continue;
            Location location = mb.getLocation();
            multiBreakLocationMap
                    .computeIfAbsent(location, k -> new HashSet<>(multiBlocks.length))
                    .add(multiBreak);
        }

        return multiBreak;
    }

    public boolean wasMultiBroken(Block block) {
        return block.hasMetadata("multi-broken");
    }

    public void removeMultiBrokenMetadata(Block block) {
        block.removeMetadata("multi-broken", Main.getInstance());
    }

    public boolean isMultiBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        boolean wasMultiBroken = block.hasMetadata("multi-broken");
        boolean isIgnoredMaterial = Config.getInstance().getIgnoredMaterials().contains(block.getType());
        boolean isCancelled = e.isCancelled();
        return !wasMultiBroken && !isIgnoredMaterial && !isCancelled;
    }

    public void filter(Set<Block> blocks, EnumSet<Material> includedMaterials, EnumSet<Material> ignoredMaterials) {
        blocks.removeIf(block -> {
            Material mainBlockType = block.getType();
            return BlockFilter.isExcluded(mainBlockType, includedMaterials, ignoredMaterials);
        });
    }

    public float getSlowDownFactor(Player p, Set<Block> blocks, float baseProgressPerTick, MultiBreak multiBreak) {
        float lowestProgressPerTick = baseProgressPerTick;
        if (multiBreak != null) {
            ServerPlayer serverPlayer = ((CraftPlayer)p).getHandle();
            for (Block block : blocks) {
                BlockPos blockPos = CraftLocation.toBlockPosition(block.getLocation());
                float progressPerTick = BreakUtils.getDestroySpeed(serverPlayer, blockPos, multiBreak);
                if (progressPerTick < lowestProgressPerTick) {
                    lowestProgressPerTick = progressPerTick;
                }
            }
        } else {
            for (Block block : blocks) {
                float progressPerTick = block.getBreakSpeed(p);
                if (progressPerTick < lowestProgressPerTick) {
                    lowestProgressPerTick = progressPerTick;
                }
            }
        }
        return lowestProgressPerTick / baseProgressPerTick;
    }

    public Figure getFigure(Player p) {
        ItemStack tool = p.getInventory().getItemInMainHand();
        return this.getFigure(p, tool);
    }


    /** Retrieves the Figure associated with the given tool ItemStack.
     * Also checks for material-based presets, with itemstack data as priority.
     *
     * @param tool The ItemStack representing the tool.
     * @return The Figure associated with the tool, or null if none is found.
     */
    public Figure getFigure(Player p, ItemStack tool) {
        if (figureCache.containsKey(p.getUniqueId())) {
            return figureCache.get(p.getUniqueId());
        }
        if (tool.getItemMeta() == null) {
            figureCache.put(p.getUniqueId(), null);
            return null;
        }
        FigureItemDataType figureItemDataType = new FigureItemDataType();
        Figure figure = figureItemDataType.get(tool);
        if (figure == null) {
            Material material = tool.getType();
            Config config = Config.getInstance();
            if (config.getMaterialOptions().containsKey(material)) {
                String configOptionName = config.getMaterialOptions().get(material);
                figure = config.getConfigOptions().get(configOptionName);
            }
        }
        FetchFigureEvent fetchFigureEvent = new FetchFigureEvent(figure, p, tool);
        fetchFigureEvent.callEvent();
        if (fetchFigureEvent.isCancelled()) {
            figureCache.put(p.getUniqueId(), null);
            return null;
        }
        figure = fetchFigureEvent.getFigure();
        figureCache.put(p.getUniqueId(), figure);
        return figure;
    }

    public MultiBreak getMultiBreak(Player p) {
        if (multiBreakMap.containsKey(p.getUniqueId())) {
            MultiBreak multiBreak = multiBreakMap.get(p.getUniqueId());
            if (!multiBreak.hasEnded()) {
                return multiBreak;
            }
        }
        return null;
    }

    public MultiBreak getMultiBreakOffstate(Player p) {
        return multiBreakMap.get(p.getUniqueId());
    }

    public void onPlayerQuit(Player p) {
        endMultiBreak(p, getMultiBreak(p), false);
        UUID uuid = p.getUniqueId();
        figureCache.remove(uuid);
        lastTargetBlock.remove(uuid);
        multiBreakMap.remove(uuid);
        movedPlayers.remove(uuid);
        BreakUtils.interactionRangeCache.remove(uuid);
    }

    public void handleBlockRemoval(Location location) {
        if (!multiBreakLocationMap.containsKey(location)) return;
        Set<MultiBreak> multiBreaks = multiBreakLocationMap.get(location);
        for (MultiBreak multiBreak : multiBreaks) {
            MultiBlock[] multiBlocks = multiBreak.getMultiBlocks();
            for (int i = 0; i < multiBlocks.length; i++) {
                MultiBlock multiBlock = multiBreak.getMultiBlocks()[i];
                if (multiBlock != null && multiBlock.getLocation().equals(location)) {
                    multiBreak.writeStage(-1, new MultiBlock[]{multiBlock});
                    multiBlocks[i] = null;
                    break;
                }
            }
        }
    }

    public void handleBlockRemovals(Set<Location> locations) {
        Set<MultiBreak> breaksToUpdate = new HashSet<>();
        for (Location location : locations) {
            Set<MultiBreak> linkedBreaks = multiBreakLocationMap.get(location);
            if (linkedBreaks != null) {
                breaksToUpdate.addAll(linkedBreaks);
            }
        }

        for (MultiBreak multiBreak : breaksToUpdate) {
            MultiBlock[] multiBlocks = multiBreak.getMultiBlocks();
            int removeCount = 0;

            for (MultiBlock multiBlock : multiBlocks) {
                if (multiBlock != null && locations.contains(multiBlock.getLocation())) {
                    removeCount++;
                }
            }

            if (removeCount > 0) {
                MultiBlock[] toRemove = new MultiBlock[removeCount];
                int idx = 0;
                for (int i = 0; i < multiBlocks.length; i++) {
                    if (multiBlocks[i] != null && locations.contains(multiBlocks[i].getLocation())) {
                        toRemove[idx++] = multiBlocks[i];
                        multiBlocks[i] = null;
                    }
                }
                multiBreak.writeStage(-1, toRemove);
            }
        }
    }

    public boolean isBreaking(UUID uuid) {
        return multiBreakTask.containsKey(uuid);
    }

    public Set<UUID> getBreakingPlayers() {
        return multiBreakTask.keySet();
    }

    public Map<UUID, Integer> getMovedPlayers() {
        return movedPlayers;
    }

    public Map<UUID, MultiBreak> getMultiBreakMap() {
        return multiBreakMap;
    }
}
