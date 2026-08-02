package cz.nekara.rpg.echovein;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoVeinMathTest {
    @Test
    void chanceRequiresWinningFiniteRollWithoutCooldown() {
        assertTrue(EchoVeinMath.winsChance(0.049, 0.05));
        assertFalse(EchoVeinMath.winsChance(0.05, 0.05));
        assertFalse(EchoVeinMath.winsChance(Double.NaN, 0.05));
        assertFalse(EchoVeinMath.winsChance(0.0, Double.NaN));
        assertFalse(EchoVeinMath.winsChance(0.0, 0.0));
    }

    @Test
    void previousDefaultChanceMigratesWithoutChangingCustomValues() {
        assertEquals(0.05, EchoVeinMath.migratePreviousDefaultTriggerChance(0.04), 0.0001);
        assertEquals(0.041, EchoVeinMath.migratePreviousDefaultTriggerChance(0.041), 0.0001);
        assertEquals(0.10, EchoVeinMath.migratePreviousDefaultTriggerChance(0.10), 0.0001);
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
