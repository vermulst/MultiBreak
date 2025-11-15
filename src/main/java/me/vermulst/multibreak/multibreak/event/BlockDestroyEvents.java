package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.multibreak.BreakManager;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class BlockDestroyEvents implements Listener {
    private final BreakManager breakManager;
    public BlockDestroyEvents(BreakManager breakManager) {
        this.breakManager = breakManager;
    }


    @EventHandler
    public void liquidFLowEvent(BlockFromToEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }

    @EventHandler
    public void entityChangeBlock(EntityChangeBlockEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }

    @EventHandler
    public void pistonExtend(BlockPistonExtendEvent e) {
        if (e.isCancelled()) return;
        Set<Location> blockLocations = e.getBlocks().stream().map(Block::getLocation).collect(Collectors.toSet());
        breakManager.handleBlockRemovals(blockLocations);
    }

    @EventHandler
    public void pistonRetract(BlockPistonRetractEvent e) {
        if (e.isCancelled() || !e.isSticky()) return;
        Set<Location> blockLocations = e.getBlocks().stream().map(Block::getLocation).collect(Collectors.toSet());
        breakManager.handleBlockRemovals(blockLocations);
    }

}
