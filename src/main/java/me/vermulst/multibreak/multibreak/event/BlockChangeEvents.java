package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.multibreak.BreakManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.Map;

public class BlockChangeEvents implements Listener {
    private final BreakManager breakManager;
    public BlockChangeEvents(BreakManager breakManager) {
        this.breakManager = breakManager;
    }


    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }

    @EventHandler
    public void liquidFLowEvent(BlockFromToEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }

    @EventHandler
    public void blockForm(BlockFormEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }

    @EventHandler
    public void entityChangeBlock(EntityChangeBlockEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }

    @EventHandler
    public void pistonExtend(BlockPistonExtendEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }

    @EventHandler
    public void pistonRetract(BlockPistonRetractEvent e) {
        breakManager.handleBlockRemoval(e.getBlock().getLocation());
    }



}
