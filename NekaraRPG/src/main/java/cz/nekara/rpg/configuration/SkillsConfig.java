package cz.nekara.rpg.configuration;

public record SkillsConfig(
    String databaseFile,
    long baseExperience,
    long linearGrowth,
    long quadraticGrowth,
    NativeMiningConfig mining,
    NativeGatheringConfig woodcutting,
    NativeGatheringConfig digging,
    GatheringAbilityConfig veinMining,
    GatheringAbilityConfig drilling,
    GatheringAbilityConfig treeFeller,
    NativeActivityConfig activities
) {
}
