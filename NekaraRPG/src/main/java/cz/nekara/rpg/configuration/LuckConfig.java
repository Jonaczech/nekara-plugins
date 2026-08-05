package cz.nekara.rpg.configuration;

/** Balance values for the global Luck stat. */
public record LuckConfig(int maximumPoints, double rareLootChanceBonusPerPoint) {
    public LuckConfig {
        if (maximumPoints < 0 || rareLootChanceBonusPerPoint < 0.0) {
            throw new IllegalArgumentException("Luck values cannot be negative");
        }
    }

    public static LuckConfig defaults() {
        return new LuckConfig(2, 0.02);
    }
}
