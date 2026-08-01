package cz.nekara.rpg.configuration;

import org.bukkit.Particle;

public record CampfireVisualConfig(
        boolean restingParticlesEnabled,
        Particle restingParticle,
        int restingParticleCount,
        double restingParticleRadius,
        double restingParticleYOffset,
        boolean restedActionBarEnabled,
        boolean restedParticlesEnabled,
        Particle restedParticle,
        int restedParticleCount,
        double restedParticleRadius,
        double restedParticleYOffset
) {
}
