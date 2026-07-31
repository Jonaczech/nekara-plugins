package cz.nekara.fishing.configuration;

import java.util.Map;

public record PluginConfig(
        String version,
        boolean debug,
        MinigameConfig minigame,
        HookParticleConfig hookParticles,
        OutcomeEffectConfig successEffect,
        OutcomeEffectConfig failureEffect,
        ValhallaFishingConfig valhallaFishing,
        FishingConfig fishing,
        WorldConfig worlds,
        Map<String, SoundSettings> sounds
) {
}
