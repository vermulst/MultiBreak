package me.vermulst.multibreak.multibreak;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MultiBlock {

    private final Block block;
    private boolean isVisible;
    private boolean breakThisBlock = true;
    private final Material type;
    private List<ItemStack> drops;
    private final int sourceID = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);


    private boolean mismatchedType = false;
    private volatile int lastStage = 0;

    private static final BlockFace[] cartesianBlockFaces = new BlockFace[] {
            BlockFace.NORTH,
            BlockFace.EAST,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    public MultiBlock(Block block) {
        this.block = block;
        this.type = block.getType();
        this.isVisible = this.initVisibility(block);
    }

    public boolean initVisibility(Block b) {
        for (BlockFace face : cartesianBlockFaces) {
            Block adjacent = b.getRelative(face);
            if (adjacent.isEmpty() || (adjacent.isLiquid() && adjacent.getType() != Material.POWDER_SNOW)) {
                return true;
            }
            if (!adjacent.getType().isOccluding()) {
                return true;
            }
        }
        return false;
    }

    public void setMismatchedType(boolean mismatchedType) {
        this.mismatchedType = mismatchedType;
    }

    public boolean mismatchesType() {
        return mismatchedType;
    }

    public Block getBlock() {
        return block;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
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

    public List<ItemStack> getDrops() {
        return drops;
    }

    public Material getType() {
        return type;
    }

    public int getSourceID() {
        return sourceID;
    }

    public int getLastStage() {
        return lastStage;
    }

    public void setLastStage(int lastStage) {
        this.lastStage = lastStage;
    }
}
