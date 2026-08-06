package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import net.kyori.adventure.text.format.NamedTextColor;

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
        assertTrue(SmithingTier.requiresProcessing(Material.IRON_SPEAR));
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

    @Test
    void craftsmanshipQualityIsExclusiveToPerksAndGuaranteesAnUncommonMinimum() {
        assertEquals(0.0, SmithingTier.qualityPromotionChance(1.0));
        assertEquals(0.25, SmithingTier.qualityPromotionChance(1.25));
        assertEquals(1.0, SmithingTier.qualityPromotionChance(5.0));
        assertEquals(0.63, SmithingTier.qualityPromotionChance(1.53, 2.0, 2, 0.05));
        assertEquals(0.63, SmithingTier.qualityPromotionChance(1.53, 10.0, 2, 0.05));
        assertEquals(SmithingTier.I, SmithingTier.qualityFor(0, false, false, 1.53, 0.0));
        assertEquals(SmithingTier.II, SmithingTier.qualityFor(1, false, false, 1.05, 0.01));
        assertEquals(SmithingTier.II, SmithingTier.qualityFor(1, false, false, 1.05, 0.50));
        assertEquals(SmithingTier.II, SmithingTier.qualityFor(5, true, true, 1.53, 0.99));
        assertEquals(SmithingTier.III, SmithingTier.qualityFor(3, false, false, 1.15, 0.02));
        assertEquals(SmithingTier.IV, SmithingTier.qualityFor(3, true, false, 1.33, 0.01));
        assertEquals(SmithingTier.V, SmithingTier.qualityFor(3, true, true, 1.53, 0.001));
        assertEquals(SmithingTier.V, SmithingTier.qualityFor(5, true, true, 1.53, 0.07));
        assertEquals(SmithingTier.IV, SmithingTier.qualityFor(5, true, true, 1.53, 0.10));
        assertEquals(SmithingTier.III, SmithingTier.qualityFor(5, true, true, 1.53, 0.30));
        assertEquals(SmithingTier.V, SmithingTier.qualityFor(5, true, true, 1.6625, 0.09));
    }

    @Test
    void craftsmanshipQualitiesUseTheDefinedNamesAndDistinctSeals() {
        assertEquals("Běžná", SmithingTier.I.displayName());
        assertEquals("Neobyčejná", SmithingTier.II.displayName());
        assertEquals("Vzácná", SmithingTier.III.displayName());
        assertEquals("Epická", SmithingTier.IV.displayName());
        assertEquals("Legendární", SmithingTier.V.displayName());
        assertEquals(NamedTextColor.WHITE, SmithingTier.I.displayColor());
        assertEquals(NamedTextColor.GREEN, SmithingTier.II.displayColor());
        assertEquals(NamedTextColor.BLUE, SmithingTier.III.displayColor());
        assertEquals(NamedTextColor.LIGHT_PURPLE, SmithingTier.IV.displayColor());
        assertEquals(NamedTextColor.GOLD, SmithingTier.V.displayColor());
        assertEquals(5, java.util.Arrays.stream(SmithingTier.values()).map(SmithingTier::icon).distinct().count());
        assertFalse(SmithingTier.I.bold());
        assertFalse(SmithingTier.III.bold());
        assertTrue(SmithingTier.IV.bold());
        assertTrue(SmithingTier.V.bold());
    }

    @Test
    void perkRecipeIngredientsRequireTheWholeVanillaCraftingMatrix() {
        assertTrue(ProductionPerkListener.isWorkshopKitIngredients(new Material[] {
            Material.IRON_NUGGET, Material.IRON_NUGGET, Material.IRON_NUGGET, Material.IRON_NUGGET,
            Material.PAPER, Material.STRING
        }));
        assertTrue(ProductionPerkListener.isScoutArrowIngredients(new Material[] {
            Material.ARROW, Material.ARROW, Material.ARROW, Material.ARROW,
            Material.GLOW_INK_SAC, Material.AMETHYST_SHARD
        }));
        assertFalse(ProductionPerkListener.isScoutArrowIngredients(new Material[] {
            Material.ARROW, Material.GLOW_INK_SAC, Material.AMETHYST_SHARD
        }));
    }

    @Test
    void groundReplicationHasExplicitRecipesAndDoesNotAcceptIncompleteIngredients() {
        assertEquals(Material.GRASS_BLOCK, ProductionPerkListener.groundReplicationMaterial(new Material[] {
            Material.DIRT, Material.DIRT, Material.DIRT, Material.DIRT, Material.MOSS_BLOCK, Material.BONE_MEAL
        }).orElseThrow());
        assertEquals(Material.ROOTED_DIRT, ProductionPerkListener.groundReplicationMaterial(new Material[] {
            Material.DIRT, Material.DIRT, Material.DIRT, Material.DIRT, Material.HANGING_ROOTS, Material.BONE_MEAL
        }).orElseThrow());
        assertTrue(ProductionPerkListener.groundReplicationMaterial(new Material[] {
            Material.DIRT, Material.DIRT, Material.DIRT, Material.DIRT, Material.BONE_MEAL
        }).isEmpty());
    }
}
