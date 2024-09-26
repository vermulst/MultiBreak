package me.vermulst.multibreak.multibreak;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MultiBlock {

    private final Block block;
    private final boolean hasAdjacentAir;
    private boolean breakThisBlock = true;
    private ArrayList<ItemStack> drops;
    private final int sourceID = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

    public MultiBlock(Block block) {
        this.block = block;
        this.hasAdjacentAir = this.initHasAdjacentAir(block);
    }

    public boolean initHasAdjacentAir(Block b) {
        Location loc = b.getLocation();
        for (int i = -1; i <= 1; i += 2) {
            if (loc.clone().add(new Vector(i, 0, 0)).getBlock().getType().equals(Material.AIR) ||
                    loc.clone().add(new Vector(0, 0, i)).getBlock().getType().equals(Material.AIR) ||
                    loc.clone().add(new Vector(0, i, 0)).getBlock().getType().equals(Material.AIR)) {
                return true;
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

    public boolean hasAdjacentAir() {
        return hasAdjacentAir;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiBlock that = (MultiBlock) o;
        return hasAdjacentAir == that.hasAdjacentAir && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, hasAdjacentAir);
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
