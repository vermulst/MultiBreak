package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.api.event.MultiBreakEndEvent;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.BreakManager;
import me.vermulst.multibreak.multibreak.MultiBreak;
import me.vermulst.multibreak.multibreak.MultiBreakType;
import me.vermulst.multibreak.utils.LocationKeyUtil;
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
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.RayTraceResult;

import java.util.Optional;
import java.util.UUID;

public class BreakEvents implements Listener {

    private static final String STATIC_MULTIBREAK_META = "static-multibreak";
    private static final int MAX_TICKS_BETWEEN_COMBAT_AND_STATIC_BREAK = 5;

    private final BreakManager breakManager;
    public BreakEvents(BreakManager breakManager) {
        this.breakManager = breakManager;
    }

    private record MultiBreakTypeInfo(MultiBreakType multiBreakType, long blockLocation) {
        public static MultiBreakTypeInfo of(MultiBreakType multiBreakType, Block block) {
            return new MultiBreakTypeInfo(multiBreakType, LocationKeyUtil.packBlock(block));
        }
    }

    private Optional<MultiBreakTypeInfo> popStaticMultiBreakMeta(Player player) {
        if (!player.hasMetadata(STATIC_MULTIBREAK_META)) {
            return Optional.empty();
        }
        MultiBreakTypeInfo info = (MultiBreakTypeInfo) player.getMetadata(STATIC_MULTIBREAK_META).getFirst().value();
        player.removeMetadata(STATIC_MULTIBREAK_META, Main.getInstance());
        return Optional.of(info);
    }

    // events sorted on chronological order

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        Figure figure = breakManager.getFigure(p, item);
        if (figure == null) return;
        Optional<MultiBreakTypeInfo> staticInfoOpt = popStaticMultiBreakMeta(p);

        if (staticInfoOpt.isPresent()) {
            MultiBreakTypeInfo info = staticInfoOpt.get();
            long packedCurrentBlock = LocationKeyUtil.packBlock(e.getBlock());

            // Check if the block being damaged matches the block from the static metadata
            if (packedCurrentBlock == info.blockLocation()) {
                breakManager.scheduleMultiBreak(p, figure, e.getBlock(), info.multiBreakType());
                return;
            }
        }
        // Normal or mismatched static break -> start normal break
        breakManager.scheduleMultiBreak(p, figure, e.getBlock(), MultiBreakType.NORMAL);
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
            multiBreak = breakManager.initMultiBreak(p, block, figure, blockFace, MultiBreakType.NORMAL);
            if (multiBreak == null) {
                breakManager.handleBlockRemoval(location);
                return;
            }
        }

        // Mismatch (player switched to an instamine-block while breaking)
        if (!block.equals(multiBreak.getBlock())) {
            multiBreak = breakManager.initMultiBreak(p, block, figure, MultiBreakType.NORMAL);
        }
        boolean finished = !e.isCancelled();
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, finished);
        event.callEvent();
        breakManager.endMultiBreak(p, event.getMultiBreak(), finished);
        breakManager.handleBlockRemoval(location);
        if (!finished) {
            MultiBreakTypeInfo multiBreakTypeInfo = MultiBreakTypeInfo.of(MultiBreakType.CANCELLED_STATIC, block);
            p.setMetadata(STATIC_MULTIBREAK_META, new FixedMetadataValue(Main.getInstance(), multiBreakTypeInfo));
        }
    }

    /** so that punching a mob won't cause static breaks */
    @EventHandler
    public void combat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        if (breakManager.getFigure(p, item) == null) return;

        MultiBreak multiBreak = breakManager.getMultiBreakOffstate(p);
        if (multiBreak == null) {
            UUID uuid = p.getUniqueId();
            multiBreak = new MultiBreak(uuid);
            breakManager.getMultiBreakMap().put(uuid, multiBreak);
        }
        multiBreak.setEnded(Bukkit.getCurrentTick());
    }

    /** out-of-range breaks like jump breaks for breaking trees for example */
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

        // Initiate new static break
        if (breakManager.isBreaking(p.getUniqueId())) return;
        MultiBreak multiBreakOffState = breakManager.getMultiBreakOffstate(p);
        if (multiBreakOffState == null) return;
        int ended = multiBreakOffState.getEnded();
        if (Bukkit.getCurrentTick() - ended <= MAX_TICKS_BETWEEN_COMBAT_AND_STATIC_BREAK) return;
        RayTraceResult rayTraceResult = BreakUtils.getRayTraceResultExact(p);
        if (rayTraceResult == null) return;
        Block targetBlock = rayTraceResult.getHitBlock();
        BlockFace face = rayTraceResult.getHitBlockFace();
        if (!p.hasMetadata(STATIC_MULTIBREAK_META)) {
            MultiBreakTypeInfo multiBreakTypeInfo = MultiBreakTypeInfo.of(MultiBreakType.STATIC, targetBlock);
            p.setMetadata(STATIC_MULTIBREAK_META, new FixedMetadataValue(Main.getInstance(), multiBreakTypeInfo));
        }
        BlockDamageEvent blockDamageEvent = new BlockDamageEvent(p, targetBlock, face, item, false);
        blockDamageEvent.callEvent();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        breakManager.onPlayerQuit(p);
    }
}
