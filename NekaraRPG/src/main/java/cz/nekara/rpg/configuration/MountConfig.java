package cz.nekara.rpg.configuration;

import org.bukkit.Material;

import java.util.Set;

public record MountConfig(
        String storageFile,
        String databaseFile,
        long deathCooldownSeconds,
        long combatTagSeconds,
        long summonCooldownSeconds,
        long activeRecallCooldownSeconds,
        int activeTeleportDistanceChunks,
        int minimumSpawnDistance,
        int maximumSpawnDistance,
        double waitingRadius,
        double wanderingRadius,
        double pathfindingSpeed,
        long autosavePeriodTicks,
        boolean recallOnQuit,
        Set<String> allowedWorlds,
        double defaultMaxHealth,
        double defaultMovementSpeed,
        double defaultJumpStrength,
        int minimumNameLength,
        int maximumNameLength,
        Material whistleMaterial,
        int whistleCustomModelData
) {
    public boolean isWorldAllowed(String worldName) {
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName.toLowerCase(java.util.Locale.ROOT));
    }
}
