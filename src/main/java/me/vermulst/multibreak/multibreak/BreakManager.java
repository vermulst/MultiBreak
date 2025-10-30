package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.api.event.RequestFigureEvent;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.api.event.MultiBreakEndEvent;
import me.vermulst.multibreak.api.event.MultiBreakStartEvent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BreakManager implements Listener {

    private final Main plugin;

    private final Map<UUID, Integer> multiBreakTask = new HashMap<>();
    private final Map<UUID, MultiBreak> multiBlockMap = new HashMap<>();

    public BreakManager(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Figure> figureCache = new HashMap<>();
    private final Map<UUID, Block> lastTargetBlock = new HashMap<>();

    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        this.refresh(e.getPlayer());
    }

    @EventHandler
    public void itemHeld(PlayerItemHeldEvent e) {
        this.refresh(e.getPlayer());
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        this.refresh((Player) e.getWhoClicked());
    }

    @EventHandler
    public void dragEvent(InventoryDragEvent e) {
        this.refresh((Player) e.getWhoClicked());
    }

    @EventHandler
    public void swapOffhand(PlayerSwapHandItemsEvent e) {
        this.refresh(e.getPlayer());
    }


    private void refresh(Player p) {
        boolean fairMode = Main.getInstance().getConfigManager().getOptions()[0];
        if (!fairMode) return;
        UUID uuid = p.getUniqueId();
        figureCache.remove(uuid);
        lastTargetBlock.remove(uuid);
        new BukkitRunnable() {
            @Override
            public void run() {
                setBreakSpeed(p);
            }
        }.runTaskLater(Main.getInstance(), 1L);
    }

    @EventHandler
    public void tickEvent(ServerTickEndEvent e) {
        boolean fairMode = Main.getInstance().getConfigManager().getOptions()[0];
        if (!fairMode) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.setBreakSpeed(p);
        }
    }

    public void setBreakSpeed(Player p) {
        UUID uuid = p.getUniqueId();
        Block targetBlock = this.getTargetBlock(p);
        if (targetBlock == null) {
            lastTargetBlock.remove(uuid);
            return;
        }
        if (!lastTargetBlock.containsKey(uuid)) {
            lastTargetBlock.put(uuid, targetBlock);
        } else {
            Block lastBlock = lastTargetBlock.get(uuid);
            if (!lastBlock.equals(targetBlock)) {
                lastTargetBlock.put(uuid, targetBlock);
            }
            AttributeInstance attribute = p.getAttribute(Attribute.BLOCK_BREAK_SPEED);
            AttributeModifier modifierToRemove = attribute.getModifier(new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"));
            if (modifierToRemove != null) {
                attribute.removeModifier(modifierToRemove);
            }
            Figure figure = this.getFigure(p);
            if (figure == null) return;
            Set<Block> blocks = figure.getBlocks(p, targetBlock);
            float baseProgressPerTick = targetBlock.getBreakSpeed(p);
            if (baseProgressPerTick == Float.POSITIVE_INFINITY) return;
            float slowDownFactor = this.getSlowDownFactor(p, blocks, baseProgressPerTick);
            if (slowDownFactor == 1.0f) {
                return;
            }
            double currentAttributeTotal = p.getAttribute(Attribute.BLOCK_BREAK_SPEED).getValue();
            double newAttributeTotal = currentAttributeTotal * slowDownFactor;
            double currentAttributePlayer = p.getAttribute(Attribute.BLOCK_BREAK_SPEED).getBaseValue();
            AttributeModifier slowDownModifier = new AttributeModifier(
                    new NamespacedKey(Main.getInstance(), "MultiBreakSlowdown"),
                    -(currentAttributePlayer - newAttributeTotal),
                    AttributeModifier.Operation.ADD_NUMBER
            );
            attribute.addModifier(slowDownModifier);
        }
    }

    public float getSlowDownFactor(Player p, Set<Block> blocks, float baseProgressPerTick) {
        float lowestProgressPerTick = baseProgressPerTick;
        for (Block block : blocks) {
            float progressPerTick = block.getBreakSpeed(p);
            if (progressPerTick < lowestProgressPerTick) {
                lowestProgressPerTick = progressPerTick;
            }
        }
        return lowestProgressPerTick / baseProgressPerTick;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        Figure figure = this.getFigure(p, item);
        if (figure == null) return;
        this.scheduleMultiBreak(e.getPlayer(), figure);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStop(BlockDamageAbortEvent e) {
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getMultiBreak(p);
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
        MultiBreak multiBreak = this.getMultiBreak(p);
        if (multiBreak == null) {
            Figure figure = this.getFigure(p);
            multiBreak = this.initMultiBreak(p, e.getBlock(), figure);
            p.sendMessage("created new multibreak");
            if (multiBreak == null) return;
        }
        Block block = e.getBlock();

        // Mismatch (player switched to an instamine-block while breaking)
        if (!block.equals(multiBreak.getBlock())) {
            Figure figure = this.getFigure(p);
            multiBreak = this.initMultiBreak(p, block, figure);
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
    public void scheduleMultiBreak(Player p, Figure figure) {
        Block block = this.getTargetBlock(p);
        MultiBreakRunnable multiBreakRunnable = new MultiBreakRunnable(p, block, figure, this);
        int taskID = multiBreakRunnable.runTaskTimer(getPlugin(), 1, 1).getTaskId();
        getMultiBreakTask().put(p.getUniqueId(), taskID);
    }


    public void endMultiBreak(Player p, MultiBreak multiBreak, boolean finished) {
        UUID uuid = p.getUniqueId();
        multiBreak.end(finished, getPlugin());
        this.getMultiBlockMap().remove(uuid);
        if (!this.getMultiBreakTask().containsKey(uuid)) return;
        Bukkit.getScheduler().cancelTask(this.getMultiBreakTask().get(uuid));
        this.getMultiBreakTask().remove(uuid);
    }

    public MultiBreak getMultiBreak(Player p) {
        if (p.getGameMode().equals(GameMode.CREATIVE)) return null;
        if (multiBlockMap.containsKey(p.getUniqueId())) {
            MultiBreak multiBreak = multiBlockMap.get(p.getUniqueId());
            if (!multiBreak.hasEnded()) {
                return multiBreak;
            }
        }
        return null;
    }

    public MultiBreak initMultiBreak(Player p, Block block, Figure figure) {
        if (block == null) return null;
        BlockFace blockFace = this.getBlockFace(p);
        if (blockFace == null) return null;
        ConfigManager config = plugin.getConfigManager();
        EnumSet<Material> includedMaterials = config.getIncludedMaterials();
        EnumSet<Material> ignoredMaterials = config.getIgnoredMaterials();
        MultiBreak multiBreak = new MultiBreak(p, block, blockFace.getDirection(), figure);
        MultiBreakStartEvent event = new MultiBreakStartEvent(p, multiBreak, block, includedMaterials, ignoredMaterials);
        if (!event.callEvent()) return null;
        includedMaterials = event.getIncludedMaterials();
        ignoredMaterials = event.getExcludedMaterials();
        multiBreak = event.getMultiBreak();
        if (!multiBreak.isValid(includedMaterials, ignoredMaterials)) return null;
        float progressPerTick = multiBreak.getBlock().getBreakSpeed(p);
        multiBreak.checkValid(progressPerTick, includedMaterials, ignoredMaterials);
        multiBlockMap.put(p.getUniqueId(), multiBreak);
        return multiBreak;
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
        FigureItemDataType figureItemDataType = new FigureItemDataType(this.getPlugin());
        Figure figure = figureItemDataType.get(tool);
        if (figure == null) {
            Material material = tool.getType();
            ConfigManager configManager = this.getPlugin().getConfigManager();
            if (configManager.getMaterialOptions().containsKey(material)) {
                String configOptionName = configManager.getMaterialOptions().get(material);
                figure = configManager.getConfigOptions().get(configOptionName);
            }
        }
        RequestFigureEvent requestFigureEvent = new RequestFigureEvent(figure, p, tool);
        requestFigureEvent.callEvent();
        if (requestFigureEvent.isCancelled()) {
            figureCache.put(p.getUniqueId(), null);
            return null;
        }
        figure = requestFigureEvent.getFigure();
        figureCache.put(p.getUniqueId(), figure);
        return figure;
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
