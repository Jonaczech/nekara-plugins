package cz.nekara.rpg.configuration;

import org.bukkit.Particle;

public record OutcomeEffectConfig(
        boolean enabled,
        Particle particle,
        int count,
        int durationTicks,
        double radius,
        double yOffset
) {
}
