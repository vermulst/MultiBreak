package me.vermulst.multibreak.multibreak;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MultiBlock {

    private final Block block;
    private boolean isVisible;
    private final Material type;
    private final int sourceID = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

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

    public Location getLocation() {
        return block.getLocation();
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
