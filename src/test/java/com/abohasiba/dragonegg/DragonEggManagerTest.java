package com.abohasiba.dragonegg;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DragonEggManagerTest {

    @Test
    @DisplayName("Verify genuine Dragon Egg detection and fake/renamed item rejection")
    void testGenuineDragonEggDetection() {
        // Null item
        assertFalse(DragonEggManager.isGenuineDragonEgg(null));

        // Amount <= 0
        ItemStack emptyEgg = mock(ItemStack.class);
        when(emptyEgg.getType()).thenReturn(Material.DRAGON_EGG);
        when(emptyEgg.getAmount()).thenReturn(0);
        assertFalse(DragonEggManager.isGenuineDragonEgg(emptyEgg));

        // Different material (e.g. DIRT renamed to Dragon Egg)
        ItemStack fakeDirt = mock(ItemStack.class);
        when(fakeDirt.getType()).thenReturn(Material.DIRT);
        when(fakeDirt.getAmount()).thenReturn(1);
        assertFalse(DragonEggManager.isGenuineDragonEgg(fakeDirt));

        // Genuine Dragon Egg without custom meta
        ItemStack genuineEgg = mock(ItemStack.class);
        when(genuineEgg.getType()).thenReturn(Material.DRAGON_EGG);
        when(genuineEgg.getAmount()).thenReturn(1);
        when(genuineEgg.getItemMeta()).thenReturn(null);
        assertTrue(DragonEggManager.isGenuineDragonEgg(genuineEgg));

        // Genuine Dragon Egg with plain meta
        ItemMeta plainMeta = mock(ItemMeta.class);
        when(plainMeta.hasDisplayName()).thenReturn(false);
        when(plainMeta.hasLore()).thenReturn(false);
        when(plainMeta.hasCustomModelDataComponent()).thenReturn(false);
        when(genuineEgg.getItemMeta()).thenReturn(plainMeta);
        assertTrue(DragonEggManager.isGenuineDragonEgg(genuineEgg));

        // Renamed Dragon Egg (on anvil or by plugin)
        ItemMeta renamedMeta = mock(ItemMeta.class);
        when(renamedMeta.hasDisplayName()).thenReturn(true);
        when(renamedMeta.hasLore()).thenReturn(false);
        when(renamedMeta.hasCustomModelDataComponent()).thenReturn(false);
        ItemStack renamedEgg = mock(ItemStack.class);
        when(renamedEgg.getType()).thenReturn(Material.DRAGON_EGG);
        when(renamedEgg.getAmount()).thenReturn(1);
        when(renamedEgg.getItemMeta()).thenReturn(renamedMeta);
        assertFalse(DragonEggManager.isGenuineDragonEgg(renamedEgg), "Renamed egg must NOT activate powers!");

        // Dragon Egg with custom lore
        ItemMeta loreMeta = mock(ItemMeta.class);
        when(loreMeta.hasDisplayName()).thenReturn(false);
        when(loreMeta.hasLore()).thenReturn(true);
        when(loreMeta.hasCustomModelDataComponent()).thenReturn(false);
        ItemStack loreEgg = mock(ItemStack.class);
        when(loreEgg.getType()).thenReturn(Material.DRAGON_EGG);
        when(loreEgg.getAmount()).thenReturn(1);
        when(loreEgg.getItemMeta()).thenReturn(loreMeta);
        assertFalse(DragonEggManager.isGenuineDragonEgg(loreEgg), "Custom lore egg must NOT activate powers!");

        // Dragon Egg with CustomModelDataComponent
        ItemMeta componentMeta = mock(ItemMeta.class);
        when(componentMeta.hasDisplayName()).thenReturn(false);
        when(componentMeta.hasLore()).thenReturn(false);
        when(componentMeta.hasCustomModelDataComponent()).thenReturn(true);
        ItemStack componentEgg = mock(ItemStack.class);
        when(componentEgg.getType()).thenReturn(Material.DRAGON_EGG);
        when(componentEgg.getAmount()).thenReturn(1);
        when(componentEgg.getItemMeta()).thenReturn(componentMeta);
        assertFalse(DragonEggManager.isGenuineDragonEgg(componentEgg), "Egg with custom model component must NOT activate powers!");
    }
}
