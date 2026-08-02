package cz.nekara.rpg.echovein;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoVeinMathTest {
    @Test
    void triggerRequiresExpiredCooldownAndWinningRoll() {
        assertTrue(EchoVeinMath.canTrigger(1_000, 1_000, 0.039, 0.04));
        assertFalse(EchoVeinMath.canTrigger(1_001, 1_000, 0.0, 0.04));
        assertFalse(EchoVeinMath.canTrigger(0, 1_000, 0.04, 0.04));
        assertFalse(EchoVeinMath.canTrigger(0, 1_000, Double.NaN, 0.04));
    }

    @Test
    void bonusExperienceUsesFinalSourceAmountOnce() {
        assertEquals(27.5, EchoVeinMath.bonusExperience(110.0, 0.25), 0.0001);
        assertEquals(0.0, EchoVeinMath.bonusExperience(-1.0, 0.25), 0.0001);
        assertEquals(0.0, EchoVeinMath.bonusExperience(100.0, Double.NaN), 0.0001);
    }

    @Test
    void dropSelectionIsWeightedByActualFinalStackAmounts() {
        List<Integer> amounts = List.of(3, 0, 1, 2);
        assertEquals(0, EchoVeinMath.weightedUnitIndex(amounts, 0));
        assertEquals(0, EchoVeinMath.weightedUnitIndex(amounts, 2));
        assertEquals(2, EchoVeinMath.weightedUnitIndex(amounts, 3));
        assertEquals(3, EchoVeinMath.weightedUnitIndex(amounts, 4));
        assertEquals(3, EchoVeinMath.weightedUnitIndex(amounts, 5));
        assertEquals(-1, EchoVeinMath.weightedUnitIndex(amounts, 6));
        assertEquals(-1, EchoVeinMath.weightedUnitIndex(List.of(0, -1), 0));
    }
}
