package me.vermulst.multibreak.multibreak.event;

import me.vermulst.multibreak.multibreak.MultiBreak;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class LegacyModeEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean legacy = false;

    public LegacyModeEvent() {
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }
}
