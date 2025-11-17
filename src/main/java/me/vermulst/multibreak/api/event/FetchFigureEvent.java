package me.vermulst.multibreak.api.event;

import me.vermulst.multibreak.figure.Figure;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class FetchFigureEvent extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    private @NotNull final Player player;
    private @NotNull final ItemStack item;
    private boolean cancelled = false;

    private Figure figure; // set to null to prevent any multibreaking


    public FetchFigureEvent(Figure figure, @NotNull Player p, @NotNull ItemStack item) {
        this.figure = figure;
        this.player = p;
        this.item = item;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public Figure getFigure() {
        return figure;
    }

    public void setFigure(Figure figure) {
        this.figure = figure;
    }

    public @NotNull ItemStack getItem() {
        return item;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }



}
