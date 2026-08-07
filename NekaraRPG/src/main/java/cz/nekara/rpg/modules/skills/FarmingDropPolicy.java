package cz.nekara.rpg.modules.skills;

import java.util.Set;

/** Defines the blocks whose vanilla drops can receive Farming's passive bonus. */
final class FarmingDropPolicy {
    private static final Set<String> EXTRA_SOURCES = Set.of(
        "MELON", "PUMPKIN", "BROWN_MUSHROOM", "RED_MUSHROOM",
        "KELP", "KELP_PLANT", "SUGAR_CANE", "CACTUS", "OPEN_EYEBLOSSOM", "CLOSED_EYEBLOSSOM",
        "WILDFLOWERS", "PINK_PETALS", "PITCHER_PLANT", "SEA_PICKLE", "NETHER_WART", "LILY_PAD",
        "SPORE_BLOSSOM", "GRASS", "TALL_GRASS", "FERN", "LARGE_FERN", "PALE_HANGING_MOSS",
        "DRY_GRASS", "DEAD_BUSH", "FIREFLY_BUSH", "VINE", "GLOW_LICHEN", "HANGING_ROOTS",
        "SEAGRASS", "TALL_SEAGRASS", "RESIN_CLUMP",
        "DANDELION", "POPPY", "BLUE_ORCHID", "ALLIUM", "AZURE_BLUET", "OXEYE_DAISY", "CORNFLOWER",
        "LILY_OF_THE_VALLEY", "WITHER_ROSE", "SUNFLOWER", "LILAC", "ROSE_BUSH", "PEONY"
    );

    private FarmingDropPolicy() {
    }

    static boolean isEligible(String materialName, boolean mature) {
        return mature || EXTRA_SOURCES.contains(materialName)
            || materialName.endsWith("_FLOWER") || materialName.endsWith("_TULIP");
    }
}
