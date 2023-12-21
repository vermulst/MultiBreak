package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.figure.types.FigureCircle;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.item.FigureItemInfo;
import me.vermulst.multibreak.multibreak.event.MultiBreakEndEvent;
import me.vermulst.multibreak.multibreak.event.MultiBreakStartEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
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

    @EventHandler
    public void multiBreakStart(BlockDamageEvent e) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Player p = e.getPlayer();
                MultiBreak multiBreak = getMultiBreak(p);
                BlockFace blockFace = getBlockFace(p);
                MultiBreakStartEvent event = new MultiBreakStartEvent(p, multiBreak, e.getBlock(), blockFace.getDirection());
                if (!event.callEvent()) return;
                if (!multiBreak.equals(event.getMultiBreak())) {
                    multiBreak = event.getMultiBreak();
                    multiBlockHashMap.put(p.getUniqueId(), multiBreak);
                }
                if (multiBreak == null) return;
                MultiBreak finalMultiBreak = multiBreak;
                int taskID = new BukkitRunnable() {
                    @Override
                    public void run() {
                        finalMultiBreak.tick();
                    }
                }.runTaskTimer(getPlugin(), 0, 1).getTaskId();
                getMultiBreakTask().put(p.getUniqueId(), taskID);
            }
        }.runTaskLater(plugin, 1);
    }



    @EventHandler
    public void multiBreakStop(BlockDamageAbortEvent e) {
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getMultiBreak(p);
        new MultiBreakEndEvent(p, multiBreak, false).callEvent();
        if (multiBreak == null) return;
        this.end(p, multiBreak, false);
    }

    @EventHandler
    public void breakBlockType(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (e.isCancelled()) return;
        MultiBreak multiBreak = this.getMultiBreak(p);
        new MultiBreakEndEvent(p, multiBreak, true).callEvent();
        if (multiBreak == null) return;
        this.end(p, multiBreak, true);
    }

    public void end(Player p, MultiBreak multiBreak, boolean finished) {
        UUID uuid = p.getUniqueId();
        multiBreak.end(finished);
        this.getMultiBlockHashMap().remove(uuid);
        Bukkit.getScheduler().cancelTask(this.getMultiBreakTask().get(uuid));
    }

    public MultiBreak getMultiBreak(Player p) {
        if (p.getGameMode().equals(GameMode.CREATIVE)) return null;
        ItemStack tool = p.getInventory().getItemInMainHand();
        Figure figure = this.getFigure(tool);
        if (figure == null) return null;
        BlockFace blockFace = this.getBlockFace(p);
        if (blockFace == null) return null;
        Block blockMining = this.getTargetBlock(p);
        return multiBlockHashMap.computeIfAbsent(p.getUniqueId(),
                b -> new MultiBreak(p, blockMining, figure, blockFace.getDirection()));
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
