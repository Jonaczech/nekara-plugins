package cz.nekara.rpg.configuration;

import java.util.Locale;
import java.util.Set;

public record DragonConfig(
        Set<String> allowedWorlds,
        int summonCooldownSeconds,
        int activeRecallCooldownSeconds,
        int activeTeleportDistanceChunks,
        int minimumSpawnDistance,
        int maximumSpawnDistance,
        int minimumSpawnHeight,
        int maximumSpawnHeight,
        double flyingSpeed,
        double maxHealth,
        int maximumAltitude
) {
    public DragonConfig {
        allowedWorlds = Set.copyOf(allowedWorlds);
    }

    public boolean isWorldAllowed(String worldName) {
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }
}
