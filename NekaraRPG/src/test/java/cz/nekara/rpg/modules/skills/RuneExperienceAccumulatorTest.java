package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cz.nekara.rpg.skills.SkillId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RuneExperienceAccumulatorTest {
    @Test
    void preservesOnePercentBonusesFromSmallAwards() {
        RuneExperienceAccumulator accumulator = new RuneExperienceAccumulator();
        UUID playerId = UUID.randomUUID();
        long claimed = 0L;
        for (int action = 0; action < 50; action++) {
            claimed += accumulator.claim(playerId, SkillId.MINING, 0.02);
        }
        assertEquals(1L, claimed);
    }

    @Test
    void keepsFractionsSeparateBySkillAndPlayer() {
        RuneExperienceAccumulator accumulator = new RuneExperienceAccumulator();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertEquals(0L, accumulator.claim(first, SkillId.MINING, 0.75));
        assertEquals(0L, accumulator.claim(first, SkillId.WOODCUTTING, 0.75));
        assertEquals(0L, accumulator.claim(second, SkillId.MINING, 0.75));
        assertEquals(1L, accumulator.claim(first, SkillId.MINING, 0.25));
    }
}
