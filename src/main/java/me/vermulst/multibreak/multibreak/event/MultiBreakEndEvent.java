package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.multibreak.MultiBlock;
import me.vermulst.multibreak.multibreak.MultiBreak;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MultiBreakEndEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final Player player;
    private final MultiBreak multiBreak;
    private final boolean successful;

    public MultiBreakEndEvent(Player p, MultiBreak multiBreak, boolean successful) {
        this.player = p;
        this.multiBreak = multiBreak;
        this.successful = successful;
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

    public Block getBlock() {
        return multiBreak.getBlock();
    }

    public MultiBreak getMultiBreak() {
        return multiBreak;
    }

    public ArrayList<Block> getBlocks() {
        return multiBreak.getBlocks();
    }

    public boolean isSuccessful() {
        return successful;
    }
}
