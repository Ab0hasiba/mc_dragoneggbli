package com.abohasiba.dragonegg.listener;

import com.abohasiba.dragonegg.DragonEgg;
import com.abohasiba.dragonegg.DragonEggConfig;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

/**
 * Strictly prevents the Dragon Egg artifact from being stored inside ANY container,
 * external inventory, plugin storage, bundle, decorated pot, or entity storage.
 */
public class ContainerProtectionListener implements Listener {

    private final DragonEgg plugin;

    public ContainerProtectionListener(DragonEgg plugin) {
        this.plugin = plugin;
    }

    private boolean isDragonEgg(ItemStack item) {
        return item != null && item.getType() == Material.DRAGON_EGG;
    }

    private boolean isBundle(ItemStack item) {
        if (item == null) return false;
        Material mat = item.getType();
        return mat == Material.BUNDLE || mat.name().endsWith("_BUNDLE");
    }

    private void notifyBlocked(Player player) {
        String msg = plugin.getConfiguration().getMessageCannotStore();
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    /**
     * Blocks inventory click exploits:
     * - Moving egg into external containers (chests, barrels, shulkers, hoppers, plugin GUIs)
     * - Shift-clicking into external containers
     * - Number-key hotbar swap into external containers
     * - Offhand swap into external containers
     * - Double-click collecting into containers
     * - Inserting into Bundles
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        boolean isTopExternalContainer = topInventory.getType() != InventoryType.CRAFTING;

        // 1. Check Bundle interactions (in ANY inventory)
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if ((isDragonEgg(cursor) && isBundle(current)) || (isBundle(cursor) && isDragonEgg(current))) {
            event.setCancelled(true);
            notifyBlocked(player);
            return;
        }

        // 2. Direct click into top external inventory with Dragon Egg on cursor
        if (clickedInventory != null && clickedInventory == topInventory && isTopExternalContainer) {
            if (isDragonEgg(cursor)) {
                event.setCancelled(true);
                notifyBlocked(player);
                return;
            }

            // Number key swap from hotbar into container slot
            if (event.getClick() == ClickType.NUMBER_KEY) {
                int hotbarSlot = event.getHotbarButton();
                if (hotbarSlot >= 0 && hotbarSlot < 9) {
                    ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
                    if (isDragonEgg(hotbarItem) || isDragonEgg(current)) {
                        event.setCancelled(true);
                        notifyBlocked(player);
                        return;
                    }
                }
            }

            // Offhand swap into container slot
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (isDragonEgg(offhand) || isDragonEgg(current)) {
                    event.setCancelled(true);
                    notifyBlocked(player);
                    return;
                }
            }
        }

        // 3. Shift-clicking Dragon Egg from player inventory into external container
        if (event.isShiftClick() && isTopExternalContainer) {
            if (clickedInventory != null && clickedInventory == player.getInventory()) {
                if (isDragonEgg(current)) {
                    event.setCancelled(true);
                    notifyBlocked(player);
                    return;
                }
            }
        }

        // 4. Double-click collection across container inventories
        if (event.getClick() == ClickType.DOUBLE_CLICK && isTopExternalContainer) {
            if (isDragonEgg(cursor)) {
                event.setCancelled(true);
                notifyBlocked(player);
                return;
            }
        }

        // 5. Additional safety for Collect-to-cursor actions
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR && isTopExternalContainer) {
            if (isDragonEgg(cursor)) {
                event.setCancelled(true);
                notifyBlocked(player);
            }
        }
    }

    /**
     * Blocks dragging Dragon Egg into external container slots.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!isDragonEgg(event.getOldCursor())) {
            return;
        }

        InventoryView view = event.getView();
        Inventory top = view.getTopInventory();

        if (top.getType() == InventoryType.CRAFTING) {
            // Player's 2x2 survival crafting grid is fine
            return;
        }

        int topSize = top.getSize();
        Set<Integer> rawSlots = event.getRawSlots();

        for (int slot : rawSlots) {
            if (slot < topSize) {
                // Drag target touches external container inventory
                event.setCancelled(true);
                notifyBlocked(player);
                return;
            }
        }
    }

    /**
     * Prevents hoppers, droppers, and hopper minecarts from moving the Dragon Egg.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isDragonEgg(event.getItem())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents hoppers and hopper minecarts from picking up the Dragon Egg from the ground.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (isDragonEgg(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Blocks right-clicking blocks that store items without opening an inventory GUI:
     * - Decorated Pots
     * - Chiseled Bookshelves
     * - Jukeboxes
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        ItemStack item = event.getItem();
        if (!isDragonEgg(item)) {
            return;
        }

        Material blockType = event.getClickedBlock().getType();

        if (blockType == Material.DECORATED_POT
                || blockType == Material.CHISELED_BOOKSHELF
                || blockType == Material.JUKEBOX) {
            event.setCancelled(true);
            notifyBlocked(event.getPlayer());
        }
    }

    /**
     * Prevents placing the Dragon Egg as a block (if configured)
     * so it remains strictly an inventory artifact or dropped item entity.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() == Material.DRAGON_EGG) {
            DragonEggConfig config = plugin.getConfiguration();
            if (config.isPreventBlockPlacement()) {
                event.setCancelled(true);
                notifyBlocked(event.getPlayer());
            }
        }
    }

    /**
     * Blocks inserting the Dragon Egg into entity storage:
     * - Item Frames & Glow Item Frames
     * - Armor Stands
     * - Storage & Hopper Minecarts
     * - Chested Horses / Donkeys / Mules / Llamas
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (!isDragonEgg(mainHand) && !isDragonEgg(offHand)) {
            return;
        }

        Entity target = event.getRightClicked();

        if (target instanceof ItemFrame
                || target instanceof ArmorStand
                || target instanceof StorageMinecart
                || target instanceof HopperMinecart
                || target instanceof ChestedHorse) {
            event.setCancelled(true);
            notifyBlocked(player);
        }
    }
}
