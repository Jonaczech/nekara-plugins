package cz.nekara.rpg.configuration;

/**
 * Small innate rewards earned from a skill level. They deliberately remain
 * below perk rewards, so level progression enriches a build instead of
 * replacing the perk tree.
 */
public record LevelRewardConfig(
    double gatheringDoubleDropChancePerLevel,
    double gatheringDoubleDropMaximumChance,
    double diggingRareDropChancePerLevel,
    double diggingRareDropMaximumChance,
    double fishingTreasureBaseChance,
    double fishingTreasureChancePerLevel,
    double fishingTreasureMaximumChance
) {
    public LevelRewardConfig {
        validate(gatheringDoubleDropChancePerLevel, gatheringDoubleDropMaximumChance,
            "gathering double-drop");
        validate(diggingRareDropChancePerLevel, diggingRareDropMaximumChance,
            "digging rare-drop");
        validate(fishingTreasureBaseChance, fishingTreasureMaximumChance,
            "fishing treasure base");
        validate(fishingTreasureChancePerLevel, fishingTreasureMaximumChance - fishingTreasureBaseChance,
            "fishing treasure");
    }

    public double gatheringDoubleDropChance(int level) {
        return capped(level, gatheringDoubleDropChancePerLevel, gatheringDoubleDropMaximumChance);
    }

    public double diggingRareDropChance(int level) {
        return capped(level, diggingRareDropChancePerLevel, diggingRareDropMaximumChance);
    }

    public double fishingTreasureChance(int level) {
        return Math.min(fishingTreasureMaximumChance,
            fishingTreasureBaseChance + Math.max(0, level) * fishingTreasureChancePerLevel);
    }

    private static double capped(int level, double perLevel, double maximum) {
        return Math.min(maximum, Math.max(0, level) * perLevel);
    }

    private static void validate(double perLevel, double maximum, String label) {
        if (!Double.isFinite(perLevel) || !Double.isFinite(maximum)
            || perLevel < 0 || maximum < 0 || maximum > 1 || perLevel > maximum) {
            throw new IllegalArgumentException("Invalid " + label + " level reward");
        }
    }
}
