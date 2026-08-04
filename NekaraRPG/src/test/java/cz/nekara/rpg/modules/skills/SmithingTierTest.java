package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SmithingTierTest {
    @Test
    void constructionOutputScalesFromTheSmithingTierWithoutExceedingStackSize() {
        assertEquals(4, SmithingTier.efficientOutput(4, 0));
        assertEquals(5, SmithingTier.efficientOutput(4, 20));
        assertEquals(6, SmithingTier.efficientOutput(4, 40));
        assertEquals(7, SmithingTier.efficientOutput(4, 70));
        assertEquals(8, SmithingTier.efficientOutput(4, 100));
        assertEquals(64, SmithingTier.efficientOutput(64, 100));
    }

    @Test
    void workshopProcessingAppliesOnlyToMetalLikeEquipment() {
        assertTrue(SmithingTier.requiresProcessing(Material.IRON_SWORD));
        assertTrue(SmithingTier.requiresProcessing(Material.DIAMOND_CHESTPLATE));
        assertTrue(SmithingTier.requiresProcessing(Material.CHAINMAIL_BOOTS));
        assertFalse(SmithingTier.requiresProcessing(Material.WOODEN_SWORD));
        assertFalse(SmithingTier.requiresProcessing(Material.STONE_AXE));
        assertFalse(SmithingTier.requiresProcessing(Material.LEATHER_HELMET));
        assertFalse(SmithingTier.requiresProcessing(Material.BOW));
        assertFalse(SmithingTier.requiresProcessing(Material.CROSSBOW));
    }

    @Test
    void bulkCraftingAlsoCoversConstructionComponentsButNotEquipment() {
        assertTrue(ProductionPerkListener.isEfficientConstructionComponent(Material.STICK));
        assertTrue(ProductionPerkListener.isEfficientConstructionComponent(Material.BOWL));
        assertTrue(ProductionPerkListener.isEfficientConstructionComponent(Material.BRICK));
        assertTrue(ProductionPerkListener.isEfficientConstructionComponent(Material.RESIN_BRICK));
        assertFalse(ProductionPerkListener.isEfficientConstructionComponent(Material.IRON_SWORD));
        assertFalse(ProductionPerkListener.isEfficientConstructionComponent(Material.COOKED_BEEF));
    }
}
