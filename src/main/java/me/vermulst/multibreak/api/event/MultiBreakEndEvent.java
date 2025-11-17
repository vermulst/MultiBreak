package me.vermulst.multibreak.api.event;

import me.vermulst.multibreak.multibreak.MultiBreak;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MultiBreakEndEvent extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    private @NotNull final Player player;
    private @NotNull final MultiBreak multiBreak;
    private final boolean successful;

    public MultiBreakEndEvent(@NotNull Player p, @NotNull MultiBreak multiBreak, boolean successful) {
        this.player = p;
        this.multiBreak = multiBreak;
        this.successful = successful;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public Block getBlock() {
        return multiBreak.getBlock();
    }

    public @NotNull MultiBreak getMultiBreak() {
        return multiBreak;
    }

    public List<Block> getBlocks() {
        return multiBreak.getBlocks();
    }

    public boolean isSuccessful() {
        return successful;
    }
}
