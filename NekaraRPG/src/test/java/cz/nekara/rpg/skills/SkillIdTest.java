package cz.nekara.rpg.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillIdTest {
    @Test
    void catalogContainsOneDerivedAndFifteenGameplaySkills() {
        assertEquals(16, SkillId.values().length);
        assertEquals(15, SkillId.gameplaySkills().size());
        assertEquals(13, SkillId.activeGameplaySkills().size());
        assertFalse(SkillId.MARTIAL_ARTS.isActive());
        assertFalse(SkillId.TRADING.isActive());
        assertFalse(SkillId.POWER.gainsExperience());
        assertFalse(SkillId.gameplaySkills().contains(SkillId.POWER));
        assertTrue(SkillId.gameplaySkills().stream().allMatch(SkillId::gainsExperience));
    }
}
