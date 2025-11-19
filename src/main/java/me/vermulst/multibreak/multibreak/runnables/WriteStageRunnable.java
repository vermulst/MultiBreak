package me.vermulst.multibreak.multibreak.runnables;

import me.vermulst.multibreak.multibreak.MultiBlock;
import me.vermulst.multibreak.multibreak.MultiBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class WriteStageRunnable extends BukkitRunnable {

    private final int stage;
    private final ServerGamePacketListenerImpl[] connections;
    private final MultiBlock[] multiBlocks;
    private final Block mainBlock;
    private final ReentrantLock lock;
    private final MultiBreak multiBreak;

    public WriteStageRunnable(MultiBlock[] multiBlocks, Block mainBlock, int stage,
                              ServerGamePacketListenerImpl[] connections, ReentrantLock lock,
                              MultiBreak multiBreak) {
        this.stage = stage;
        this.connections = connections;
        this.multiBlocks = multiBlocks;
        this.mainBlock = mainBlock;
        this.lock = lock;
        this.multiBreak = multiBreak;
    }


    @Override
    public void run() {
        if (this.stage != -1 && multiBreak.hasEnded()) return;

        int capacity = Math.max(this.multiBlocks.length - 1, 0);
        List<ClientboundBlockDestructionPacket> packetsToSend = new ArrayList<>(capacity);
        try {
            lock.lock();
            if (this.stage != -1 && multiBreak.hasEnded()) return;

            for (MultiBlock multiBlock : this.multiBlocks) {
                if (!multiBlock.isVisible()) continue;
                if (multiBlock.getBlock().equals(mainBlock)) continue;

                int lastStage = multiBlock.getLastStage();

                // Use predicted stage for comparison and setting
                if ((stage != -1 && lastStage > stage) || lastStage == -1) continue;
                multiBlock.setLastStage(stage);

                BlockPos blockPos = CraftLocation.toBlockPosition(multiBlock.getLocation());
                ClientboundBlockDestructionPacket packet =
                        new ClientboundBlockDestructionPacket(
                                multiBlock.getSourceID(),
                                blockPos,
                                stage  // Send predicted stage
                        );
                packetsToSend.add(packet);
            }
        } finally {
            lock.unlock();
        }

        // Send packets outside lock
        if (!packetsToSend.isEmpty()) {
            for (ServerGamePacketListenerImpl connection : connections) {
                if (connection == null) continue;
                for (ClientboundBlockDestructionPacket packet : packetsToSend) {
                    connection.send(packet);
                }
            }
        }
    }
}
