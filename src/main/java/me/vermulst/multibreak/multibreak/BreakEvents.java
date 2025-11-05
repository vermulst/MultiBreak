package me.vermulst.multibreak.multibreak;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.vermulst.multibreak.api.event.MultiBreakEndEvent;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.figure.Figure;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
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

public class BreakEvents implements Listener {

    private final BreakManager breakManager;
    public BreakEvents(BreakManager breakManager) {
        this.breakManager = breakManager;
    }

    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        breakManager.refresh(e.getPlayer());
    }

    @EventHandler
    public void itemHeld(PlayerItemHeldEvent e) {
        breakManager.refresh(e.getPlayer());
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        breakManager.refresh((Player) e.getWhoClicked());
    }

    @EventHandler
    public void dragEvent(InventoryDragEvent e) {
        breakManager.refresh((Player) e.getWhoClicked());
    }

    @EventHandler
    public void swapOffhand(PlayerSwapHandItemsEvent e) {
        breakManager.refresh(e.getPlayer());
    }


    @EventHandler
    public void tickEvent(ServerTickEndEvent e) {
        boolean fairMode = Config.getInstance().isFairModeEnabled();
        if (!fairMode) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            breakManager.setBreakSpeed(p);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        Figure figure = breakManager.getFigure(p, item);
        if (figure == null) return;
        breakManager.scheduleMultiBreak(e.getPlayer(), figure);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStop(BlockDamageAbortEvent e) {
        Player p = e.getPlayer();
        MultiBreak multiBreak = breakManager.getMultiBreak(p);
        if (multiBreak == null) return;
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, false);
        event.callEvent();
        breakManager.endMultiBreak(p, multiBreak, false);
    }

    /** Block broken */

    @EventHandler(priority = EventPriority.MONITOR)
    public void breakBlockType(BlockBreakEvent e) {
        if (breakManager.ignoreMultiBreak(e)) return;
        Player p = e.getPlayer();
        MultiBreak multiBreak = breakManager.getMultiBreak(p);
        if (multiBreak == null) {
            Figure figure = breakManager.getFigure(p);
            multiBreak = breakManager.initMultiBreak(p, e.getBlock(), figure);
            if (multiBreak == null) return;
        }
        Block block = e.getBlock();

        // Mismatch (player switched to an instamine-block while breaking)
        if (!block.equals(multiBreak.getBlock())) {
            Figure figure = breakManager.getFigure(p);
            multiBreak = breakManager.initMultiBreak(p, block, figure);
        }
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, true);
        event.callEvent();
        if (event.isCancelled()) return;
        if (event.getMultiBreak() == null) return;
        breakManager.endMultiBreak(p, event.getMultiBreak(), true);
    }


}
