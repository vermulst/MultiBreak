package me.vermulst.multibreak.multibreak.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;


public class LegacyModeEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean legacy = true;

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
