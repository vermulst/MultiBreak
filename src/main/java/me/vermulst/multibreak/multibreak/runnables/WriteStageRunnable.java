package me.vermulst.multibreak.multibreak.runnables;

import me.vermulst.multibreak.multibreak.MultiBlock;
import me.vermulst.multibreak.multibreak.MultiBreak;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class WriteStageRunnable extends BukkitRunnable {

    private final UUID uuid;
    private final int stage;
    private final List<Player> players;
    private final List<MultiBlock> multiBlocks;
    private final Block mainBlock;
    private final ReentrantLock lock;


    public WriteStageRunnable(UUID uuid, List<MultiBlock> multiBlocks, Block mainBlock, int stage, List<Player> players, ReentrantLock lock) {
        this.uuid = uuid;
        this.stage = stage;
        this.players = players;
        this.multiBlocks = multiBlocks;
        this.mainBlock = mainBlock;
        this.lock = lock;
    }

    @Override
    public void run() {
        List<ClientboundBlockDestructionPacket> packetsToSend = new ArrayList<>();
        try {
            lock.lock();

            for (MultiBlock multiBlock : this.multiBlocks) {
                if (!multiBlock.isVisible()) continue;
                if (multiBlock.getBlock().equals(mainBlock)) continue;

                int lastStage = multiBlock.getLastStage();
                if ((this.stage != -1 && lastStage > this.stage) || lastStage == -1) continue;
                multiBlock.setLastStage(stage);

                BlockPos blockPos = CraftLocation.toBlockPosition(multiBlock.getLocation());
                ClientboundBlockDestructionPacket packet =
                        new ClientboundBlockDestructionPacket(
                                multiBlock.getSourceID(),
                                blockPos,
                                stage
                        );
                packetsToSend.add(packet);
            }
            if (!packetsToSend.isEmpty()) {
                for (Player p : players) {
                    ServerGamePacketListenerImpl connection = ((CraftPlayer) p).getHandle().connection;
                    for (ClientboundBlockDestructionPacket packet : packetsToSend) {
                        connection.send(packet);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
