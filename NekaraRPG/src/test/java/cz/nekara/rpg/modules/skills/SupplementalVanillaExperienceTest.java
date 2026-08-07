package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupplementalVanillaExperienceTest {
    @Test
    void turnsSmallPerkAwardsIntoOccasionalWholeExperience() {
        SupplementalVanillaExperience experience = new SupplementalVanillaExperience();
        UUID playerId = UUID.randomUUID();

        assertEquals(0, experience.claim(playerId, 0.25));
        assertEquals(0, experience.claim(playerId, 0.25));
        assertEquals(0, experience.claim(playerId, 0.25));
        assertEquals(1, experience.claim(playerId, 0.25));
    }

    @Test
    void doesNotCapSupplementalExperience() {
        SupplementalVanillaExperience experience = new SupplementalVanillaExperience();
        UUID playerId = UUID.randomUUID();

        assertEquals(100, experience.claim(playerId, 100.0));
        assertEquals(1, experience.claim(playerId, 1.0));
    }
}
