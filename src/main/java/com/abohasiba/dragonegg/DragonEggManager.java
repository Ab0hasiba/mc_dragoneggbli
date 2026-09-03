package com.abohasiba.dragonegg;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Core coordinator managing Dragon Egg carrier tracking, transitions, and power synchronization.
 */
public class DragonEggManager {

    private final DragonEgg plugin;
    private final HealthManager healthManager;
    private final EffectManager effectManager;
    private volatile DragonEggConfig config;

    private final AtomicReference<UUID> currentCarrierUuid = new AtomicReference<>(null);

    public DragonEggManager(DragonEgg plugin, HealthManager healthManager, EffectManager effectManager, DragonEggConfig config) {
        this.plugin = plugin;
        this.healthManager = healthManager;
        this.effectManager = effectManager;
        this.config = config;
    }

    public DragonEggConfig getConfig() {
        return config;
    }

    public void setConfig(DragonEggConfig newConfig) {
        this.config = newConfig;
        // Refresh active carrier immediately if present
        UUID carrierUuid = currentCarrierUuid.get();
        if (carrierUuid != null) {
            Player carrier = Bukkit.getPlayer(carrierUuid);
            if (carrier != null && carrier.isOnline() && isDirectlyCarryingEgg(carrier)) {
                healthManager.applyHealthBonus(carrier, config);
                effectManager.applyOrRefreshEffects(carrier, config);
            }
        }
    }

    public UUID getCurrentCarrierUuid() {
        return currentCarrierUuid.get();
    }

    /**
     * Verifies whether an item is the genuine vanilla Dragon Egg.
     * Rejects null, non-DRAGON_EGG items, and renamed / fake items.
     */
    public static boolean isGenuineDragonEgg(ItemStack item) {
        if (item == null || item.getType() != Material.DRAGON_EGG || item.getAmount() <= 0) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Reject renamed or fake items
            if (meta.hasDisplayName()) {
                return false;
            }
            if (meta.hasLore()) {
                return false;
            }
            if (meta.hasCustomModelData()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks if the player is physically carrying the genuine Dragon Egg in their:
     * - Main inventory / storage slots
     * - Hotbar
     * - Offhand
     */
    public boolean isDirectlyCarryingEgg(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        PlayerInventory inv = player.getInventory();

        // Check offhand
        if (isGenuineDragonEgg(inv.getItemInOffHand())) {
            return true;
        }

        // Check main inventory and hotbar (storage contents)
        ItemStack[] storage = inv.getStorageContents();
        for (ItemStack item : storage) {
            if (isGenuineDragonEgg(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Immediate update of a player's carrier state (e.g. on inventory click, pickup, drop).
     */
    public synchronized void updatePlayerCarrierStatus(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        boolean carrying = isDirectlyCarryingEgg(player);
        UUID uuid = player.getUniqueId();
        UUID currentCarrier = currentCarrierUuid.get();

        if (carrying) {
            if (!uuid.equals(currentCarrier)) {
                // If someone else was the carrier, revoke their powers first
                if (currentCarrier != null) {
                    Player oldCarrier = Bukkit.getPlayer(currentCarrier);
                    if (oldCarrier != null && oldCarrier.isOnline()) {
                        revokePowers(oldCarrier);
                    }
                }

                currentCarrierUuid.set(uuid);
                grantPowers(player);
            } else {
                // Refresh existing carrier
                healthManager.applyHealthBonus(player, config);
                effectManager.applyOrRefreshEffects(player, config);
            }
        } else {
            if (uuid.equals(currentCarrier)) {
                currentCarrierUuid.set(null);
                revokePowers(player);
            }
        }
    }

    /**
     * Scheduled safety check: scans all online players and synchronizes the carrier state.
     */
    public synchronized void scanAllOnlinePlayers() {
        if (!config.isPluginEnabled()) {
            // If plugin disabled in config, clear any active powers
            UUID carrierUuid = currentCarrierUuid.getAndSet(null);
            if (carrierUuid != null) {
                Player carrier = Bukkit.getPlayer(carrierUuid);
                if (carrier != null && carrier.isOnline()) {
                    revokePowers(carrier);
                }
            }
            return;
        }

        Player foundCarrier = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (isDirectlyCarryingEgg(online)) {
                foundCarrier = online;
                break;
            }
        }

        UUID previousCarrierUuid = currentCarrierUuid.get();

        if (foundCarrier != null) {
            UUID foundUuid = foundCarrier.getUniqueId();
            if (!foundUuid.equals(previousCarrierUuid)) {
                // Carrier changed
                if (previousCarrierUuid != null) {
                    Player oldCarrier = Bukkit.getPlayer(previousCarrierUuid);
                    if (oldCarrier != null && oldCarrier.isOnline()) {
                        revokePowers(oldCarrier);
                    }
                }
                currentCarrierUuid.set(foundUuid);
                grantPowers(foundCarrier);
            } else {
                // Carrier remains the same; safely refresh powers
                healthManager.applyHealthBonus(foundCarrier, config);
                effectManager.applyOrRefreshEffects(foundCarrier, config);
            }
        } else {
            // No online player is carrying the egg
            if (previousCarrierUuid != null) {
                currentCarrierUuid.set(null);
                Player oldCarrier = Bukkit.getPlayer(previousCarrierUuid);
                if (oldCarrier != null && oldCarrier.isOnline()) {
                    revokePowers(oldCarrier);
                }
            }
        }
    }

    private void grantPowers(Player player) {
        healthManager.applyHealthBonus(player, config);
        effectManager.applyOrRefreshEffects(player, config);

        String msg = config.getMessageGained();
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    private void revokePowers(Player player) {
        healthManager.removeHealthBonus(player);
        effectManager.removeEffects(player);

        String msg = config.getMessageLost();
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(msg);
        }
    }

    public synchronized void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        if (uuid.equals(currentCarrierUuid.get())) {
            currentCarrierUuid.set(null);
            revokePowers(player);
        }
        effectManager.cleanupPlayer(uuid);
    }

    public synchronized void handlePlayerDeath(Player player) {
        UUID uuid = player.getUniqueId();
        if (uuid.equals(currentCarrierUuid.get())) {
            currentCarrierUuid.set(null);
            revokePowers(player);
        }
    }
}
