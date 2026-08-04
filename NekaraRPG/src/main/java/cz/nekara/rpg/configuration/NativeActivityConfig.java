package cz.nekara.rpg.configuration;

import cz.nekara.rpg.skills.SkillId;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

public record NativeActivityConfig(
    boolean enabled,
    int deduplicationMillis,
    Map<SkillId, Map<String, Long>> experienceBySkill
) {
    public NativeActivityConfig {
        EnumMap<SkillId, Map<String, Long>> normalized = new EnumMap<>(SkillId.class);
        experienceBySkill.forEach((skill, sources) -> normalized.put(skill, Map.copyOf(sources)));
        experienceBySkill = Map.copyOf(normalized);
    }

    public long experience(SkillId skill, String source) {
        return experienceBySkill.getOrDefault(skill, Map.of()).getOrDefault(source, 0L);
    }

    public static Map<SkillId, Map<String, Long>> defaults() {
        EnumMap<SkillId, Map<String, Long>> values = new EnumMap<>(SkillId.class);
        put(values, SkillId.MARTIAL_ARTS, "combat_hit", 4);
        put(values, SkillId.TRADING, "villager_trade", 10);
        put(values, SkillId.SMITHING, "equipment_craft", 12, "smithing_table", 12);
        put(values, SkillId.ENCHANTING, "enchant_item", 15);
        put(values, SkillId.ALCHEMY, "brew_complete", 10);
        put(values, SkillId.FARMING, "mature_harvest", 4, "berry_harvest", 3,
            "wild_flower", 2, "wild_mushroom", 2, "grass_bundle", 1,
            "animal_breed", 8, "animal_shear", 3, "bonemeal_growth", 2);
        put(values, SkillId.FISHING, "vanilla_catch", 12, "deferred_catch", 12);
        put(values, SkillId.LIGHT_WEAPONS, "combat_hit", 4);
        put(values, SkillId.HEAVY_WEAPONS, "combat_hit", 5);
        put(values, SkillId.ARCHERY, "combat_hit", 5);
        put(values, SkillId.LIGHT_ARMOR, "armor_hit", 3);
        put(values, SkillId.HEAVY_ARMOR, "armor_hit", 3);
        return Map.copyOf(values);
    }

    private static void put(EnumMap<SkillId, Map<String, Long>> values, SkillId skill, Object... pairs) {
        Map<String, Long> sources = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            sources.put((String) pairs[index], ((Number) pairs[index + 1]).longValue());
        }
        values.put(skill, Map.copyOf(sources));
    }
}
