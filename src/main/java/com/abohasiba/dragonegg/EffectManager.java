package com.abohasiba.dragonegg;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages potion effects granted by the Dragon Egg artifact.
 *
 * CRITICAL DESIGN GUARANTEES:
 * 1. Only applies explicitly configured effects (Strength, Speed, Resistance, Fire Resistance, Haste, Regeneration, Water Breathing).
 * 2. Does NOT add Slow Falling or unrequested effects.
 * 3. Uses 1-based level configuration (amplifier: 2 -> Level II, internal amplifier 1).
 * 4. Refreshes effects continuously and smoothly with ambient particles.
 * 5. Protects external potion effects: does not downgrade stronger external effects, and does not wipe out third-party effects when the egg is lost.
 */
public class EffectManager {

    private static final int EFFECT_DURATION_TICKS = 300;

    private static class ExternalEffectBackup {
        final PotionEffectType type;
        final int amplifier;
        final long expiryTimeMillis;

        ExternalEffectBackup(PotionEffect effect) {
            this.type = effect.getType();
            this.amplifier = effect.getAmplifier();
            this.expiryTimeMillis = System.currentTimeMillis() + (effect.getDuration() * 50L);
        }

        boolean isStillValid() {
            return System.currentTimeMillis() < expiryTimeMillis;
        }

        int getRemainingTicks() {
            long remainingMs = expiryTimeMillis - System.currentTimeMillis();
            return (int) Math.max(20L, remainingMs / 50L);
        }
    }

    private final Map<DragonEggConfig.EffectType, PotionEffectType> bukkitTypeMapping = new EnumMap<>(DragonEggConfig.EffectType.class);

    // Map: Player UUID -> Map of PotionEffectType -> ExternalEffectBackup
    private final Map<UUID, Map<PotionEffectType, ExternalEffectBackup>> externalEffectBackups = new ConcurrentHashMap<>();

    // Map: Player UUID -> Map of PotionEffectType -> applied internal amplifier
    private final Map<UUID, Map<PotionEffectType, Integer>> appliedAmplifiers = new ConcurrentHashMap<>();

    public EffectManager() {
        initMappings();
    }

    private void initMappings() {
        registerMapping(DragonEggConfig.EffectType.STRENGTH, "strength");
        registerMapping(DragonEggConfig.EffectType.SPEED, "speed");
        registerMapping(DragonEggConfig.EffectType.RESISTANCE, "resistance");
        registerMapping(DragonEggConfig.EffectType.FIRE_RESISTANCE, "fire_resistance");
        registerMapping(DragonEggConfig.EffectType.HASTE, "haste");
        registerMapping(DragonEggConfig.EffectType.REGENERATION, "regeneration");
        registerMapping(DragonEggConfig.EffectType.WATER_BREATHING, "water_breathing");
    }

    private void registerMapping(DragonEggConfig.EffectType effectType, String key) {
        try {
            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key));
            if (type != null) {
                bukkitTypeMapping.put(effectType, type);
            }
        } catch (Throwable ignored) {
            // Fallback to getByName if Registry not initialized in test environment
            try {
                @SuppressWarnings("deprecation")
                PotionEffectType type = PotionEffectType.getByName(key);
                if (type != null) {
                    bukkitTypeMapping.put(effectType, type);
                }
            } catch (Throwable ignored2) {}
        }
    }

    public PotionEffectType getBukkitEffectType(DragonEggConfig.EffectType effectType) {
        return bukkitTypeMapping.get(effectType);
    }

    /**
     * Continuously applies or refreshes Dragon Egg effects on the carrier.
     */
    public void applyOrRefreshEffects(Player player, DragonEggConfig config) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<PotionEffectType, ExternalEffectBackup> backups =
                externalEffectBackups.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        Map<PotionEffectType, Integer> currentApplied =
                appliedAmplifiers.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        for (Map.Entry<DragonEggConfig.EffectType, DragonEggConfig.EffectConfig> entry : config.getEffects().entrySet()) {
            DragonEggConfig.EffectType effectType = entry.getKey();
            PotionEffectType bukkitType = bukkitTypeMapping.get(effectType);

            if (bukkitType == null) {
                continue;
            }

            DragonEggConfig.EffectConfig effectCfg = entry.getValue();

            if (!effectCfg.isEnabled()) {
                if (currentApplied.containsKey(bukkitType)) {
                    removeSingleEffect(player, bukkitType, currentApplied.get(bukkitType), backups.remove(bukkitType));
                    currentApplied.remove(bukkitType);
                }
                continue;
            }

            int targetAmplifier = effectCfg.getInternalAmplifier();
            PotionEffect existing = player.getPotionEffect(bukkitType);

            if (existing != null) {
                if (existing.getAmplifier() > targetAmplifier) {
                    continue; // Do not downgrade superior external effect
                }

                if (!currentApplied.containsKey(bukkitType) && !backups.containsKey(bukkitType)) {
                    if (existing.getDuration() > EFFECT_DURATION_TICKS) {
                        backups.put(bukkitType, new ExternalEffectBackup(existing));
                    }
                }
            }

            PotionEffect dragonEffect = new PotionEffect(
                    bukkitType,
                    EFFECT_DURATION_TICKS,
                    targetAmplifier,
                    true,  // ambient
                    true,  // particles
                    true   // icon
            );

            player.addPotionEffect(dragonEffect);
            currentApplied.put(bukkitType, targetAmplifier);
        }
    }

    /**
     * Removes all Dragon Egg-managed effects when the player loses the artifact,
     * safely restoring any backed-up third-party potion effects that haven't expired.
     */
    public void removeEffects(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        Map<PotionEffectType, Integer> applied = appliedAmplifiers.remove(uuid);
        Map<PotionEffectType, ExternalEffectBackup> backups = externalEffectBackups.remove(uuid);

        if (applied == null) {
            return;
        }

        for (Map.Entry<PotionEffectType, Integer> entry : applied.entrySet()) {
            PotionEffectType type = entry.getKey();
            int appliedAmp = entry.getValue();
            ExternalEffectBackup backup = (backups != null) ? backups.get(type) : null;

            removeSingleEffect(player, type, appliedAmp, backup);
        }
    }

    private void removeSingleEffect(Player player, PotionEffectType type, int appliedAmp, ExternalEffectBackup backup) {
        PotionEffect current = player.getPotionEffect(type);
        if (current == null) {
            return;
        }

        if (current.getAmplifier() == appliedAmp) {
            player.removePotionEffect(type);

            if (backup != null && backup.isStillValid()) {
                PotionEffect restored = new PotionEffect(
                        backup.type,
                        backup.getRemainingTicks(),
                        backup.amplifier,
                        false,
                        true,
                        true
                );
                player.addPotionEffect(restored);
            }
        }
    }

    public void cleanupPlayer(UUID uuid) {
        appliedAmplifiers.remove(uuid);
        externalEffectBackups.remove(uuid);
    }
}
