package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.api.event.MultiBreakEndEvent;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.BreakManager;
import me.vermulst.multibreak.multibreak.MultiBreak;
import me.vermulst.multibreak.utils.BreakUtils;
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

    // events sorted on chronological order

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        Figure figure = breakManager.getFigure(p, item);
        if (figure == null) return;
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


    @EventHandler(priority = EventPriority.MONITOR)
    public void breakBlockType(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Figure figure = breakManager.getFigure(p);
        if (figure == null) return;

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
        MultiBreak multiBreak = breakManager.getMultiBreak(p);

        // insta mine
        if (multiBreak == null) {
            BlockFace blockFace = BreakUtils.getBlockFace(p);
            if (blockFace == null) {
                blockFace = BreakUtils.getThickRaytraceBlockFace(p, block);
            }
            multiBreak = breakManager.initMultiBreak(p, block, figure, blockFace);
            if (multiBreak == null) {
                breakManager.handleBlockRemoval(location);
                return;
            }
        }

        // Mismatch (player switched to an instamine-block while breaking)
        if (!block.equals(multiBreak.getBlock())) {
            multiBreak = breakManager.initMultiBreak(p, block, figure);
        }
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, true);
        event.callEvent();
        breakManager.endMultiBreak(p, event.getMultiBreak(), true);
        breakManager.handleBlockRemoval(location);
    }

    /** Weird edge case where animation persists after breaking stops */
    @EventHandler
    public void mining(PlayerAnimationEvent e) {
        if (!e.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;
        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        Figure figure = breakManager.getFigure(p, item);
        if (figure == null) return;
        MultiBreak multiBreak = breakManager.getMultiBreak(p);

        // Update static break
        if (multiBreak != null) {
            if (multiBreak.isNotStatic()) return;
            RayTraceResult rayTraceResult = BreakUtils.getRayTraceResultExact(p);
            if (rayTraceResult == null) return;
            multiBreak.setLastTick(Bukkit.getCurrentTick());
            return;
        }

        // Initiate static breaks
        if (breakManager.isBreaking(p.getUniqueId())) return;
        MultiBreak multiBreakOffState = breakManager.getMultiBreakOffstate(p);
        if (multiBreakOffState == null) return;
        int ended = multiBreakOffState.getEnded();
        if (Bukkit.getCurrentTick() - ended <= 5) return;
        RayTraceResult rayTraceResult = BreakUtils.getRayTraceResultExact(p);
        if (rayTraceResult == null) return;
        Block targetBlock = rayTraceResult.getHitBlock();
        BlockFace face = rayTraceResult.getHitBlockFace();
        p.setMetadata("static-multibreak", new FixedMetadataValue(Main.getInstance(), true));
        BlockDamageEvent blockDamageEvent = new BlockDamageEvent(p, targetBlock, face, item, false);
        blockDamageEvent.callEvent();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        breakManager.onPlayerQuit(p);
    }


}
