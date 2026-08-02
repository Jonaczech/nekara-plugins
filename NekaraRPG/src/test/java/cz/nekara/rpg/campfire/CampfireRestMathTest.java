package cz.nekara.rpg.campfire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CampfireRestMathTest {
    @Test
    void groupMultiplierScalesAndStopsAtConfiguredMaximum() {
        assertEquals(1.0, CampfireRestMath.groupMultiplier(1, 0.15, 1.75));
        assertEquals(1.3, CampfireRestMath.groupMultiplier(3, 0.15, 1.75), 0.0001);
        assertEquals(1.75, CampfireRestMath.groupMultiplier(10, 0.15, 1.75));
    }

    @Test
    void fractionalHungerLossIsCarriedBetweenEvents() {
        CampfireRestMath.HungerLossResult first = CampfireRestMath.scaleHungerLoss(1, 0.5, 0.0);
        assertEquals(0, first.appliedLoss());
        assertEquals(0.5, first.carry(), 0.0001);

        CampfireRestMath.HungerLossResult second = CampfireRestMath.scaleHungerLoss(1, 0.5, first.carry());
        assertEquals(1, second.appliedLoss());
        assertEquals(0.0, second.carry(), 0.0001);
    }

    @Test
    void restedBonusCanBeRefreshedWhilePlayerRemainsAtTheFire() {
        RestedBonusState state = new RestedBonusState(2_000L, 300, false, false);
        state.refresh(5_000L, 420, true, true);

        assertEquals(true, state.isActive(4_999L));
        assertEquals(1L, state.remainingSeconds(4_001L));
        assertEquals(420, state.durationSeconds());
        assertEquals(true, state.hasteEnabled());
        assertEquals(true, state.hungerReductionEnabled());
    }

    @Test
    void everyUniqueCampFeatureAddsConfiguredRestedTime() {
        assertEquals(300, CampfireRestMath.restedDurationSeconds(300, 0, 60));
        assertEquals(420, CampfireRestMath.restedDurationSeconds(300, 2, 60));
        assertEquals(720, CampfireRestMath.restedDurationSeconds(300, 7, 60));
    }

    @Test
    void restedCountdownUsesMinutesAndZeroPaddedSeconds() {
        assertEquals("5:00", CampfireRestMath.formatCountdown(300));
        assertEquals("4:09", CampfireRestMath.formatCountdown(249));
        assertEquals("0:00", CampfireRestMath.formatCountdown(-1));
    }

    @Test
    void restedTimerYieldsToChargingAndFishingInterfaces() {
        assertEquals(true, CampfireRestMath.shouldShowRestedTimer(true, false, false));
        assertEquals(false, CampfireRestMath.shouldShowRestedTimer(true, true, false));
        assertEquals(false, CampfireRestMath.shouldShowRestedTimer(true, false, true));
        assertEquals(false, CampfireRestMath.shouldShowRestedTimer(false, false, false));
    }
}
