package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.api.event.FetchFigureEvent;
import me.vermulst.multibreak.api.event.FilterBlocksEvent;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.api.event.MultiBreakStartEvent;
import me.vermulst.multibreak.utils.BreakUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BreakManager {
    private final Map<UUID, Integer> multiBreakTask = new HashMap<>();
    private final Map<UUID, MultiBreak> multiBlockMap = new HashMap<>();
    private final Map<UUID, Figure> figureCache = new HashMap<>();
    private final Map<UUID, Block> lastTargetBlock = new HashMap<>();

    private static final BreakManager breakManager = new BreakManager();

    public static BreakManager getInstance() {
        return breakManager;
    }

    public void refreshTool(Player p) {
        boolean fairMode = Config.getInstance().isFairModeEnabled();
        if (!fairMode) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID uuid = p.getUniqueId();
                Figure previousFigure = figureCache.remove(uuid);
                Figure newFigure = getFigure(p);
                if (newFigure != null && newFigure.equals(previousFigure)) return;
                lastTargetBlock.remove(uuid);
                refreshBreakSpeed(p);
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    protected void refreshBreakSpeed(Player p) {
        UUID uuid = p.getUniqueId();
        Block targetBlock = BreakUtils.getTargetBlock(p);
        if (targetBlock == null) {
            lastTargetBlock.remove(uuid);
            return;
        }
        if (lastTargetBlock.containsKey(uuid) && targetBlock.equals(lastTargetBlock.get(uuid))) return;
        lastTargetBlock.put(uuid, targetBlock);

        AttributeInstance attribute = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        AttributeModifier modifierToRemove = attribute.getModifier(new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"));
        if (modifierToRemove != null) {
            attribute.removeModifier(modifierToRemove);
        }
        Figure figure = this.getFigure(p);
        if (figure == null) return;

        Config config = Config.getInstance();
        EnumSet<Material> includedMaterials = config.getIncludedMaterials();
        EnumSet<Material> ignoredMaterials = config.getIgnoredMaterials();
        FilterBlocksEvent filterBlocksEvent = new FilterBlocksEvent(figure, p, p.getInventory().getItemInMainHand(), includedMaterials, ignoredMaterials);
        filterBlocksEvent.callEvent();

        Set<Block> blocks = figure.getBlocks(p, targetBlock);
        this.filter(blocks, filterBlocksEvent.getIncludedMaterials(), filterBlocksEvent.getExcludedMaterials());

        float baseProgressPerTick = targetBlock.getBreakSpeed(p);
        if (baseProgressPerTick == Float.POSITIVE_INFINITY) return;
        float slowDownFactor = this.getSlowDownFactor(p, blocks, baseProgressPerTick);
        if (slowDownFactor == 1.0f) {
            return;
        }
        double currentAttributeTotal = p.getAttribute(Attribute.BLOCK_BREAK_SPEED).getValue();
        double newAttributeTotal = currentAttributeTotal * slowDownFactor;
        AttributeModifier slowDownModifier = new AttributeModifier(
                new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"),
                -(currentAttributeTotal - newAttributeTotal),
                AttributeModifier.Operation.ADD_NUMBER
        );
        attribute.addModifier(slowDownModifier);
    }

    /** Schedules multibreak ticking task
     *
     * @param p - player breaking
     */
    protected void scheduleMultiBreak(Player p, Figure figure, Block block) {
        if (multiBreakTask.containsKey(p.getUniqueId())) {
            endMultiBreak(p, this.getMultiBreak(p), false);
        }
        MultiBreakRunnable multiBreakRunnable = new MultiBreakRunnable(p, block, figure, this);
        int taskID = multiBreakRunnable.runTaskTimer(Main.getInstance(), 1, 1).getTaskId();
        multiBreakTask.put(p.getUniqueId(), taskID);
    }

    protected void endMultiBreak(Player p, MultiBreak multiBreak, boolean finished) {
        UUID uuid = p.getUniqueId();
        if (multiBreak != null) {
            multiBreak.end(finished);
            multiBlockMap.remove(uuid);
        }
        if (!multiBreakTask.containsKey(uuid)) return;
        Bukkit.getScheduler().cancelTask(multiBreakTask.get(uuid));
        multiBreakTask.remove(uuid);
    }

    protected MultiBreak initMultiBreak(Player p, Block block, Figure figure) {
        if (block == null) return null;
        BlockFace blockFace = BreakUtils.getBlockFace(p);
        if (blockFace == null) return null;
        Config config = Config.getInstance();
        EnumSet<Material> includedMaterials = config.getIncludedMaterials();
        EnumSet<Material> ignoredMaterials = config.getIgnoredMaterials();
        MultiBreak multiBreak = new MultiBreak(p, block, blockFace.getDirection(), figure);
        MultiBreakStartEvent event = new MultiBreakStartEvent(p, multiBreak, block);
        if (!event.callEvent()) return null;
        FilterBlocksEvent filterBlocksEvent = new FilterBlocksEvent(figure, p, p.getInventory().getItemInMainHand(), includedMaterials, ignoredMaterials);
        filterBlocksEvent.callEvent();
        includedMaterials = filterBlocksEvent.getIncludedMaterials();
        ignoredMaterials = filterBlocksEvent.getExcludedMaterials();
        multiBreak = event.getMultiBreak();
        if (!multiBreak.isValid(includedMaterials, ignoredMaterials)) return null;
        float progressPerTick = multiBreak.getBlock().getBreakSpeed(p);
        multiBreak.checkValid(progressPerTick, includedMaterials, ignoredMaterials);
        multiBlockMap.put(p.getUniqueId(), multiBreak);
        return multiBreak;
    }

    protected boolean isMultiBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        boolean wasMultiBroken = block.hasMetadata("multi-broken");
        boolean isIgnoredMaterial = Config.getInstance().getIgnoredMaterials().contains(block.getType());
        boolean isCancelled = e.isCancelled();
        if (wasMultiBroken) {
            block.removeMetadata("multi-broken", Main.getInstance());
        }
        return !wasMultiBroken && !isIgnoredMaterial && !isCancelled;
    }

    protected void filter(Set<Block> blocks, EnumSet<Material> includedMaterials, EnumSet<Material> ignoredMaterials) {
        blocks.removeIf(block -> {
            Material mainBlockType = block.getType();
            if (includedMaterials != null && !includedMaterials.isEmpty() && !includedMaterials.contains(mainBlockType)) {
                return true;
            }
            if (ignoredMaterials != null && !ignoredMaterials.isEmpty() && ignoredMaterials.contains(mainBlockType)) {
                return true;
            }
            return false;
        });
    }

    protected float getSlowDownFactor(Player p, Set<Block> blocks, float baseProgressPerTick) {
        float lowestProgressPerTick = baseProgressPerTick;
        for (Block block : blocks) {
            float progressPerTick = block.getBreakSpeed(p);
            if (progressPerTick < lowestProgressPerTick) {
                lowestProgressPerTick = progressPerTick;
            }
        }
        return lowestProgressPerTick / baseProgressPerTick;
    }

    protected Figure getFigure(Player p) {
        ItemStack tool = p.getInventory().getItemInMainHand();
        return this.getFigure(p, tool);
    }


    /** Retrieves the Figure associated with the given tool ItemStack.
     * Also checks for material-based presets, with itemstack data as priority.
     *
     * @param tool The ItemStack representing the tool.
     * @return The Figure associated with the tool, or null if none is found.
     */
    protected Figure getFigure(Player p, ItemStack tool) {
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

    protected MultiBreak getMultiBreak(Player p) {
        if (multiBlockMap.containsKey(p.getUniqueId())) {
            MultiBreak multiBreak = multiBlockMap.get(p.getUniqueId());
            if (!multiBreak.hasEnded()) {
                return multiBreak;
            }
        }
        return null;
    }

    public Map<UUID, Integer> getMultiBreakTask() {
        return multiBreakTask;
    }
}
