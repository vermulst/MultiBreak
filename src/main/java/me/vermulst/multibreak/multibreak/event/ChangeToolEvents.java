package me.vermulst.multibreak.multibreak.event;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.vermulst.multibreak.config.Config;
import me.vermulst.multibreak.multibreak.BreakManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.*;

public class ChangeToolEvents implements Listener {

    private final BreakManager breakManager;
    public ChangeToolEvents(BreakManager breakManager) {
        this.breakManager = breakManager;
    }

    /**  Refresh break speed ever tick, checking for new target block */

    @EventHandler
    public void tickEvent(ServerTickEndEvent e) {
        boolean fairMode = Config.getInstance().isFairModeEnabled();
        if (!fairMode) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            breakManager.refreshBreakSpeed(p);
        }
    }

     /** Refresh tool events */

    @EventHandler
    public void pickupIntoHand(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        PlayerInventory inventory = p.getInventory();
        ItemStack[] items = inventory.getContents();
        int heldSlot = inventory.getHeldItemSlot();
        if (items[heldSlot] != null) return;
        int firstEmptySlot = -1;
        for (int i = 0; i < 9; i++) {
            if (items[i] == null) {
                firstEmptySlot = i;
                break;
            }
        }
        if (firstEmptySlot == heldSlot) {
            breakManager.refreshTool(p);
        }
    }

    @EventHandler
    public void emptyHeldSlot(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (p.getInventory().getItemInMainHand().getType() != Material.AIR) return;
        breakManager.refreshTool(p);
    }


    @EventHandler
    public void joinEvent(PlayerJoinEvent e) {
        breakManager.refreshTool(e.getPlayer());
    }

    @EventHandler
    public void hotbarSwap(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        breakManager.refreshTool(p);
    }

    @EventHandler
    public void offhandSwap(PlayerSwapHandItemsEvent e) {
        breakManager.refreshTool(e.getPlayer());
    }

    @EventHandler
    public void heldItemClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;

        // 1. Handle SHIFT_CLICK: Assumes it might affect the held slot by moving an item in.
        if (e.isShiftClick()) {
            shiftClick(e, p);
        }
        // 2. Handle NUMBER_KEY: Player is swapping with a hotbar slot.
        else if (ClickType.NUMBER_KEY.equals(e.getClick())) {
            int playerHeldSlotRelative = p.getInventory().getHeldItemSlot();
            Inventory topInv = e.getInventory();
            // check if held item slot is involved
            if ((e.getHotbarButton() == playerHeldSlotRelative) || (e.getRawSlot() == this.getPlayerHeldSlotRaw(p, topInv))) {
                breakManager.refreshTool(p);
            }
        }
        // 3. Handle DIRECT CLICK: Player clicked directly on their held slot in the GUI.
        else {
            Inventory topInv = e.getInventory();
            int playerHeldSlotRaw = this.getPlayerHeldSlotRaw(p, topInv);
            if (e.getRawSlot() == playerHeldSlotRaw) {
                breakManager.refreshTool(p);
            }
        }
    }


    public void shiftClick(InventoryClickEvent e, Player p) {
        Inventory topInv = e.getInventory();

        boolean refresh = false;
        int playerHeldSlot = p.getInventory().getHeldItemSlot();
        int playerHeldSlotRaw = this.getPlayerHeldSlotRaw(p, topInv);
        int baseHotbarSlotRaw = playerHeldSlotRaw - playerHeldSlot;
        int click = e.getRawSlot();

        // clicking on hotbar, refresh if clicked on held item
        if (click >= baseHotbarSlotRaw && click <= baseHotbarSlotRaw + 8) {
            refresh = (click == playerHeldSlotRaw);
        }
        // clicking on 27 inventory slots, refresh if item goes into held slot
        else if (topInv instanceof CraftingInventory) {
            if (click <= 8 || click == 45) return;
            ItemStack[] items = p.getInventory().getContents();
            int firstEmptySlot = -1;
            for (int i = 0; i < 9; i++) {
                if (items[i] == null) {
                    firstEmptySlot = i;
                    break;
                }
            }
            // this is not guaranteed, since the shifted item could be stacked
            // ,but it's the most performant we can do
            // refreshing takes shorter than checking for stacking items
            // checks if shifted item goes into the player slot
            refresh = firstEmptySlot == playerHeldSlot;
        }
        //
        else {
            int slot = e.getRawSlot();
            int topInventorySize = this.getTopInventorySize(topInv);

            // - we already handle the hotbar case,
            // - nothing changes if player clicks 1 of 27 playerinventory slots,
            // since they can only be forced to the top
            if (slot <= topInventorySize) {
                ItemStack[] items = p.getInventory().getContents();
                int firstEmptySlot = -1;

                // for some reason, when collecting from top inventories,
                // reverse order is used as opposed to 1 of 27 playerinventory slots (within craftinginventory)
                for (int i = 8; i >= 0; i++) {
                    if (items[i] == null) {
                        firstEmptySlot = i;
                        break;
                    }
                }
                refresh = (firstEmptySlot == playerHeldSlot);
            }
        }

        if (refresh) {
            breakManager.refreshTool(p);
        }
    }

    @EventHandler
    public void dragIntoHeldItemEvent(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory topInv = e.getInventory();
        int playerHeldSlotRaw = this.getPlayerHeldSlotRaw(p, topInv);

        if (e.getRawSlots().contains(playerHeldSlotRaw)) {
            breakManager.refreshTool(p);
        }
    }

    private int getPlayerHeldSlotRaw(Player p, Inventory topInv) {
        return this.getTopInventorySize(topInv) + 27 + p.getInventory().getHeldItemSlot();
    }

    private int getTopInventorySize(Inventory topInv) {
        int inventorySize = topInv.getSize();
        if (topInv instanceof CraftingInventory) {
            inventorySize += 4;
        } else if (topInv instanceof CrafterInventory) {
            inventorySize--;
        }
        return inventorySize;
    }
}
