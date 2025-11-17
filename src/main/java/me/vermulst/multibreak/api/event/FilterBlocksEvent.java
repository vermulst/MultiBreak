package me.vermulst.multibreak.api.event;

import me.vermulst.multibreak.figure.Figure;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class FilterBlocksEvent extends Event {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }

    @NotNull private final Player player;
    @NotNull private final ItemStack item;
    @NotNull private EnumSet<Material> includedMaterials;
    @NotNull private EnumSet<Material> excludedMaterials;

    private Figure figure; // set to null to prevent any multibreaking

    public FilterBlocksEvent(@NotNull Figure figure, @NotNull Player p, @NotNull ItemStack item, @NotNull EnumSet<Material> includedMaterials, @NotNull EnumSet<Material> excludedMaterials) {
        this.figure = figure;
        this.player = p;
        this.item = item;
        this.includedMaterials = includedMaterials;
        this.excludedMaterials = excludedMaterials;
    }

    public Figure getFigure() {
        return figure;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @NotNull ItemStack getItem() {
        return item;
    }

    public @NotNull EnumSet<Material> getIncludedMaterials() {
        return includedMaterials;
    }

    public @NotNull EnumSet<Material> getExcludedMaterials() {
        return excludedMaterials;
    }

    public void setFigure(Figure figure) {
        this.figure = figure;
    }

    public void excludeOnly(@NotNull EnumSet<Material> excludedMaterials) {
        this.excludedMaterials = excludedMaterials;
    }

    public void includeOnly(@NotNull EnumSet<Material> includedMaterials) {
        this.includedMaterials = includedMaterials;
    }

    public void exclude(@NotNull Material material) {
        this.excludedMaterials.add(material);
    }

    public void include(@NotNull Material material) {
        this.includedMaterials.add(material);
    }
}
