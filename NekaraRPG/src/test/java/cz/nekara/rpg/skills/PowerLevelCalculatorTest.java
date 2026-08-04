package cz.nekara.rpg.skills;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PowerLevelCalculatorTest {
    private final PowerLevelCalculator calculator = new PowerLevelCalculator(100);

    @Test
    void powerIsFloorAverageOfAllGameplaySkills() {
        EnumMap<SkillId, Integer> levels = allAt(50);
        PowerProgress power = calculator.calculate(levels);

        assertEquals(50, power.level());
        assertEquals(750, power.contributingLevelTotal());
        assertEquals(15, power.levelsUntilNext());
    }

    @Test
    void missingSkillsCountAsZeroAndNoSingleSkillCanDominatePower() {
        PowerProgress power = calculator.calculate(Map.of(SkillId.MINING, 100));

        assertEquals(6, power.level());
        assertEquals(5, power.levelsUntilNext());
    }

    @Test
    void fullyCappedFirstRunCanStillProgressToNewGamePlusPower() {
        PowerProgress power = calculator.calculate(allAt(100));

        assertEquals(100, power.level());
        assertEquals(15, power.levelsUntilNext());
    }

    @Test
    void newGamePlusMakesASecondLevelingRunContributeBeyondOneHundredPower() {
        EnumMap<SkillId, Integer> levels = allAt(100);
        EnumMap<SkillId, Integer> rebirths = allAt(1);
        PowerProgress power = calculator.calculate(levels, rebirths);

        assertEquals(200, power.level());
        assertEquals(0, power.levelsUntilNext());
    }

    @Test
    void outOfRangeSkillLevelIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> calculator.calculate(Map.of(SkillId.FISHING, 101)));
    }

    private static EnumMap<SkillId, Integer> allAt(int level) {
        EnumMap<SkillId, Integer> levels = new EnumMap<>(SkillId.class);
        for (SkillId skill : SkillId.gameplaySkills()) {
            levels.put(skill, level);
        }
        return levels;
    }
}
