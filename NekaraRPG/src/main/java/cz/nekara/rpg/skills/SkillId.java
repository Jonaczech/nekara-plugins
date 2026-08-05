package cz.nekara.rpg.skills;

import java.util.Arrays;
import java.util.List;

public enum SkillId {
    POWER("power", false),
    MARTIAL_ARTS("martial_arts", true),
    TRADING("trading", true),
    SMITHING("smithing", true),
    ENCHANTING("runotepectvi", true),
    ALCHEMY("alchemy", true),
    MINING("tezba", true),
    WOODCUTTING("lesnictvi", true),
    DIGGING("kopani", true),
    FARMING("statkarstvi", true),
    FISHING("rybareni", true),
    LIGHT_WEAPONS("lehke_zbrane", true),
    HEAVY_WEAPONS("heavy_weapons", true),
    ARCHERY("archery", true),
    LIGHT_ARMOR("light_armor", true),
    HEAVY_ARMOR("heavy_armor", true);

    private static final List<SkillId> GAMEPLAY_SKILLS = Arrays.stream(values())
        .filter(SkillId::gainsExperience)
        .toList();
    private static final List<SkillId> ACTIVE_GAMEPLAY_SKILLS = GAMEPLAY_SKILLS.stream()
        .filter(SkillId::isActive)
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

    /** Kept for backwards-compatible profile storage while the path is parked. */
    public boolean isActive() {
        return this != MARTIAL_ARTS && this != TRADING;
    }

    public static List<SkillId> gameplaySkills() {
        return GAMEPLAY_SKILLS;
    }

    public static List<SkillId> activeGameplaySkills() {
        return ACTIVE_GAMEPLAY_SKILLS;
    }

    public static java.util.Map<String, String> renamedIds() {
        return java.util.Map.of(
            "enchanting", "runotepectvi",
            "mining", "tezba",
            "woodcutting", "lesnictvi",
            "digging", "kopani",
            "farming", "statkarstvi",
            "fishing", "rybareni",
            "light_weapons", "lehke_zbrane");
    }
}
