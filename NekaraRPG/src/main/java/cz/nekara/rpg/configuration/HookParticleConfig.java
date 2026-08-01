package cz.nekara.rpg.configuration;

import org.bukkit.Particle;

public record HookParticleConfig(
        boolean enabled,
        Particle particle,
        double radius,
        int count,
        int intervalTicks,
        double yOffset
) {
}
