package cz.nekara.rpg.configuration;

public record GatheringAbilityConfig(
    boolean enabled,
    int maximumBlocks,
    int blocksPerTick,
    int durationSeconds,
    int cooldownSeconds
) {
    public GatheringAbilityConfig {
        if (maximumBlocks < 1 || maximumBlocks > 512) {
            throw new IllegalArgumentException("Ability block limit must be between 1 and 512");
        }
        if (blocksPerTick < 1 || blocksPerTick > maximumBlocks) {
            throw new IllegalArgumentException("Ability tick budget must be positive and within its block limit");
        }
        if (durationSeconds < 0 || durationSeconds > 3_600) {
            throw new IllegalArgumentException("Ability duration must be between 0 and 3600 seconds");
        }
        if (cooldownSeconds < 0 || cooldownSeconds > 86_400) {
            throw new IllegalArgumentException("Ability cooldown must be between 0 and 86400 seconds");
        }
    }
}
