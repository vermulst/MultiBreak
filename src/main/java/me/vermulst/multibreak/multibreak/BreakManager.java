package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
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
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BreakManager implements Listener {

    private final Main plugin;

    private final Map<UUID, Integer> multiBreakTask = new HashMap<>();
    private final Map<UUID, MultiBreak> multiBlockMap = new HashMap<>();

    public BreakManager(Main plugin) {
        this.plugin = plugin;
    }

    /** Legacy entry point */


    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void armSwingEvent(PlayerAnimationEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[0];
        if (!legacy_mode) return;
        if (!e.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;
        Player p = e.getPlayer();
        if (p.getGameMode().equals(GameMode.CREATIVE)) return;
        MultiBreak multiBreak = getOrCreateMultiBreak(p);
        if (multiBreak == null) return;
        Block blockMining = this.getTargetBlock(p);
        if (blockMining == null) return;
        multiBreak.tick(this.getPlugin(), blockMining);
    }

    /** Modern entry point */

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[0];
        if (legacy_mode) return;
        this.scheduleMultiBreak(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStop(BlockDamageAbortEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[0];
        if (legacy_mode) return;
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getOrCreateMultiBreak(p);
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, false);
        event.callEvent();
        if (multiBreak == null) return;
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
        int taskID = new BukkitRunnable() {
            @Override
            public void run() {
                MultiBreak multiBreak = getOrCreateMultiBreak(p);
                if (multiBreak == null) return;

                // check if direction changed
                BlockFace blockFace = getBlockFace(p);
                if (blockFace == null) {
                    cancel();
                    return;
                }
                Vector direction = blockFace.getDirection();
                if (!multiBreak.getPlayerDirection().equals(direction)) {
                    multiBreak = replaceMultiBreak(p, multiBreak);
                    scheduleMultiBreak(p);
                }
                multiBreak.tick();
            }
        }.runTaskTimer(getPlugin(), 1, 1).getTaskId();
        getMultiBreakTask().put(p.getUniqueId(), taskID);
    }

    /** Replaces multibreak while preserving progress
     *
     * @param p - player breaking
     * @param multiBreak - multibreak to replaced
     * @return replaced multibreak
     */
    public MultiBreak replaceMultiBreak(Player p, MultiBreak multiBreak) {
        endMultiBreak(p, multiBreak, false);
        float progressBroken = multiBreak.getProgressBroken();
        int progressTicks = multiBreak.getProgressTicks();
        multiBreak = getOrCreateMultiBreak(p);
        multiBreak.setProgressBroken(progressBroken);
        multiBreak.setProgressTicks(progressTicks);
        return multiBreak;
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
        multiBreak = event.getMultiBreak();
        float breakSpeed = multiBreak.getBlock().getBreakSpeed(p);
        multiBreak.checkValid(breakSpeed, event.getIncludedMaterials(), event.getExcludedMaterials());
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
