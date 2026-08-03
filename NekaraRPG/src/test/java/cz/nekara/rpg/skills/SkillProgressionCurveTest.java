package cz.nekara.rpg.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillProgressionCurveTest {
    private final SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();

    @Test
    void exactThresholdsAdvanceDeterministically() {
        assertEquals(100, curve.experienceForNextLevel(0));
        assertEquals(137, curve.experienceForNextLevel(1));
        assertEquals(237, curve.cumulativeExperienceForLevel(2));

        SkillLevelProgress before = curve.resolve(236);
        assertEquals(1, before.level());
        assertEquals(136, before.experienceIntoLevel());
        assertEquals(137, before.experienceForNextLevel());

        SkillLevelProgress exact = curve.resolve(237);
        assertEquals(2, exact.level());
        assertEquals(0, exact.experienceIntoLevel());
    }

    @Test
    void progressCapsAtLevelOneHundredWithoutDiscardingTotalExperience() {
        long cap = curve.cumulativeExperienceForLevel(100);
        SkillLevelProgress progress = curve.resolve(Math.addExact(cap, 1_000_000));

        assertEquals(100, progress.level());
        assertTrue(progress.capped());
        assertEquals(cap + 1_000_000, progress.totalExperience());
        assertEquals(0, progress.experienceIntoLevel());
    }

    @Test
    void invalidAndOverflowingCurvesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SkillProgressionCurve(0, 100, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> curve.resolve(-1));
        assertThrows(ArithmeticException.class,
            () -> new SkillProgressionCurve(100, Long.MAX_VALUE, 0, 0));
    }
}
