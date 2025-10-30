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

    private final Player player;
    private final ItemStack item;

    // set to null to prevent any multibreaking
    private Figure figure;
    private EnumSet<Material> includedMaterials;
    private EnumSet<Material> excludedMaterials;



    public FilterBlocksEvent(Figure figure, Player p, ItemStack item, EnumSet<Material> includedMaterials, EnumSet<Material> excludedMaterials) {
        this.figure = figure;
        this.player = p;
        this.item = item;
        this.includedMaterials = includedMaterials;
        this.excludedMaterials = excludedMaterials;
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

    public Figure getFigure() {
        return figure;
    }

    public void setFigure(Figure figure) {
        this.figure = figure;
    }

    public ItemStack getItem() {
        return item;
    }

    public EnumSet<Material> getIncludedMaterials() {
        return includedMaterials;
    }

    public EnumSet<Material> getExcludedMaterials() {
        return excludedMaterials;
    }

    public void excludeOnly(EnumSet<Material> excludedMaterials) {
        this.excludedMaterials = excludedMaterials;
    }

    public void includeOnly(EnumSet<Material> includedMaterials) {
        this.includedMaterials = includedMaterials;
    }

    public void exclude(Material material) {
        this.excludedMaterials.add(material);
    }

    public void include(Material material) {
        this.includedMaterials.add(material);
    }
}
