package com.abohasiba.dragonegg;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class DragonEggConfigTest {

    private static final String DEFAULT_CONFIG = """
            dragon-egg:
              enabled: true
              effects:
                strength:
                  enabled: true
                  amplifier: 2
                speed:
                  enabled: true
                  amplifier: 2
                resistance:
                  enabled: true
                  amplifier: 1
                fire-resistance:
                  enabled: true
                haste:
                  enabled: true
                  amplifier: 2
                regeneration:
                  enabled: true
                  amplifier: 1
                water-breathing:
                  enabled: true
              extra-hearts:
                enabled: true
                amount: 10
              check-interval-ticks: 10
              container-protection:
                prevent-block-placement: true
            messages:
              gained: "&5🐉 The Dragon Egg empowers you!"
              lost: "&c🐉 The Dragon Egg's power has left you."
              cannot-store: "&cThe Dragon Egg cannot be stored in containers."
              reloaded: "&aDragonEgg configuration reloaded."
              no-permission: "&cYou don't have permission to do that."
            """;

    @Test
    @DisplayName("Verify default configuration values and 1-based amplifier mapping")
    void testDefaultConfig() {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(DEFAULT_CONFIG));
        DragonEggConfig config = new DragonEggConfig(yaml);

        assertTrue(config.isPluginEnabled());

        // Strength II -> config level 2, internal amplifier 1
        DragonEggConfig.EffectConfig strength = config.getEffectConfig(DragonEggConfig.EffectType.STRENGTH);
        assertNotNull(strength);
        assertTrue(strength.isEnabled());
        assertEquals(2, strength.getConfigLevel(), "Config level must be 2 (Level II)");
        assertEquals(1, strength.getInternalAmplifier(), "Internal amplifier must be 1 for Level II");

        // Speed II -> config level 2, internal amplifier 1
        DragonEggConfig.EffectConfig speed = config.getEffectConfig(DragonEggConfig.EffectType.SPEED);
        assertNotNull(speed);
        assertTrue(speed.isEnabled());
        assertEquals(2, speed.getConfigLevel());
        assertEquals(1, speed.getInternalAmplifier());

        // Resistance I -> config level 1, internal amplifier 0
        DragonEggConfig.EffectConfig resistance = config.getEffectConfig(DragonEggConfig.EffectType.RESISTANCE);
        assertNotNull(resistance);
        assertTrue(resistance.isEnabled());
        assertEquals(1, resistance.getConfigLevel(), "Config level must be 1 (Level I)");
        assertEquals(0, resistance.getInternalAmplifier(), "Internal amplifier must be 0 for Level I");

        // Fire Resistance -> enabled
        DragonEggConfig.EffectConfig fireRes = config.getEffectConfig(DragonEggConfig.EffectType.FIRE_RESISTANCE);
        assertNotNull(fireRes);
        assertTrue(fireRes.isEnabled());

        // Haste II -> config level 2, internal amplifier 1
        DragonEggConfig.EffectConfig haste = config.getEffectConfig(DragonEggConfig.EffectType.HASTE);
        assertNotNull(haste);
        assertTrue(haste.isEnabled());
        assertEquals(2, haste.getConfigLevel());
        assertEquals(1, haste.getInternalAmplifier());

        // Regeneration I -> config level 1, internal amplifier 0
        DragonEggConfig.EffectConfig regen = config.getEffectConfig(DragonEggConfig.EffectType.REGENERATION);
        assertNotNull(regen);
        assertTrue(regen.isEnabled());
        assertEquals(1, regen.getConfigLevel());
        assertEquals(0, regen.getInternalAmplifier());

        // Water Breathing -> enabled
        DragonEggConfig.EffectConfig water = config.getEffectConfig(DragonEggConfig.EffectType.WATER_BREATHING);
        assertNotNull(water);
        assertTrue(water.isEnabled());

        // Extra hearts: 10 hearts = 20 HP
        assertTrue(config.isExtraHeartsEnabled());
        assertEquals(10, config.getExtraHeartsAmount());
        assertEquals(20.0, config.getExtraHealthHp(), 0.001);

        // Check interval
        assertEquals(10L, config.getCheckIntervalTicks());

        // Block placement
        assertTrue(config.isPreventBlockPlacement());

        // Messages color translation
        assertTrue(config.getMessageGained().contains("§5"));
        assertTrue(config.getMessageLost().contains("§c"));
        assertTrue(config.getMessageCannotStore().contains("§c"));
        assertTrue(config.getMessageReloaded().contains("§a"));
        assertTrue(config.getMessageNoPermission().contains("§c"));
    }

    @Test
    @DisplayName("Verify custom configurations (disabling effects, level changes, custom hearts)")
    void testCustomConfig() {
        String custom = """
                dragon-egg:
                  enabled: true
                  effects:
                    strength:
                      enabled: false
                      amplifier: 3
                    speed:
                      enabled: true
                      amplifier: 4
                  extra-hearts:
                    enabled: false
                    amount: 5
                  check-interval-ticks: 20
                """;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(custom));
        DragonEggConfig config = new DragonEggConfig(yaml);

        // Strength disabled
        DragonEggConfig.EffectConfig strength = config.getEffectConfig(DragonEggConfig.EffectType.STRENGTH);
        assertFalse(strength.isEnabled());
        assertEquals(3, strength.getConfigLevel());
        assertEquals(2, strength.getInternalAmplifier());

        // Speed Level IV -> internal 3
        DragonEggConfig.EffectConfig speed = config.getEffectConfig(DragonEggConfig.EffectType.SPEED);
        assertTrue(speed.isEnabled());
        assertEquals(4, speed.getConfigLevel());
        assertEquals(3, speed.getInternalAmplifier());

        // Extra hearts disabled
        assertFalse(config.isExtraHeartsEnabled());
        assertEquals(5, config.getExtraHeartsAmount());
        assertEquals(10.0, config.getExtraHealthHp(), 0.001);

        assertEquals(20L, config.getCheckIntervalTicks());
    }

    @Test
    @DisplayName("Verify plugin.yml parses cleanly with PluginDescriptionFile")
    void testPluginYml() throws Exception {
        try (var is = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(is, "plugin.yml must be on the classpath");
            org.bukkit.plugin.PluginDescriptionFile pdf = new org.bukkit.plugin.PluginDescriptionFile(is);
            assertEquals("DragonEgg", pdf.getName());
            assertNotNull(pdf.getAPIVersion());
        }
    }
}
