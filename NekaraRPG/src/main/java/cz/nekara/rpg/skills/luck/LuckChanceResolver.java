package cz.nekara.rpg.skills.luck;

/** Applies the small, server-configured global Luck bonus to Nekara rare-loot rolls only. */
public final class LuckChanceResolver {
    private LuckChanceResolver() {
    }

    public static double rareLootChance(double baseChance, double luckPoints, int maximumPoints,
                                        double bonusPerPoint) {
        double base = clamp(baseChance);
        double points = Math.max(0.0, Math.min(luckPoints, Math.max(0, maximumPoints)));
        return clamp(base + points * Math.max(0.0, bonusPerPoint));
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
