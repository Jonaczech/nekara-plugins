package cz.nekara.rpg.configuration;

import org.bukkit.Particle;

public record EchoVeinConfig(
        double triggerChance,
        double chainChance,
        int durationTicks,
        int searchRadius,
        int pulseIntervalTicks,
        double experienceBonusMultiplier,
        boolean bonusDropEnabled,
        double oreRevealChance,
        Particle particle,
        int particleCount,
        double particleSpread
) {
}
