package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.multibreak.BreakManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.Map;

public class BlockChangeEvents implements Listener {
    private final Map<Location, Integer> multiblockMap;
    public BlockChangeEvents(BreakManager breakManager) {
        this.multiblockMap = breakManager.getMultiblockMap();
    }


    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        multiblockMap.remove(e.getBlock().getLocation());
    }

    @EventHandler
    public void liquidFLowEvent(BlockFromToEvent e) {
        multiblockMap.remove(e.getBlock().getLocation());
    }

    @EventHandler
    public void blockForm(BlockFormEvent e) {
        multiblockMap.remove(e.getBlock().getLocation());
    }

    @EventHandler
    public void entityChangeBlock(EntityChangeBlockEvent e) {
        multiblockMap.remove(e.getBlock().getLocation());
    }

    @EventHandler
    public void pistonExtend(BlockPistonExtendEvent e) {
        multiblockMap.remove(e.getBlock().getLocation());
    }

    @EventHandler
    public void pistonRetract(BlockPistonRetractEvent e) {
        multiblockMap.remove(e.getBlock().getLocation());
    }



}
