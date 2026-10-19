package com.abohasiba.dragonegg;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthManagerTest {

    private HealthManager healthManager;
    private NamespacedKey dragonEggKey;

    @BeforeEach
    void setUp() {
        dragonEggKey = new NamespacedKey("dragonegg", HealthManager.MODIFIER_KEY_NAME);
        healthManager = new HealthManager(dragonEggKey);
    }

    /**
     * Simulated AttributeInstance tracking modifiers accurately in memory.
     */
    @SuppressWarnings({"deprecation", "removal"})
    static class FakeAttributeInstance implements AttributeInstance {
        double baseValue = 20.0; // Standard vanilla player health (10 hearts)
        final Map<net.kyori.adventure.key.Key, AttributeModifier> modifiers = new HashMap<>();

        @Override
        public Attribute getAttribute() {
            return null;
        }

        @Override
        public double getBaseValue() {
            return baseValue;
        }

        @Override
        public void setBaseValue(double v) {
            this.baseValue = v;
        }

        @Override
        public java.util.Collection<AttributeModifier> getModifiers() {
            return modifiers.values();
        }

        @Override
        public AttributeModifier getModifier(net.kyori.adventure.key.Key key) {
            return modifiers.get(key);
        }

        @Override
        public void removeModifier(net.kyori.adventure.key.Key key) {
            modifiers.remove(key);
        }

        @Override
        public AttributeModifier getModifier(java.util.UUID uuid) {
            return null;
        }

        @Override
        public void removeModifier(java.util.UUID uuid) {}

        @Override
        public void addModifier(AttributeModifier modifier) {
            modifiers.put(modifier.getKey(), modifier);
        }

        @Override
        public void addTransientModifier(AttributeModifier modifier) {
            addModifier(modifier);
        }

        @Override
        public void removeModifier(AttributeModifier modifier) {
            modifiers.remove(modifier.getKey());
        }

        @Override
        public double getValue() {
            double total = baseValue;
            for (AttributeModifier mod : modifiers.values()) {
                if (mod.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                    total += mod.getAmount();
                }
            }
            return total;
        }

        @Override
        public double getDefaultValue() {
            return 20.0;
        }
    }

    @Test
    @DisplayName("Verify universal health modifier stacking with multiple plugins (RankedSMP + Lifesteal + DragonEgg)")
    void testUniversalHealthStacking() {
        FakeAttributeInstance maxHealthAttr = new FakeAttributeInstance();

        // 1. Simulate RankedSMP plugin adding health modifier (+10 hearts = +20 HP)
        NamespacedKey rankedSmpKey = new NamespacedKey("rankedsmp", "rank_health");
        AttributeModifier rankedSmpMod = new AttributeModifier(rankedSmpKey, 20.0, AttributeModifier.Operation.ADD_NUMBER);
        maxHealthAttr.addModifier(rankedSmpMod);

        // 2. Simulate Lifesteal plugin adding health modifier (+5 hearts = +10 HP)
        NamespacedKey lifestealKey = new NamespacedKey("lifesteal", "hearts");
        AttributeModifier lifestealMod = new AttributeModifier(lifestealKey, 10.0, AttributeModifier.Operation.ADD_NUMBER);
        maxHealthAttr.addModifier(lifestealMod);

        // Current health before DragonEgg: 20 base + 20 RankedSMP + 10 Lifesteal = 50 HP (25 hearts)
        assertEquals(50.0, maxHealthAttr.getValue(), 0.001);

        // Configuration with 10 extra hearts (+20 HP)
        String configYaml = """
                dragon-egg:
                  extra-hearts:
                    enabled: true
                    amount: 10
                """;
        DragonEggConfig config = new DragonEggConfig(YamlConfiguration.loadConfiguration(new StringReader(configYaml)));

        // Apply DragonEgg health bonus directly to the AttributeInstance
        boolean applied = healthManager.applyHealthModifier(maxHealthAttr, config.getExtraHealthHp());
        assertTrue(applied);

        // 3. Verify total max health: 20 (base) + 20 (RankedSMP) + 10 (Lifesteal) + 20 (DragonEgg) = 70 HP (35 hearts)
        assertEquals(70.0, maxHealthAttr.getValue(), 0.001, "All 3 plugins must stack cleanly without overwriting!");
        assertNotNull(maxHealthAttr.getModifier(dragonEggKey));

        // 4. Verify RankedSMP and Lifesteal modifiers are completely intact
        assertNotNull(maxHealthAttr.getModifier(rankedSmpKey), "RankedSMP modifier must not be removed!");
        assertNotNull(maxHealthAttr.getModifier(lifestealKey), "Lifesteal modifier must not be removed!");

        // 5. Simulate RankedSMP updating rank while carrying egg (+30 HP instead of +20 HP)
        maxHealthAttr.removeModifier(rankedSmpKey);
        AttributeModifier newRankedSmpMod = new AttributeModifier(rankedSmpKey, 30.0, AttributeModifier.Operation.ADD_NUMBER);
        maxHealthAttr.addModifier(newRankedSmpMod);

        // Total should now be 20 base + 30 RankedSMP + 10 Lifesteal + 20 DragonEgg = 80 HP
        assertEquals(80.0, maxHealthAttr.getValue(), 0.001);
        assertNotNull(maxHealthAttr.getModifier(dragonEggKey), "DragonEgg modifier must still be active after rank update!");

        // 6. Simulate losing the Dragon Egg
        double newMaxHealth = healthManager.removeHealthModifier(maxHealthAttr);

        // Verify DragonEgg modifier was removed
        assertNull(maxHealthAttr.getModifier(dragonEggKey));

        // Verify RankedSMP and Lifesteal modifiers REMAIN 100% INTACT!
        assertNotNull(maxHealthAttr.getModifier(rankedSmpKey), "RankedSMP modifier must remain untouched!");
        assertEquals(30.0, maxHealthAttr.getModifier(rankedSmpKey).getAmount(), 0.001);

        assertNotNull(maxHealthAttr.getModifier(lifestealKey), "Lifesteal modifier must remain untouched!");
        assertEquals(10.0, maxHealthAttr.getModifier(lifestealKey).getAmount(), 0.001);

        // Total remaining max health: 20 base + 30 RankedSMP + 10 Lifesteal = 60 HP
        assertEquals(60.0, newMaxHealth, 0.001);
        assertEquals(60.0, maxHealthAttr.getValue(), 0.001);

        // 7. Verify health clamping logic
        double playerCurrentHealth = 75.0;
        double clampedHealth = Math.max(1.0, Math.min(playerCurrentHealth, newMaxHealth));
        assertEquals(60.0, clampedHealth, 0.001, "Player health must be safely clamped to remaining max health!");
    }
}
