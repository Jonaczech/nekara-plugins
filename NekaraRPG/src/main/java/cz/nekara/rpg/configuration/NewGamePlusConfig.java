package cz.nekara.rpg.configuration;

public record NewGamePlusConfig(
    boolean enabled,
    double experienceMultiplier,
    double perkStatBonusPerRank,
    double innateGatheringDoubleDropMultiplierPerRank,
    double farmingAndButcheryBonusDropChance,
    double woodcuttingBonusDropChance,
    double diggingBonusDropChance
) {
}
