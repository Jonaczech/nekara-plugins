package cz.nekara.rpg.modules.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatheringDropChanceMathTest {
    @Test
    void combinesIndependentWoodcuttingChances() {
        assertEquals(0.475, GatheringDropChanceMath.atLeastOneBonusDrop(0.25, 0.0, 0.30), 0.000_001);
    }

    @Test
    void combinesAllFarmingHarvestChances() {
        assertEquals(0.60625,
            GatheringDropChanceMath.atLeastOneBonusDrop(0.25, 0.0, 0.25, 0.30), 0.000_001);
    }

    @Test
    void combinesFarmingAnimalChancesWithoutInnateHarvestRoll() {
        assertEquals(0.5625,
            GatheringDropChanceMath.atLeastOneBonusDrop(0.0, 0.0, 0.375, 0.30), 0.000_001);
    }

    @Test
    void rejectsInvalidChance() {
        assertThrows(IllegalArgumentException.class,
            () -> GatheringDropChanceMath.atLeastOneBonusDrop(1.01, 0.0));
    }
}
