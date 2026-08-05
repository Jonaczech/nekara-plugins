package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;

/** Coordinates deliberately form small thematic silhouettes in the scrollable perk viewport. */
public record PerkTreeLayout(
    PerkPosition root,
    PerkPosition left,
    PerkPosition right,
    PerkPosition leftDeep,
    PerkPosition rightDeep,
    PerkPosition crown,
    PerkPosition newGamePlus
) {
    private static final PerkTreeLayout DEFAULT = new PerkTreeLayout(
        new PerkPosition(4, 9), new PerkPosition(1, 7), new PerkPosition(7, 7),
        new PerkPosition(1, 3), new PerkPosition(7, 3), new PerkPosition(4, 1),
        new PerkPosition(7, 9));
    private static final PerkTreeLayout HAMMER = new PerkTreeLayout(
        new PerkPosition(4, 7), new PerkPosition(0, 7), new PerkPosition(8, 7),
        new PerkPosition(2, 4), new PerkPosition(6, 4), new PerkPosition(4, 1),
        new PerkPosition(7, 7));
    private static final PerkTreeLayout ANVIL = new PerkTreeLayout(
        new PerkPosition(4, 9), new PerkPosition(1, 7), new PerkPosition(7, 7),
        new PerkPosition(1, 3), new PerkPosition(7, 3), new PerkPosition(4, 1),
        new PerkPosition(7, 9));
    private static final PerkTreeLayout BOW = new PerkTreeLayout(
        new PerkPosition(4, 8), new PerkPosition(1, 6), new PerkPosition(7, 6),
        new PerkPosition(0, 2), new PerkPosition(8, 2), new PerkPosition(4, 1),
        new PerkPosition(7, 8));
    private static final PerkTreeLayout FISHING_ROD = new PerkTreeLayout(
        new PerkPosition(2, 8), new PerkPosition(5, 8), new PerkPosition(5, 6),
        new PerkPosition(5, 4), new PerkPosition(7, 4), new PerkPosition(7, 1),
        new PerkPosition(0, 8));
    private static final PerkTreeLayout WHEAT = new PerkTreeLayout(
        new PerkPosition(4, 9), new PerkPosition(1, 7), new PerkPosition(7, 7),
        new PerkPosition(3, 4), new PerkPosition(5, 4), new PerkPosition(4, 1),
        new PerkPosition(7, 9));

    public static PerkTreeLayout forSkill(SkillId skill) {
        return switch (skill) {
            case HEAVY_WEAPONS -> HAMMER;
            case SMITHING -> ANVIL;
            case ARCHERY -> BOW;
            case FISHING -> FISHING_ROD;
            case FARMING -> WHEAT;
            default -> DEFAULT;
        };
    }
}
