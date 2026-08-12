package cz.nekara.rpg.modules.bonemeal;

import java.util.List;
import org.bukkit.Material;

final class BoneMealPolicy {
    private static final List<Material> DYE_FLOWERS = List.of(
        Material.DANDELION, Material.POPPY, Material.BLUE_ORCHID, Material.ALLIUM,
        Material.AZURE_BLUET, Material.RED_TULIP, Material.ORANGE_TULIP,
        Material.WHITE_TULIP, Material.PINK_TULIP, Material.OXEYE_DAISY,
        Material.CORNFLOWER, Material.LILY_OF_THE_VALLEY, Material.WITHER_ROSE,
        Material.SUNFLOWER, Material.LILAC, Material.ROSE_BUSH, Material.PEONY,
        Material.TORCHFLOWER, Material.PITCHER_PLANT
    );

    private BoneMealPolicy() {
    }

    static boolean isGrass(Material material) {
        return material == Material.GRASS_BLOCK;
    }

    static boolean isDesertSand(Material material) {
        return material == Material.SAND || material == Material.RED_SAND;
    }

    static List<Material> dyeFlowers() {
        return DYE_FLOWERS;
    }

    static DesertPlant desertPlantForRoll(int roll) {
        if (roll < 0 || roll >= 100) throw new IllegalArgumentException("Roll must be between 0 and 99");
        if (roll < 35) return DesertPlant.DEAD_BUSH;
        if (roll < 60) return DesertPlant.SHORT_DRY_GRASS;
        if (roll < 80) return DesertPlant.TALL_DRY_GRASS;
        return DesertPlant.CACTUS_FLOWER;
    }

    enum DesertPlant {
        DEAD_BUSH,
        SHORT_DRY_GRASS,
        TALL_DRY_GRASS,
        CACTUS_FLOWER
    }
}
