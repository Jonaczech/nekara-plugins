package cz.nekara.rpg.campfire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CampfireSleepRulesTest {
    @Test
    void loneLyingPlayerCanPassNightAfterDelay() {
        assertTrue(CampfireSleepRules.canSkipNight(
                true, 1, true, 5_000L, 5_000L, true, 13_000L));
    }

    @Test
    void anotherOnlinePlayerAlwaysPreventsNightSkip() {
        assertFalse(CampfireSleepRules.canSkipNight(
                true, 2, true, 10_000L, 5_000L, true, 13_000L));
    }

    @Test
    void lyingOutsideActiveCampfireNeverPassesNight() {
        assertFalse(CampfireSleepRules.canSkipNight(
                true, 1, false, 10_000L, 5_000L, true, 13_000L));
    }

    @Test
    void dayAndNonOverworldNeverAdvance() {
        assertFalse(CampfireSleepRules.canSkipNight(
                true, 1, true, 10_000L, 5_000L, true, 6_000L));
        assertFalse(CampfireSleepRules.canSkipNight(
                true, 1, true, 10_000L, 5_000L, false, 13_000L));
    }
}
