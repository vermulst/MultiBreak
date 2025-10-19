package me.vermulst.multibreak.api.event;

import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.multibreak.MultiBlock;
import me.vermulst.multibreak.multibreak.MultiBreak;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;

public class MultiBreakStartEvent extends Event {

    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean isCancelled;

    private final Player player;
    private final Vector playerDirection;
    private final Block block;
    private MultiBreak multiBreak;
    private final boolean fair_mode;
    private final EnumSet<Material> ignoredMaterials;


    public MultiBreakStartEvent(Player p, MultiBreak multiBreak, Block block, Vector playerDirection, boolean fair_mode, EnumSet<Material> ignoredMaterials) {
        this.player = p;
        this.multiBreak = multiBreak;
        this.playerDirection = playerDirection;
        this.block = block;
        this.fair_mode = fair_mode;
        this.ignoredMaterials = ignoredMaterials;
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

    public void setFigure(Figure figure) {
        if (this.multiBreak == null) {
            this.multiBreak = new MultiBreak(this.getPlayer(), block, figure, this.getPlayerDirection(), this.fair_mode, this.ignoredMaterials);
        } else {
            this.getMultiBreak().setFigure(figure);
        }
    }

    public MultiBreak getMultiBreak() {
        return multiBreak;
    }

    public ArrayList<Block> getBlocks() {
        ArrayList<Block> blocks = new ArrayList<>();
        if (this.getMultiBreak() != null) {
            blocks.addAll(multiBreak.getBlocks());
        }
        if (!blocks.contains(this.getBlock())) {
            blocks.add(this.getBlock());
        }
        return blocks;
    }

    public EnumSet<Material> getIgnoredMaterials() {
        return ignoredMaterials;
    }

    public Vector getPlayerDirection() {
        return playerDirection;
    }

    public Block getBlock() {
        return block;
    }
}
