package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.api.event.MultiBreakAllowEvent;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.api.event.MultiBreakEndEvent;
import me.vermulst.multibreak.api.event.MultiBreakStartEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BreakManager implements Listener {

    private final Main plugin;

    private final Map<UUID, Integer> multiBreakTask = new HashMap<>();
    private final Map<UUID, MultiBreak> multiBlockMap = new HashMap<>();

    public BreakManager(Main plugin) {
        this.plugin = plugin;
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        Player p = e.getPlayer();
        boolean allowed = this.hasFigure(e.getItemInHand());
        MultiBreakAllowEvent multiBreakAllowEvent = new MultiBreakAllowEvent(allowed, p, e.getItemInHand(), e.getBlock());
        multiBreakAllowEvent.callEvent();
        if (!multiBreakAllowEvent.isAllowed()) return;
        this.scheduleMultiBreak(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStop(BlockDamageAbortEvent e) {
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getOrCreateMultiBreak(p);
        if (multiBreak == null) return;
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, false);
        event.callEvent();
        this.endMultiBreak(p, multiBreak, false);
    }

    /** Block broken */

    @EventHandler(priority = EventPriority.MONITOR)
    public void breakBlockType(BlockBreakEvent e) {
        if (this.ignoreMultiBreak(e)) return;
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getOrCreateMultiBreak(p);
        if (multiBreak == null) return;
        Block block = e.getBlock();

        // Mismatch (player switched to an instamine-block while breaking)
        if (!block.equals(multiBreak.getBlock())) {
            multiBreak = this.initMultiBreak(p, block);
        }
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, true);
        event.callEvent();
        if (event.isCancelled()) return;
        if (event.getMultiBreak() == null) return;
        this.endMultiBreak(p, event.getMultiBreak(), true);
    }

    private boolean ignoreMultiBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        boolean b1 = block.hasMetadata("multi-broken");
        boolean b2 = plugin.getConfigManager().getIgnoredMaterials().contains(block.getType());
        boolean b3 = e.isCancelled();
        if (block.hasMetadata("multi-broken")) {
            block.removeMetadata("multi-broken", getPlugin());
        }
        return b1 || b2 || b3;
    }


    /** Schedules multibreak ticking task
     *
     * @param p - player breaking
     */
    public void scheduleMultiBreak(Player p) {
        Block block = this.getTargetBlock(p);
        MultiBreakRunnable multiBreakRunnable = new MultiBreakRunnable(block, p, this);
        int taskID = multiBreakRunnable.runTaskTimer(getPlugin(), 1, 1).getTaskId();
        getMultiBreakTask().put(p.getUniqueId(), taskID);
    }


    public void endMultiBreak(Player p, MultiBreak multiBreak, boolean finished) {
        UUID uuid = p.getUniqueId();
        multiBreak.end(finished, getPlugin());
        this.getMultiBlockMap().remove(uuid);
        if (!this.getMultiBreakTask().containsKey(uuid)) return;
        Bukkit.getScheduler().cancelTask(this.getMultiBreakTask().get(uuid));
    }

    public MultiBreak getOrCreateMultiBreak(Player p) {
        if (p.getGameMode().equals(GameMode.CREATIVE)) return null;
        if (multiBlockMap.containsKey(p.getUniqueId())) {
            MultiBreak multiBreak = multiBlockMap.get(p.getUniqueId());
            if (!multiBreak.hasEnded()) {
                return multiBreak;
            }
        }
        Block blockMining = this.getTargetBlock(p);
        return this.initMultiBreak(p, blockMining);
    }

    public MultiBreak initMultiBreak(Player p, Block block) {
        ItemStack tool = p.getInventory().getItemInMainHand();
        Figure figure = this.getFigure(tool);
        BlockFace blockFace = this.getBlockFace(p);
        if (block == null || blockFace == null) return null;
        ConfigManager config = plugin.getConfigManager();
        EnumSet<Material> includedMaterials = config.getIncludedMaterials();
        EnumSet<Material> ignoredMaterials = config.getIgnoredMaterials();
        MultiBreak multiBreak = new MultiBreak(p, block, figure, blockFace.getDirection());
        MultiBreakStartEvent event = new MultiBreakStartEvent(p, multiBreak, block, blockFace.getDirection(), includedMaterials, ignoredMaterials);
        if (!event.callEvent()) return null;
        includedMaterials = event.getIncludedMaterials();
        ignoredMaterials = event.getExcludedMaterials();
        multiBreak = event.getMultiBreak();
        if (!multiBreak.isValid(includedMaterials, ignoredMaterials)) return null;
        float breakSpeed = multiBreak.getBlock().getBreakSpeed(p);
        multiBreak.checkValid(breakSpeed, includedMaterials, ignoredMaterials);
        multiBlockMap.put(p.getUniqueId(), multiBreak);
        return multiBreak;
    }

    /** Retrieves the Figure associated with the given tool ItemStack.
     * Also checks for material-based presets, with itemstack data as priority.
     *
     * @param tool The ItemStack representing the tool.
     * @return The Figure associated with the tool, or null if none is found.
     */
    public Figure getFigure(ItemStack tool) {
        if (tool.getItemMeta() == null) return null;
        FigureItemDataType figureItemDataType = new FigureItemDataType(this.getPlugin());
        Figure figure = figureItemDataType.get(tool);
        if (figure != null) return figure;

        // Fallback on material figures
        Material material = tool.getType();
        ConfigManager configManager = this.getPlugin().getConfigManager();
        if (configManager.getMaterialOptions().containsKey(material)) {
            String configOptionName = configManager.getMaterialOptions().get(material);
            return configManager.getConfigOptions().get(configOptionName);
        }
        return null;
    }

    public boolean hasFigure(ItemStack tool) {
        if (tool.getItemMeta() == null) return false;
        FigureItemDataType figureItemDataType = new FigureItemDataType(this.getPlugin());
        boolean toolHasMultibreak = figureItemDataType.has(tool);
        Material material = tool.getType();
        ConfigManager configManager = this.getPlugin().getConfigManager();
        boolean materialHasMultibreak = configManager.getMaterialOptions().containsKey(material);
        return toolHasMultibreak || materialHasMultibreak;
    }

    public Block getTargetBlock(Player p) {
        return p.getTargetBlockExact(plugin.getConfigManager().getMaxRange());
    }

    public BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(plugin.getConfigManager().getMaxRange());
    }

    public Main getPlugin() {
        return plugin;
    }

    public Map<UUID, Integer> getMultiBreakTask() {
        return multiBreakTask;
    }

    public Map<UUID, MultiBreak> getMultiBlockMap() {
        return multiBlockMap;
    }
}
