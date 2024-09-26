package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.item.FigureItemInfo;
import me.vermulst.multibreak.multibreak.event.MultiBreakEndEvent;
import me.vermulst.multibreak.multibreak.event.MultiBreakStartEvent;
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

import java.util.*;

public class BreakManager implements Listener {

    private final Main plugin;

    private final HashMap<UUID, Integer> multiBreakTask = new HashMap<>();
    private final HashMap<UUID, MultiBreak> multiBlockHashMap = new HashMap<>();

    public BreakManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void armSwingEvent(PlayerAnimationEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[1];
        if (!legacy_mode) return;
        if (!e.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;
        Player p = e.getPlayer();
        if (p.getGameMode().equals(GameMode.CREATIVE)) return;
        MultiBreak multiBreak = getMultiBreak(p);
        if (multiBreak == null) return;
        Block blockMining = this.getTargetBlock(p);
        multiBreak.tick(this.getPlugin(), blockMining);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void multiBreakStart(BlockDamageEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[1];
        if (legacy_mode) return;
        this.scheduleMultiBreak(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void multiBreakStop(BlockDamageAbortEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[1];
        if (legacy_mode) return;
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getMultiBreak(p);
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, false);
        event.callEvent();
        if (multiBreak == null) return;
        this.end(p, multiBreak, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void breakBlockType(BlockBreakEvent e) {
        if (this.ignoreMultiBreak(e)) return;
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getMultiBreak(p);
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, true);
        event.callEvent();
        if (event.isCancelled()) return;
        if (event.getMultiBreak() == null) return;
        this.end(p, event.getMultiBreak(), true);
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


    public void scheduleMultiBreak(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                MultiBreak multiBreak = getMultiBreak(p);
                if (multiBreak == null) return;
                int taskID = new BukkitRunnable() {
                    @Override
                    public void run() {
                        multiBreak.tick();
                    }
                }.runTaskTimer(getPlugin(), 0, 1).getTaskId();
                getMultiBreakTask().put(p.getUniqueId(), taskID);
            }
        }.runTaskLater(plugin, 1);
    }

    public void end(Player p, MultiBreak multiBreak, boolean finished) {
        UUID uuid = p.getUniqueId();
        multiBreak.end(finished, getPlugin());
        this.getMultiBlockHashMap().remove(uuid);
        if (!this.getMultiBreakTask().containsKey(uuid)) return;
        Bukkit.getScheduler().cancelTask(this.getMultiBreakTask().get(uuid));
    }

    public MultiBreak getMultiBreak(Player p) {
        if (p.getGameMode().equals(GameMode.CREATIVE)) return null;
        if (multiBlockHashMap.containsKey(p.getUniqueId())) {
            MultiBreak multiBreak = multiBlockHashMap.get(p.getUniqueId());
            if (!multiBreak.hasEnded()) return multiBreak;
        }
        //init
        ItemStack tool = p.getInventory().getItemInMainHand();
        Figure figure = this.getFigure(tool);
        BlockFace blockFace = this.getBlockFace(p);
        Block blockMining = this.getTargetBlock(p);
        ConfigManager config = plugin.getConfigManager();
        boolean fair_mode = config.getOptions()[0];
        List<Material> ignoredMaterials = config.getIgnoredMaterials();
        MultiBreak multiBreak = new MultiBreak(p, blockMining, figure, blockFace.getDirection(), fair_mode, ignoredMaterials);
        MultiBreakStartEvent event = new MultiBreakStartEvent(p, multiBreak, blockMining, blockFace.getDirection(), fair_mode, ignoredMaterials);
        if (!event.callEvent()) return null;
        MultiBreak multiBreak1 = event.getMultiBreak();
        multiBlockHashMap.put(p.getUniqueId(), multiBreak1);
        return multiBreak1;
    }

    public Figure getFigure(ItemStack tool) {
        if (tool.getItemMeta() == null) return null;
        FigureItemInfo figureItemInfo = this.getFigureItemInfo(tool);
        if (figureItemInfo == null) {
            Material material = tool.getType();
            ConfigManager configManager = this.getPlugin().getConfigManager();
            if (configManager.getMaterialOptions().containsKey(material)) {
                String configOptionName = configManager.getMaterialOptions().get(material);
                return configManager.getConfigOptions().get(configOptionName);
            }
            return null;
        } else {
            return figureItemInfo.figure();
        }
    }

    public FigureItemInfo getFigureItemInfo(ItemStack item) {
        FigureItemDataType figureItemDataType = new FigureItemDataType(this.getPlugin());
        return figureItemDataType.get(item);
    }

    public Block getTargetBlock(Player p) {
        return p.getTargetBlockExact(10);
    }

    public BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(10);
    }

    public Main getPlugin() {
        return plugin;
    }

    public HashMap<UUID, Integer> getMultiBreakTask() {
        return multiBreakTask;
    }

    public HashMap<UUID, MultiBreak> getMultiBlockHashMap() {
        return multiBlockHashMap;
    }
}
