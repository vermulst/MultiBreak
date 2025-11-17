package me.vermulst.multibreak.api.event;

import me.vermulst.multibreak.multibreak.MultiBreak;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MultiBreakStartEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean isCancelled;

    private @NotNull final Player player;
    private @NotNull final Block block;
    private @NotNull final MultiBreak multiBreak;


    public MultiBreakStartEvent(@NotNull Player p, @NotNull MultiBreak multiBreak, @NotNull Block block) {
        this.player = p;
        this.multiBreak = multiBreak;
        this.block = block;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    public @NotNull MultiBreak getMultiBreak() {
        return multiBreak;
    }

    public List<Block> getBlocks() {
        return new ArrayList<>(multiBreak.getBlocks());
    }

    public @NotNull Block getBlock() {
        return block;
    }
}
