package cz.nekara.rpg.skills.milestones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PowerMilestoneCatalogTest {
    private final PowerMilestoneCatalog catalog = new PowerMilestoneCatalog(
        Arrays.stream(PowerMilestoneId.values()).map(PowerMilestoneId::milestone).toList());

    @Test
    void milestonesUnlockAtOneTwentyFiveOneHundredAndTwoHundred() {
        assertEquals(1, catalog.unlockedAt(1).size());
        assertEquals(1, catalog.unlockedAt(24).size());
        assertEquals(2, catalog.unlockedAt(25).size());
        assertEquals(2, catalog.unlockedAt(99).size());
        assertEquals(3, catalog.unlockedAt(100).size());
        assertEquals(3, catalog.unlockedAt(199).size());
        assertEquals(4, catalog.unlockedAt(200).size());
    }

    @Test
    void milestoneCannotExceedTheMaximumDerivedPowerLevel() {
        assertThrows(IllegalArgumentException.class, () -> new PowerMilestone("too_high", 201));
    }
}
