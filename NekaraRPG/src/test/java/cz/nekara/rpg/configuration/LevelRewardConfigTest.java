package cz.nekara.rpg.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevelRewardConfigTest {
    private final LevelRewardConfig rewards = new LevelRewardConfig(
        0.002, 0.20,
        0.00010, 0.010,
        0.005, 0.00015, 0.020
    );

    @Test
    void fishingTreasureStartsWithASmallChanceAndCapsAtLevelOneHundred() {
        assertEquals(0.005, rewards.fishingTreasureChance(0), 0.000001);
        assertEquals(0.0125, rewards.fishingTreasureChance(50), 0.000001);
        assertEquals(0.020, rewards.fishingTreasureChance(100), 0.000001);
    }

    @Test
    void gatheringDoubleDropReachesTwentyPercentAtLevelOneHundred() {
        assertEquals(0.20, rewards.gatheringDoubleDropChance(100), 0.000001);
        assertEquals(0.25, rewards.gatheringDoubleDropChance(100, 1.25), 0.000001);
    }
}
