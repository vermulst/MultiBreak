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

    private final int stage;
    private final List<Player> players;
    private final List<MultiBlock> multiBlocks;
    private final Block mainBlock;

    private final UUID uuid;

    private static final Map<UUID, ReentrantLock> stageLock = new HashMap<>();

    public WriteStageRunnable(MultiBreak multiBreak, int stage, List<Player> players) {
        this.multiBlocks = multiBreak.getMultiBlocks();
        this.mainBlock = multiBreak.getBlock();
        this.stage = stage;
        this.players = players;

        this.uuid = multiBreak.getPlayer().getUniqueId();
    }

    @Override
    public void run() {
        if (!stageLock.containsKey(uuid)) {
            stageLock.put(uuid, new ReentrantLock());
        }
        ReentrantLock lock = stageLock.get(uuid);
        List<ClientboundBlockDestructionPacket> packetsToSend = new ArrayList<>();
        try {
            lock.lock();
            for (MultiBlock multiBlock : this.multiBlocks) {
                if (!multiBlock.isVisible()) continue;
                if (multiBlock.getBlock().equals(mainBlock)) continue;

                int lastStage = multiBlock.getLastStage();
                if ((this.stage != -1 && lastStage > this.stage) || lastStage == -1) continue;
                multiBlock.setLastStage(stage);

                BlockPos blockPos = CraftLocation.toBlockPosition(multiBlock.getBlock().getLocation());
                ClientboundBlockDestructionPacket packet =
                        new ClientboundBlockDestructionPacket(
                                multiBlock.getSourceID(),
                                blockPos,
                                stage
                        );
                packetsToSend.add(packet);
            }
            for (Player p : players) {
                ServerGamePacketListenerImpl connection = ((CraftPlayer) p).getHandle().connection;
                for (ClientboundBlockDestructionPacket packet : packetsToSend) {
                    connection.send(packet);
                }
            }
        } finally {
            lock.unlock();
        }
    }
}
