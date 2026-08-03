package cz.nekara.rpg.configuration;

import cz.nekara.rpg.skills.SkillId;

import java.util.EnumMap;
import java.util.Map;

public record NativeActivityConfig(
    boolean enabled,
    int deduplicationMillis,
    Map<SkillId, Long> experienceBySkill
) {
    public NativeActivityConfig {
        experienceBySkill = Map.copyOf(experienceBySkill);
    }

    public long experience(SkillId skill) {
        return experienceBySkill.getOrDefault(skill, 0L);
    }

    public static Map<SkillId, Long> defaults() {
        EnumMap<SkillId, Long> values = new EnumMap<>(SkillId.class);
        values.put(SkillId.MARTIAL_ARTS, 4L);
        values.put(SkillId.TRADING, 10L);
        values.put(SkillId.SMITHING, 12L);
        values.put(SkillId.ENCHANTING, 15L);
        values.put(SkillId.ALCHEMY, 10L);
        values.put(SkillId.FARMING, 4L);
        values.put(SkillId.FISHING, 12L);
        values.put(SkillId.LIGHT_WEAPONS, 4L);
        values.put(SkillId.HEAVY_WEAPONS, 5L);
        values.put(SkillId.ARCHERY, 5L);
        values.put(SkillId.LIGHT_ARMOR, 3L);
        values.put(SkillId.HEAVY_ARMOR, 3L);
        return Map.copyOf(values);
    }
}
