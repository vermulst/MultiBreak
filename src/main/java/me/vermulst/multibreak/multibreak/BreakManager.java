package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.item.FigureItemInfo;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
public class BreakManager implements Listener {

    private final Main plugin;

    private final HashMap<UUID, MultiBreak> multiBlockHashMap = new HashMap<>();

    public BreakManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void armSwingEvent(PlayerAnimationEvent e) {
        if (!e.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;
        Player p = e.getPlayer();
        if (p.getGameMode().equals(GameMode.CREATIVE)) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        Figure figure = this.getFigure(tool);
        if (figure == null) return;

        BlockFace blockFace = this.getBlockFace(p);
        Block blockMining = this.getTargetBlock(p);
        MultiBreak multiBlock = multiBlockHashMap.computeIfAbsent(p.getUniqueId(),
                b -> new MultiBreak(p, blockMining, figure, blockFace.getDirection()));
        multiBlock.tick(plugin, blockMining);
        if (multiBlock.hasEnded()) {
            multiBlock = new MultiBreak(p, blockMining, figure, blockFace.getDirection());
            multiBlockHashMap.put(p.getUniqueId(), multiBlock);
            multiBlock.tick(plugin, blockMining);
        }
    }

    @EventHandler
    public void breakBlockType(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (e.isCancelled() || p.getGameMode().equals(GameMode.CREATIVE)) return;
        ItemStack tool = p.getInventory().getItemInMainHand();
        Figure figure = this.getFigure(tool);
        if (figure == null) return;

        BlockFace blockFace = this.getBlockFace(p);
        MultiBreak multiBlock = multiBlockHashMap.computeIfAbsent(p.getUniqueId(),
                b -> new MultiBreak(p, getTargetBlock(p), figure, blockFace.getDirection()));
        multiBlock.end(true);
        multiBlockHashMap.remove(p.getUniqueId());
    }

    public Figure getFigure(ItemStack tool) {
        Figure figure = null;
        if (tool.getItemMeta() == null) return null;
        FigureItemInfo figureItemInfo = this.getFigureItemInfo(tool);
        if (figureItemInfo == null) {
            Material material = tool.getType();
            ConfigManager configManager = this.plugin.getConfigManager();
            if (configManager.getMaterialOptions().containsKey(material)) {
                String configOptionName = configManager.getMaterialOptions().get(material);
                figure = configManager.getConfigOptions().get(configOptionName);
            }
        } else {
            figure = figureItemInfo.figure();
        }
        return figure;
    }

    public FigureItemInfo getFigureItemInfo(ItemStack item) {
        FigureItemDataType figureItemDataType = new FigureItemDataType(this.plugin);
        return figureItemDataType.get(item);
    }

    public Block getTargetBlock(Player p) {
        return p.getLastTwoTargetBlocks(null, 10).get(1);
    }

    public BlockFace getBlockFace(Player p) {
        List<Block> targetBlocks = p.getLastTwoTargetBlocks(null, 10);
        Block mainBlock = targetBlocks.get(1);
        Block adjacentBlock = targetBlocks.get(0);
        return mainBlock.getFace(adjacentBlock);
    }

}
