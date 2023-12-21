package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.MultiBlock;
import me.vermulst.multibreak.multibreak.MultiBreak;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MultiBreakStartEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final Player player;
    private final MultiBreak multiBreak;
    private boolean isCancelled;


    public MultiBreakStartEvent(Player p, MultiBreak multiBreak) {
        this.player = p;
        this.multiBreak = multiBreak;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public MultiBreak getMultiBreak() {
        return multiBreak;
    }

    public ArrayList<MultiBlock> getMultiBlocks() {
        return multiBreak.getMultiBlocks();
    }

    public ArrayList<Block> getBlocks() {
        return multiBreak.getBlocks();
    }
}
