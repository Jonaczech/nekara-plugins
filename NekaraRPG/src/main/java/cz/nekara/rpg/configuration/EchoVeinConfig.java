package cz.nekara.rpg.configuration;

import org.bukkit.Particle;

public record EchoVeinConfig(
        double triggerChance,
        int cooldownSeconds,
        int durationTicks,
        int searchRadius,
        int pulseIntervalTicks,
        double experienceBonusMultiplier,
        boolean bonusDropEnabled,
        Particle particle,
        int particleCount,
        double particleSpread
) {
}
