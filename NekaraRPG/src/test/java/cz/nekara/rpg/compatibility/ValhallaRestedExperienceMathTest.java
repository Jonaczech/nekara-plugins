package cz.nekara.rpg.compatibility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValhallaRestedExperienceMathTest {
    @Test
    void naturalAndSharedSkillExperienceAreEligible() {
        assertTrue(ValhallaRestedExperienceMath.isEligibleReason("SKILL_ACTION"));
        assertTrue(ValhallaRestedExperienceMath.isEligibleReason("EXP_SHARE"));
        assertFalse(ValhallaRestedExperienceMath.isEligibleReason("COMMAND"));
        assertFalse(ValhallaRestedExperienceMath.isEligibleReason("RESET"));
    }

    @Test
    void restedMultiplierAddsTenPercentWithoutChangingInvalidAmounts() {
        assertEquals(110.0, ValhallaRestedExperienceMath.applyMultiplier(100.0, 1.10), 0.0001);
        assertEquals(0.0, ValhallaRestedExperienceMath.applyMultiplier(0.0, 1.10), 0.0001);
        assertEquals(-5.0, ValhallaRestedExperienceMath.applyMultiplier(-5.0, 1.10), 0.0001);
        assertEquals(100.0, ValhallaRestedExperienceMath.applyMultiplier(100.0, 0.5), 0.0001);
    }
}
