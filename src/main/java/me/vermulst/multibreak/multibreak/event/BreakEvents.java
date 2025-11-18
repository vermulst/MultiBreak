package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.api.event.MultiBreakEndEvent;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.BreakManager;
import me.vermulst.multibreak.multibreak.MultiBreak;
import me.vermulst.multibreak.utils.BreakUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;

public class BreakEvents implements Listener {

    private final BreakManager breakManager;
    public BreakEvents(BreakManager breakManager) {
        this.breakManager = breakManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        Figure figure = breakManager.getFigure(p, item);
        if (figure == null) return;
        p.sendMessage(Component.text("starting", NamedTextColor.GREEN));
        if (p.hasMetadata("static-multibreak")) {
            breakManager.scheduleMultiBreak(p, figure, e.getBlock(), true);
            p.removeMetadata("static-multibreak", Main.getInstance());
        } else {
            breakManager.scheduleMultiBreak(p, figure, e.getBlock(), false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStop(BlockDamageAbortEvent e) {
        Player p = e.getPlayer();
        if (!breakManager.isBreaking(p.getUniqueId())) return;
        MultiBreak multiBreak = breakManager.getMultiBreak(p);
        if (multiBreak != null) {
            MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, false);
            event.callEvent();
        }
        breakManager.endMultiBreak(p, multiBreak, false);
    }

    /** Weird edge case */
    @EventHandler
    public void mining(PlayerAnimationEvent e) {
        if (!e.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        Figure figure = breakManager.getFigure(p, item);
        if (figure == null) return;
        MultiBreak multiBreak = breakManager.getMultiBreak(p);
        if (multiBreak != null) {
            if (multiBreak.getLastTick() == -1) return; // not a static break
            multiBreak.setLastTick(Bukkit.getCurrentTick());
            return;
        }
        if (breakManager.isBreaking(p.getUniqueId())) return;
        RayTraceResult rayTraceResult = BreakUtils.getRayTraceResult(p);
        if (rayTraceResult == null) return;
        Block targetBlock = rayTraceResult.getHitBlock();
        BlockFace face = rayTraceResult.getHitBlockFace();
        p.setMetadata("static-multibreak", new FixedMetadataValue(Main.getInstance(), true));
        BlockDamageEvent blockDamageEvent = new BlockDamageEvent(p, targetBlock, face, item, false);
        blockDamageEvent.callEvent();
    }

    /** Block broken */

    @EventHandler(priority = EventPriority.MONITOR)
    public void breakBlockType(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location location = block.getLocation();
        if (!breakManager.isMultiBreak(e)) {
            if (!breakManager.wasMultiBroken(block)) {
                breakManager.handleBlockRemoval(location);
            } else {
                breakManager.removeMultiBrokenMetadata(block);
            }
            return;
        }
        Player p = e.getPlayer();
        MultiBreak multiBreak = breakManager.getMultiBreak(p);
        if (multiBreak == null) {
            Figure figure = breakManager.getFigure(p);
            multiBreak = breakManager.initMultiBreak(p, e.getBlock(), figure);
            if (multiBreak == null) {
                breakManager.handleBlockRemoval(location);
                return;
            }
        }

        // Mismatch (player switched to an instamine-block while breaking)
        if (!block.equals(multiBreak.getBlock())) {
            Figure figure = breakManager.getFigure(p);
            multiBreak = breakManager.initMultiBreak(p, block, figure);
        }
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, true);
        event.callEvent();
        breakManager.endMultiBreak(p, event.getMultiBreak(), true);
        breakManager.handleBlockRemoval(location);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        breakManager.onPlayerQuit(p);
    }


}
