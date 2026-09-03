package com.abohasiba.dragonegg;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Encapsulates all configuration values for the DragonEgg plugin.
 */
public class DragonEggConfig {

    public enum EffectType {
        STRENGTH("strength", 2),
        SPEED("speed", 2),
        RESISTANCE("resistance", 1),
        FIRE_RESISTANCE("fire-resistance", 1),
        HASTE("haste", 2),
        REGENERATION("regeneration", 1),
        WATER_BREATHING("water-breathing", 1);

        private final String configKey;
        private final int defaultLevel;

        EffectType(String configKey, int defaultLevel) {
            this.configKey = configKey;
            this.defaultLevel = defaultLevel;
        }

        public String getConfigKey() {
            return configKey;
        }

        public int getDefaultLevel() {
            return defaultLevel;
        }
    }

    public static class EffectConfig {
        private final boolean enabled;
        private final int configLevel; // 1-based level, e.g. 1 -> Level I, 2 -> Level II
        private final int internalAmplifier; // 0-based for PotionEffect

        public EffectConfig(boolean enabled, int configLevel) {
            this.enabled = enabled;
            this.configLevel = Math.max(1, configLevel);
            this.internalAmplifier = this.configLevel - 1;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getConfigLevel() {
            return configLevel;
        }

        public int getInternalAmplifier() {
            return internalAmplifier;
        }
    }

    private final boolean pluginEnabled;
    private final Map<EffectType, EffectConfig> effects = new EnumMap<>(EffectType.class);
    private final boolean extraHeartsEnabled;
    private final int extraHeartsAmount;
    private final long checkIntervalTicks;
    private final boolean preventBlockPlacement;

    private final String messageGained;
    private final String messageLost;
    private final String messageCannotStore;
    private final String messageReloaded;
    private final String messageNoPermission;

    public DragonEggConfig(FileConfiguration config) {
        this.pluginEnabled = config.getBoolean("dragon-egg.enabled", true);

        // Load configured effects
        for (EffectType type : EffectType.values()) {
            loadEffect(config, type);
        }

        // Extra hearts: 1 heart = 2 HP
        this.extraHeartsEnabled = config.getBoolean("dragon-egg.extra-hearts.enabled", true);
        this.extraHeartsAmount = Math.max(0, config.getInt("dragon-egg.extra-hearts.amount", 10));

        // Check interval ticks
        long interval = config.getLong("dragon-egg.check-interval-ticks", 10L);
        this.checkIntervalTicks = Math.max(1L, interval);

        // Container protection & block placement
        this.preventBlockPlacement = config.getBoolean("dragon-egg.container-protection.prevent-block-placement", true);

        // Messages
        this.messageGained = color(config.getString("messages.gained", "&5🐉 The Dragon Egg empowers you!"));
        this.messageLost = color(config.getString("messages.lost", "&c🐉 The Dragon Egg's power has left you."));
        this.messageCannotStore = color(config.getString("messages.cannot-store", "&cThe Dragon Egg cannot be stored in containers."));
        this.messageReloaded = color(config.getString("messages.reloaded", "&aDragonEgg configuration reloaded."));
        this.messageNoPermission = color(config.getString("messages.no-permission", "&cYou don't have permission to do that."));
    }

    private void loadEffect(FileConfiguration config, EffectType type) {
        String base = "dragon-egg.effects." + type.getConfigKey();
        boolean enabled = config.getBoolean(base + ".enabled", true);
        int level = config.getInt(base + ".amplifier", type.getDefaultLevel());
        effects.put(type, new EffectConfig(enabled, level));
    }

    @SuppressWarnings("deprecation")
    public static String color(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }

    public Map<EffectType, EffectConfig> getEffects() {
        return Collections.unmodifiableMap(effects);
    }

    public EffectConfig getEffectConfig(EffectType type) {
        return effects.get(type);
    }

    public boolean isExtraHeartsEnabled() {
        return extraHeartsEnabled;
    }

    public int getExtraHeartsAmount() {
        return extraHeartsAmount;
    }

    public double getExtraHealthHp() {
        return extraHeartsAmount * 2.0;
    }

    public long getCheckIntervalTicks() {
        return checkIntervalTicks;
    }

    public boolean isPreventBlockPlacement() {
        return preventBlockPlacement;
    }

    public String getMessageGained() {
        return messageGained;
    }

    public String getMessageLost() {
        return messageLost;
    }

    public String getMessageCannotStore() {
        return messageCannotStore;
    }

    public String getMessageReloaded() {
        return messageReloaded;
    }

    public String getMessageNoPermission() {
        return messageNoPermission;
    }
}
