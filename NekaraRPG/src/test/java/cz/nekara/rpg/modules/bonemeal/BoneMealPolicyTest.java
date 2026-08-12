package cz.nekara.rpg.modules.bonemeal;

import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoneMealPolicyTest {
    @Test
    void recognizesOnlyTheRequestedTerrain() {
        assertTrue(BoneMealPolicy.isGrass(Material.GRASS_BLOCK));
        assertFalse(BoneMealPolicy.isGrass(Material.DIRT));
        assertTrue(BoneMealPolicy.isDesertSand(Material.SAND));
        assertTrue(BoneMealPolicy.isDesertSand(Material.RED_SAND));
        assertFalse(BoneMealPolicy.isDesertSand(Material.GRASS_BLOCK));
    }

    @Test
    void desertPoolContainsNoStandaloneCactus() {
        assertEquals(BoneMealPolicy.DesertPlant.DEAD_BUSH, BoneMealPolicy.desertPlantForRoll(0));
        assertEquals(BoneMealPolicy.DesertPlant.DEAD_BUSH, BoneMealPolicy.desertPlantForRoll(34));
        assertEquals(BoneMealPolicy.DesertPlant.SHORT_DRY_GRASS, BoneMealPolicy.desertPlantForRoll(35));
        assertEquals(BoneMealPolicy.DesertPlant.SHORT_DRY_GRASS, BoneMealPolicy.desertPlantForRoll(59));
        assertEquals(BoneMealPolicy.DesertPlant.TALL_DRY_GRASS, BoneMealPolicy.desertPlantForRoll(60));
        assertEquals(BoneMealPolicy.DesertPlant.TALL_DRY_GRASS, BoneMealPolicy.desertPlantForRoll(79));
        assertEquals(BoneMealPolicy.DesertPlant.CACTUS_FLOWER, BoneMealPolicy.desertPlantForRoll(80));
        assertEquals(BoneMealPolicy.DesertPlant.CACTUS_FLOWER, BoneMealPolicy.desertPlantForRoll(99));
    }

    @Test
    void dyeFlowerPoolCoversEveryVanillaFlowerDyeSource() {
        List<Material> flowers = BoneMealPolicy.dyeFlowers();
        assertTrue(flowers.containsAll(List.of(
            Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
            Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
            Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
            Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE,
            Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
            Material.TORCHFLOWER, Material.PITCHER_PLANT
        )));
    }
}
