package cz.nekara.rpg.configuration;

public record NewGamePlusConfig(
    boolean enabled,
    double experienceMultiplier,
    double perkStatBonusPerRank
) {
}
