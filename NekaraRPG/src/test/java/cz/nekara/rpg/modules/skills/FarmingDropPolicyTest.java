package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FarmingDropPolicyTest {
    @Test
    void includesTheExpandedFarmingSources() {
        for (String source : new String[]{
            "KELP", "SUGAR_CANE", "CACTUS", "OPEN_EYEBLOSSOM", "CLOSED_EYEBLOSSOM", "WILDFLOWERS",
            "PINK_PETALS", "PITCHER_PLANT", "SEA_PICKLE", "NETHER_WART", "LILY_PAD", "SPORE_BLOSSOM",
            "GRASS", "TALL_GRASS", "FERN", "LARGE_FERN", "PALE_HANGING_MOSS", "DRY_GRASS", "DEAD_BUSH",
            "FIREFLY_BUSH", "VINE", "GLOW_LICHEN", "HANGING_ROOTS", "SEAGRASS", "TALL_SEAGRASS", "RESIN_CLUMP"
        }) {
            assertTrue(FarmingDropPolicy.isEligible(source, false), source);
        }
    }

    @Test
    void keepsMatureCropsAndFlowersEligibleWithoutOpeningUnrelatedBlocks() {
        assertTrue(FarmingDropPolicy.isEligible("WHEAT", true));
        assertTrue(FarmingDropPolicy.isEligible("BLUE_ORCHID", false));
        assertFalse(FarmingDropPolicy.isEligible("OAK_LOG", false));
    }
}
