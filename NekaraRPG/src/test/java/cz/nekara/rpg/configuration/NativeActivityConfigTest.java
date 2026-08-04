package cz.nekara.rpg.configuration;

import cz.nekara.rpg.skills.SkillId;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeActivityConfigTest {
    @Test
    void defaultsCoverEveryNonGatheringGameplaySkill() {
        var defaults = NativeActivityConfig.defaults();
        assertEquals(12, defaults.size());
        assertFalse(defaults.containsKey(SkillId.POWER));
        assertFalse(defaults.containsKey(SkillId.MINING));
        assertFalse(defaults.containsKey(SkillId.WOODCUTTING));
        assertFalse(defaults.containsKey(SkillId.DIGGING));
        assertEquals(3L, defaults.get(SkillId.FARMING).get("berry_harvest"));
        assertEquals(2L, defaults.get(SkillId.FARMING).get("wild_flower"));
        assertEquals(1L, defaults.get(SkillId.FARMING).get("grass_bundle"));
    }

    @Test
    void constructorDefensivelyCopiesExperienceTable() {
        EnumMap<SkillId, Map<String, Long>> values = new EnumMap<>(SkillId.class);
        values.put(SkillId.FARMING, Map.of("mature_harvest", 4L));
        NativeActivityConfig config = new NativeActivityConfig(true, 750, values);
        values.put(SkillId.FARMING, Map.of("mature_harvest", 99L));
        assertEquals(4L, config.experience(SkillId.FARMING, "mature_harvest"));
        assertThrows(UnsupportedOperationException.class,
            () -> config.experienceBySkill().put(SkillId.FISHING, Map.of()));
    }
}
