package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;

/** Coordinates form compact silhouettes in the scrollable perk viewport. */
public record PerkTreeLayout(
    PerkPosition root,
    PerkPosition left,
    PerkPosition right,
    PerkPosition leftDeep,
    PerkPosition rightDeep,
    PerkPosition crown,
    PerkPosition newGamePlus
) {
    private static final PerkTreeLayout FIST = layout(4, 9, 2, 7, 6, 7, 1, 4, 7, 4, 4, 2);
    private static final PerkTreeLayout COIN = layout(4, 9, 2, 7, 6, 7, 1, 5, 7, 5, 4, 3);
    private static final PerkTreeLayout ANVIL = layout(4, 9, 1, 7, 7, 7, 2, 4, 6, 4, 4, 1);
    private static final PerkTreeLayout RUNE = layout(4, 8, 1, 6, 7, 6, 2, 3, 6, 3, 4, 1);
    private static final PerkTreeLayout FLASK = layout(4, 9, 1, 7, 7, 7, 3, 5, 5, 5, 4, 2);
    private static final PerkTreeLayout PICKAXE = layout(4, 9, 1, 7, 7, 7, 1, 4, 7, 4, 4, 2);
    private static final PerkTreeLayout AXE = layout(3, 9, 1, 7, 5, 7, 1, 4, 6, 4, 4, 2);
    private static final PerkTreeLayout SHOVEL = layout(4, 9, 2, 7, 6, 7, 3, 4, 5, 4, 4, 1);
    private static final PerkTreeLayout WHEAT = layout(4, 9, 1, 7, 7, 7, 3, 4, 5, 4, 4, 1);
    private static final PerkTreeLayout FISHING_ROD = layout(2, 8, 5, 8, 5, 6, 5, 4, 7, 4, 7, 1);
    private static final PerkTreeLayout SWORD = layout(4, 9, 2, 7, 6, 7, 2, 4, 6, 4, 4, 1);
    private static final PerkTreeLayout HAMMER = layout(4, 7, 0, 7, 8, 7, 2, 4, 6, 4, 4, 1);
    private static final PerkTreeLayout BOW = layout(4, 8, 1, 6, 7, 6, 0, 2, 8, 2, 4, 1);
    private static final PerkTreeLayout CLOAK = layout(4, 9, 1, 7, 7, 7, 2, 5, 6, 5, 4, 2);
    private static final PerkTreeLayout SHIELD = layout(4, 9, 1, 7, 7, 7, 2, 4, 6, 4, 4, 2);

    private static PerkTreeLayout layout(
        int rootColumn, int rootRow,
        int leftColumn, int leftRow,
        int rightColumn, int rightRow,
        int leftDeepColumn, int leftDeepRow,
        int rightDeepColumn, int rightDeepRow,
        int crownColumn, int crownRow
    ) {
        PerkPosition root = new PerkPosition(rootColumn, rootRow);
        return new PerkTreeLayout(
            root,
            new PerkPosition(leftColumn, leftRow),
            new PerkPosition(rightColumn, rightRow),
            new PerkPosition(leftDeepColumn, leftDeepRow),
            new PerkPosition(rightDeepColumn, rightDeepRow),
            new PerkPosition(crownColumn, crownRow),
            new PerkPosition(rootColumn + 1, rootRow));
    }

    public static PerkTreeLayout forSkill(SkillId skill) {
        return switch (skill) {
            case MARTIAL_ARTS -> FIST;
            case TRADING -> COIN;
            case SMITHING -> ANVIL;
            case ENCHANTING -> RUNE;
            case ALCHEMY -> FLASK;
            case MINING -> PICKAXE;
            case WOODCUTTING -> AXE;
            case DIGGING -> SHOVEL;
            case FARMING -> WHEAT;
            case FISHING -> FISHING_ROD;
            case LIGHT_WEAPONS -> SWORD;
            case HEAVY_WEAPONS -> HAMMER;
            case ARCHERY -> BOW;
            case LIGHT_ARMOR -> CLOAK;
            case HEAVY_ARMOR -> SHIELD;
            case POWER -> throw new IllegalArgumentException("Power has no perk tree");
        };
    }
}
