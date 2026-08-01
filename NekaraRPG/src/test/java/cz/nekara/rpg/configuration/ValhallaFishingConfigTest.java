package cz.nekara.rpg.configuration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValhallaFishingConfigTest {
    @Test
    void selectsConfiguredLevelTiersAndUsesOpenEndedExpertTier() {
        ValhallaFishingConfig config = new ValhallaFishingConfig(
                true,
                2,
                3,
                List.of(
                        new ValhallaFishingTier("novice", 1, 30, 3, 5, 1),
                        new ValhallaFishingTier("skilled", 31, 60, 3, 4, 2),
                        new ValhallaFishingTier("expert", 61, 0, 2, 3, 3)
                )
        );

        assertEquals("novice", config.tierForLevel(1).name());
        assertEquals("novice", config.tierForLevel(30).name());
        assertEquals("skilled", config.tierForLevel(31).name());
        assertEquals("skilled", config.tierForLevel(60).name());
        assertEquals("expert", config.tierForLevel(61).name());
        assertEquals("expert", config.tierForLevel(999).name());
    }
}
