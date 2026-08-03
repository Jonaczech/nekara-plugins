package cz.nekara.rpg.configuration;

public record SkillsConfig(
    String databaseFile,
    long baseExperience,
    long linearGrowth,
    long quadraticGrowth
) {
}
