package me.vermulst.multibreak.multibreak;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MultiBlock {

    private final Block block;
    private final boolean isVisible;
    private boolean breakThisBlock = true;
    private ArrayList<ItemStack> drops;
    private final int sourceID = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

    public MultiBlock(Block block) {
        this.block = block;
        this.isVisible = this.initVisibility(block);
    }

    public boolean initVisibility(Block b) {
        for (BlockFace face : BlockFace.values()) {
            if (face.isCartesian()) { // Filter for the 6 cardinal directions
                Block adjacent = b.getRelative(face);
                if (adjacent.isEmpty() || adjacent.isLiquid()) {
                    return true;
                }
                if (!adjacent.getType().isOccluding()) {
                    return true;
                }
            }
        }
        return false;
    }

    public void writeStage(Player p, float stage) {
        p.sendBlockDamage(this.getBlock().getLocation(), stage, sourceID);
    }

    public Block getBlock() {
        return block;
    }

    public boolean isVisible() {
        return isVisible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiBlock that = (MultiBlock) o;
        return isVisible == that.isVisible && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, isVisible);
    }

    public boolean breakThisBlock() {
        return breakThisBlock;
    }

    public void setBreakThisBlock(boolean breakThisBlock) {
        this.breakThisBlock = breakThisBlock;
    }

    public void setDrops(ArrayList<ItemStack> drops) {
        this.drops = drops;
    }

    public ArrayList<ItemStack> getDrops() {
        return drops;
    }
}
