package com.abohasiba.dragonegg;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Manages player maximum health modifications granted by the Dragon Egg artifact.
 *
 * CRITICAL DESIGN GUARANTEES:
 * 1. Isolates DragonEgg health bonus via a dedicated NamespacedKey modifier on Attribute.MAX_HEALTH.
 * 2. Stacks safely with ANY other health-modifying plugin (RankedSMP, Lifesteal, RPG plugins, etc.).
 * 3. NEVER modifies base health or clears existing modifiers.
 * 4. NEVER touches, alters, or removes other plugins' attribute modifiers.
 * 5. Safely clamps health upon removal without dealing lethal or arbitrary damage.
 */
public class HealthManager {

    public static final String MODIFIER_KEY_NAME = "dragon_egg_health";

    private final NamespacedKey modifierKey;

    public HealthManager(Plugin plugin) {
        this(new NamespacedKey(plugin, MODIFIER_KEY_NAME));
    }

    public HealthManager(NamespacedKey modifierKey) {
        this.modifierKey = modifierKey;
    }

    public NamespacedKey getModifierKey() {
        return modifierKey;
    }

    /**
     * Applies the Dragon Egg extra hearts attribute modifier to the given player.
     * All other modifiers (RankedSMP, Lifesteal, RPG stats, armor attributes) are strictly preserved.
     */
    public void applyHealthBonus(Player player, DragonEggConfig config) {
        if (player == null || !player.isOnline()) {
            return;
        }

        if (!config.isExtraHeartsEnabled() || config.getExtraHeartsAmount() <= 0) {
            removeHealthBonus(player);
            return;
        }

        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        applyHealthModifier(attribute, config.getExtraHealthHp());
    }

    /**
     * Applies or updates the Dragon Egg modifier directly on an AttributeInstance.
     */
    public boolean applyHealthModifier(AttributeInstance attribute, double extraHp) {
        if (attribute == null) {
            return false;
        }

        AttributeModifier existing = attribute.getModifier(modifierKey);
        if (existing != null) {
            if (Double.compare(existing.getAmount(), extraHp) == 0) {
                return false; // Already active with desired amount
            }
            attribute.removeModifier(modifierKey);
        }

        AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                extraHp,
                AttributeModifier.Operation.ADD_NUMBER
        );

        attribute.addModifier(modifier);
        return true;
    }

    /**
     * Removes ONLY the Dragon Egg health modifier from the player.
     * Leaves every other plugin's modifiers completely untouched.
     * Safely clamps player health to the new maximum if it exceeds it.
     */
    public void removeHealthBonus(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute == null) {
            return;
        }

        double newMaxHealth = removeHealthModifier(attribute);

        // Safely clamp health to the new maximum if current health exceeds it
        if (player.getHealth() > newMaxHealth) {
            player.setHealth(Math.max(1.0, Math.min(player.getHealth(), newMaxHealth)));
        }
    }

    /**
     * Removes only the Dragon Egg modifier from an AttributeInstance and returns the new max health.
     */
    public double removeHealthModifier(AttributeInstance attribute) {
        if (attribute == null) {
            return 20.0;
        }

        AttributeModifier existing = attribute.getModifier(modifierKey);
        if (existing != null) {
            attribute.removeModifier(modifierKey);
        }

        return attribute.getValue();
    }

    /**
     * Checks if the player currently has the Dragon Egg health modifier active.
     */
    public boolean hasHealthBonus(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null && attribute.getModifier(modifierKey) != null;
    }
}
