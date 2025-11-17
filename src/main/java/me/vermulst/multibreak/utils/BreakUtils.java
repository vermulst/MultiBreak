package me.vermulst.multibreak.utils;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.multibreak.MultiBreak;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.logging.Level;


public class BreakUtils {

    private static int getRange(Player p) {
        double range = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE).getValue();;
        return (int) Math.ceil(range);
    }

    public static BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(getRange(p));
    }

    public static Block getTargetBlock(Player p) {
        return p.getTargetBlockExact(getRange(p));
    }

    public static RayTraceResult getRayTraceResult(Player p) {
        return p.rayTraceBlocks(getRange(p));
    }

    public static float getDestroySpeed(Player p, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeedMain(p);
    }

    public static float getDestroySpeed(ServerPlayer serverPlayer, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeedMain(serverPlayer);
    }

    public static float getDestroySpeed(ServerPlayer serverPlayer, BlockPos blockPos, MultiBreak multiBreak) {
        return multiBreak.getDestroySpeed(serverPlayer, blockPos);
    }



    public boolean destroyBlock(ServerPlayer serverPlayer, BlockPos pos) {
        ServerLevel serverLevel = serverPlayer.level();
        ServerPlayerGameMode gameMode = serverPlayer.gameMode;
        BlockState blockState = serverLevel.getBlockState(pos);
        Block bblock = CraftBlock.at(serverLevel, pos);
        BlockBreakEvent event = null;


        boolean canAttackBlock = !serverPlayer.getMainHandItem().canDestroyBlock(blockState, serverLevel, pos, serverPlayer);
        event = new BlockBreakEvent(bblock, serverPlayer.getBukkitEntity());
        event.setCancelled(canAttackBlock);
        BlockState updatedBlockState = serverLevel.getBlockState(pos);
        net.minecraft.world.level.block.Block block = updatedBlockState.getBlock();
        if (!event.isCancelled() && !serverPlayer.isCreative() && serverPlayer.hasCorrectToolForDrops(block.defaultBlockState())) {
            ItemStack itemInHand = serverPlayer.getItemBySlot(EquipmentSlot.MAINHAND);
            event.setExpToDrop(block.getExpDrop(updatedBlockState, serverLevel, pos, itemInHand, true));
        }

        serverLevel.getCraftServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (canAttackBlock) {
                return false;
            }

            if (!gameMode.captureSentBlockEntities) {
                BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
                if (blockEntity != null) {
                    serverPlayer.connection.send(blockEntity.getUpdatePacket());
                }
            } else {
                gameMode.capturedBlockEntity = true;
            }

            return false;
        }

        blockState = serverLevel.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        } else {
            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
            block = blockState.getBlock();
            if (!(block instanceof GameMasterBlock) || serverPlayer.canUseGameMasterBlocks() || block instanceof CommandBlock && gameMode.isCreative() && serverPlayer.getBukkitEntity().hasPermission("minecraft.commandblock")) {
                if (serverPlayer.blockActionRestricted(serverLevel, pos, gameMode.getGameModeForPlayer())) {
                    return false;
                } else {
                    org.bukkit.block.BlockState state = bblock.getState();
                    serverLevel.captureDrops = new ArrayList<>();
                    BlockState blockState1 = block.playerWillDestroy(serverLevel, pos, blockState, serverPlayer);
                    boolean flag = serverLevel.removeBlock(pos, false);
                    if (SharedConstants.DEBUG_BLOCK_BREAK) {
                        Main.getInstance().getLogger().log(Level.SEVERE, "server broke {} {} -> {}", new Object[]{pos, blockState1, serverLevel.getBlockState(pos)});
                    }

                    if (flag) {
                        block.destroy(serverLevel, pos, blockState1);
                    }

                    ItemStack mainHandStack = null;
                    boolean isCorrectTool = false;
                    if (!serverPlayer.preventsBlockDrops()) {
                        ItemStack mainHandItem = serverPlayer.getMainHandItem();
                        ItemStack itemStack = mainHandItem.copy();
                        boolean hasCorrectToolForDrops = serverPlayer.hasCorrectToolForDrops(blockState1);
                        mainHandStack = itemStack;
                        isCorrectTool = hasCorrectToolForDrops;
                        mainHandItem.mineBlock(serverLevel, blockState1, pos, serverPlayer);
                        if (flag && hasCorrectToolForDrops) {
                            block.playerDestroy(serverLevel, serverPlayer, pos, blockState1, blockEntity, itemStack, event.isDropItems(), false);
                        }
                    }

                    List<ItemEntity> itemsToDrop = serverLevel.captureDrops;
                    serverLevel.captureDrops = null;
                    if (event.isDropItems()) {
                        CraftEventFactory.handleBlockDropItemEvent(bblock, state, serverPlayer, itemsToDrop);
                    }

                    if (flag) {
                        blockState.getBlock().popExperience(serverLevel, pos, event.getExpToDrop(), serverPlayer);
                    }

                    if (mainHandStack != null && flag && isCorrectTool && event.isDropItems() && block instanceof BeehiveBlock && blockEntity instanceof BeehiveBlockEntity) {
                        BeehiveBlockEntity beehiveBlockEntity = (BeehiveBlockEntity) blockEntity;
                        CriteriaTriggers.BEE_NEST_DESTROYED.trigger(serverPlayer, blockState, mainHandStack, beehiveBlockEntity.getOccupantCount());
                    }

                    return true;
                }
            } else {
                serverLevel.sendBlockUpdated(pos, blockState, blockState, 3);
                return false;
            }
        }
    }


    public Set<BlockPos> destroyBlocks(ServerPlayer serverPlayer, Collection<BlockPos> positions) {
        ServerLevel serverLevel = serverPlayer.level();
        ServerPlayerGameMode gameMode = serverPlayer.gameMode;
        ItemStack itemInHand = serverPlayer.getItemBySlot(EquipmentSlot.MAINHAND);

        Set<BlockPos> successfullyDestroyedBlocks = new HashSet<>();
        for (BlockPos pos : positions) {

            // event setup
            BlockState blockState = serverLevel.getBlockState(pos);
            Block bblock = CraftBlock.at(serverLevel, pos);
            BlockBreakEvent event = new BlockBreakEvent(bblock, serverPlayer.getBukkitEntity());
            boolean canAttackBlock = !serverPlayer.getMainHandItem().canDestroyBlock(blockState, serverLevel, pos, serverPlayer);
            event.setCancelled(canAttackBlock);


            BlockState updatedBlockState = serverLevel.getBlockState(pos);
            net.minecraft.world.level.block.Block block = updatedBlockState.getBlock();

            // xp calculation
            if (!event.isCancelled() && !serverPlayer.isCreative() && serverPlayer.hasCorrectToolForDrops(block.defaultBlockState())) {
                event.setExpToDrop(block.getExpDrop(updatedBlockState, serverLevel, pos, itemInHand, true));
            }

            // calling event
            serverLevel.getCraftServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                if (canAttackBlock) {
                    continue;
                }

                if (!gameMode.captureSentBlockEntities) {
                    BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
                    if (blockEntity != null) {
                        serverPlayer.connection.send(blockEntity.getUpdatePacket());
                    }
                } else {
                    gameMode.capturedBlockEntity = true;
                }

                continue;
            }


            blockState = serverLevel.getBlockState(pos);
            if (blockState.isAir()) continue;

            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
            block = blockState.getBlock();
            if (!(block instanceof GameMasterBlock) || serverPlayer.canUseGameMasterBlocks() || block instanceof CommandBlock && gameMode.isCreative() && serverPlayer.getBukkitEntity().hasPermission("minecraft.commandblock")) {
                if (serverPlayer.blockActionRestricted(serverLevel, pos, gameMode.getGameModeForPlayer())) continue;


                org.bukkit.block.BlockState state = bblock.getState();
                serverLevel.captureDrops = new ArrayList<>();

                // remove actual block
                BlockState blockState1 = block.playerWillDestroy(serverLevel, pos, blockState, serverPlayer);
                boolean flag = serverLevel.removeBlock(pos, false);
                if (SharedConstants.DEBUG_BLOCK_BREAK) {
                    Main.getInstance().getLogger().log(Level.SEVERE, "server broke {} {} -> {}", new Object[]{pos, blockState1, serverLevel.getBlockState(pos)});
                }

                // checks for adjacent blocks (e.g. torches)
                if (flag) {
                    block.destroy(serverLevel, pos, blockState1);
                }

                // initiate drops
                ItemStack mainHandStack = null;
                boolean isCorrectTool = false;
                if (!serverPlayer.preventsBlockDrops()) {
                    ItemStack mainHandItem = serverPlayer.getMainHandItem();
                    ItemStack itemStack = mainHandItem.copy();
                    boolean hasCorrectToolForDrops = serverPlayer.hasCorrectToolForDrops(blockState1);
                    mainHandStack = itemStack;
                    isCorrectTool = hasCorrectToolForDrops;
                    mainHandItem.mineBlock(serverLevel, blockState1, pos, serverPlayer);
                    if (flag && hasCorrectToolForDrops) {
                        block.playerDestroy(serverLevel, serverPlayer, pos, blockState1, blockEntity, itemStack, event.isDropItems(), false);
                    }
                }

                // handle drops
                List<ItemEntity> itemsToDrop = serverLevel.captureDrops;
                serverLevel.captureDrops = null;
                if (event.isDropItems()) {
                    CraftEventFactory.handleBlockDropItemEvent(bblock, state, serverPlayer, itemsToDrop);
                }

                if (flag) {
                    blockState.getBlock().popExperience(serverLevel, pos, event.getExpToDrop(), serverPlayer);
                }

                if (mainHandStack != null && flag && isCorrectTool && event.isDropItems() && block instanceof BeehiveBlock && blockEntity instanceof BeehiveBlockEntity) {
                    BeehiveBlockEntity beehiveBlockEntity = (BeehiveBlockEntity) blockEntity;
                    CriteriaTriggers.BEE_NEST_DESTROYED.trigger(serverPlayer, blockState, mainHandStack, beehiveBlockEntity.getOccupantCount());
                }

                successfullyDestroyedBlocks.add(pos);
            } else {
                serverLevel.sendBlockUpdated(pos, blockState, blockState, 3);
            }
        }
        return successfullyDestroyedBlocks;
    }

}
