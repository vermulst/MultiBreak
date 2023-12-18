package me.vermulst.multibreak.multibreak;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class MultiBlock {

    private final Block block;
    private final boolean hasAdjacentAir;
    private PacketContainer packetContainer;

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

    public void writeStage(int stage) {
        Location loc = this.getBlock().getLocation();
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        if (this.packetContainer == null) {
            this.packetContainer = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
            int randomID = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
            this.packetContainer.getIntegers().write(0, randomID);
            this.packetContainer.getBlockPositionModifier().write(0, new BlockPosition((int) loc.getX(), (int) loc.getY(), (int) loc.getZ()));
        }
        packetContainer.getIntegers().write(1, stage);
        protocolManager.broadcastServerPacket(packetContainer);
    }

    public Block getBlock() {
        return block;
    }

    public boolean hasAdjacentAir() {
        return hasAdjacentAir;
    }

}
