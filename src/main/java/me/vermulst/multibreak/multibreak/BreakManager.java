package me.vermulst.multibreak.multibreak;

import me.vermulst.multibreak.Main;
import me.vermulst.multibreak.config.ConfigManager;
import me.vermulst.multibreak.figure.Figure;
import me.vermulst.multibreak.item.FigureItemDataType;
import me.vermulst.multibreak.api.event.MultiBreakEndEvent;
import me.vermulst.multibreak.api.event.MultiBreakStartEvent;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class BreakManager implements Listener {

    private final Main plugin;

    private final Map<UUID, Integer> multiBreakTask = new HashMap<>();
    private final Map<UUID, MultiBreak> multiBlockMap = new HashMap<>();

    public BreakManager(Main plugin) {
        this.plugin = plugin;
    }

    @Deprecated
    @EventHandler(priority = EventPriority.HIGHEST)
    public void armSwingEvent(PlayerAnimationEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[1];
        if (!legacy_mode) return;
        if (!e.getAnimationType().equals(PlayerAnimationType.ARM_SWING)) return;
        Player p = e.getPlayer();
        if (p.getGameMode().equals(GameMode.CREATIVE)) return;
        MultiBreak multiBreak = getMultiBreak(p);
        if (multiBreak == null) return;
        Block blockMining = this.getTargetBlock(p);
        if (blockMining == null) return;
        multiBreak.tick(this.getPlugin(), blockMining);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStart(BlockDamageEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[1];
        if (legacy_mode) return;
        this.scheduleMultiBreak(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void multiBreakStop(BlockDamageAbortEvent e) {
        boolean legacy_mode = plugin.getConfigManager().getOptions()[1];
        if (legacy_mode) return;
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getMultiBreak(p);
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, false);
        event.callEvent();
        if (multiBreak == null) return;
        this.endMultiBreak(p, multiBreak, false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void breakBlockType(BlockBreakEvent e) {
        if (this.ignoreMultiBreak(e)) return;
        Player p = e.getPlayer();
        MultiBreak multiBreak = this.getMultiBreak(p);
        Block block = e.getBlock();
        // Mismatch
        if (!block.equals(multiBreak.getBlock())) {
            multiBreak = this.getMultiBreak(p, block);
        }
        MultiBreakEndEvent event = new MultiBreakEndEvent(p, multiBreak, true);
        event.callEvent();
        if (event.isCancelled()) return;
        if (event.getMultiBreak() == null) return;
        this.endMultiBreak(p, event.getMultiBreak(), true);
    }

    private boolean ignoreMultiBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        boolean b1 = block.hasMetadata("multi-broken");
        boolean b2 = plugin.getConfigManager().getIgnoredMaterials().contains(block.getType());
        boolean b3 = e.isCancelled();
        if (block.hasMetadata("multi-broken")) {
            block.removeMetadata("multi-broken", getPlugin());
        }
        return b1 || b2 || b3;
    }


    public void scheduleMultiBreak(Player p) {
        int taskID = new BukkitRunnable() {
            @Override
            public void run() {
                MultiBreak multiBreak = getMultiBreak(p);
                if (multiBreak == null) return;
                multiBreak.tick();
            }
        }.runTaskTimer(getPlugin(), 1, 1).getTaskId();
        getMultiBreakTask().put(p.getUniqueId(), taskID);
    }

    public void endMultiBreak(Player p, MultiBreak multiBreak, boolean finished) {
        UUID uuid = p.getUniqueId();
        multiBreak.end(finished, getPlugin());
        this.getMultiBlockMap().remove(uuid);
        if (!this.getMultiBreakTask().containsKey(uuid)) return;
        Bukkit.getScheduler().cancelTask(this.getMultiBreakTask().get(uuid));
    }

    public MultiBreak getMultiBreak(Player p) {
        if (p.getGameMode().equals(GameMode.CREATIVE)) return null;
        if (multiBlockMap.containsKey(p.getUniqueId())) {
            MultiBreak multiBreak = multiBlockMap.get(p.getUniqueId());
            if (!multiBreak.hasEnded()) {
                return multiBreak;
            }
        }
        Block blockMining = this.getTargetBlock(p);
        return this.getMultiBreak(p, blockMining);
    }

    public MultiBreak getMultiBreak(Player p, Block block) {
        ItemStack tool = p.getInventory().getItemInMainHand();
        Figure figure = this.getFigure(tool);
        BlockFace blockFace = this.getBlockFace(p);
        if (block == null || blockFace == null) return null;
        ConfigManager config = plugin.getConfigManager();
        boolean fair_mode = config.getOptions()[0];
        EnumSet<Material> ignoredMaterials = config.getIgnoredMaterials();
        MultiBreak multiBreak = new MultiBreak(p, block, figure, blockFace.getDirection(), fair_mode, ignoredMaterials);
        MultiBreakStartEvent event = new MultiBreakStartEvent(p, multiBreak, block, blockFace.getDirection(), fair_mode, ignoredMaterials);
        if (!event.callEvent()) return null;
        MultiBreak multiBreak1 = event.getMultiBreak();
        multiBlockMap.put(p.getUniqueId(), multiBreak1);
        return multiBreak1;
    }

    public Figure getFigure(ItemStack tool) {
        if (tool.getItemMeta() == null) return null;
        FigureItemDataType figureItemDataType = new FigureItemDataType(this.getPlugin());
        Figure figure = figureItemDataType.get(tool);
        if (figure != null) return figure;

        // Fallback on material figures
        Material material = tool.getType();
        ConfigManager configManager = this.getPlugin().getConfigManager();
        if (configManager.getMaterialOptions().containsKey(material)) {
            String configOptionName = configManager.getMaterialOptions().get(material);
            return configManager.getConfigOptions().get(configOptionName);
        }
        return null;
    }

    public Block getTargetBlock(Player p) {
        return p.getTargetBlockExact(plugin.getConfigManager().getMaxRange());
    }

    public BlockFace getBlockFace(Player p) {
        return p.getTargetBlockFace(plugin.getConfigManager().getMaxRange());
    }

    public Block getTargetBlockManual(Player p) {
        int maxRange = plugin.getConfigManager().getMaxRange();
        Location loc = p.getEyeLocation();
        Vector direction = loc.getDirection();
        double step = 0.05; // Small step for higher precision

        // Iterate small steps along the direction vector
        for (double i = 0; i < maxRange; i += step) {
            Vector dirClone = direction.clone();
            dirClone.multiply(i);
            Location checkLoc = loc.clone().add(dirClone);
            Block block = checkLoc.getBlock();

            // Check if the block is solid (not air, not liquid)
            if (block.getType().isSolid() && block.getType() != Material.WATER && block.getType() != Material.LAVA) {
                return block; // Return the first solid block found
            }
        }
        return null; // If nothing is hit within max range
    }

    public Main getPlugin() {
        return plugin;
    }

    public Map<UUID, Integer> getMultiBreakTask() {
        return multiBreakTask;
    }

    public Map<UUID, MultiBreak> getMultiBlockMap() {
        return multiBlockMap;
    }
}
