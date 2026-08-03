package cz.nekara.rpg.skills;

import java.util.Arrays;
import java.util.List;

public enum SkillId {
    POWER("power", false),
    MARTIAL_ARTS("martial_arts", true),
    TRADING("trading", true),
    SMITHING("smithing", true),
    ENCHANTING("enchanting", true),
    ALCHEMY("alchemy", true),
    MINING("mining", true),
    WOODCUTTING("woodcutting", true),
    DIGGING("digging", true),
    FARMING("farming", true),
    FISHING("fishing", true),
    LIGHT_WEAPONS("light_weapons", true),
    HEAVY_WEAPONS("heavy_weapons", true),
    ARCHERY("archery", true),
    LIGHT_ARMOR("light_armor", true),
    HEAVY_ARMOR("heavy_armor", true);

    private static final List<SkillId> GAMEPLAY_SKILLS = Arrays.stream(values())
        .filter(SkillId::gainsExperience)
        .toList();

    private final String id;
    private final boolean gainsExperience;

    SkillId(String id, boolean gainsExperience) {
        this.id = id;
        this.gainsExperience = gainsExperience;
    }

    public String id() {
        return id;
    }

    public boolean gainsExperience() {
        return gainsExperience;
    }

    public static List<SkillId> gameplaySkills() {
        return GAMEPLAY_SKILLS;
    }
}
