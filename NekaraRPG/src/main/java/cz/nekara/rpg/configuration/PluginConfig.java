package cz.nekara.rpg.configuration;

import java.util.Map;

public record PluginConfig(
        String version,
        boolean debug,
        Map<String, Boolean> modules,
        MinigameConfig minigame,
        HookParticleConfig hookParticles,
        OutcomeEffectConfig successEffect,
        OutcomeEffectConfig failureEffect,
        ValhallaFishingConfig valhallaFishing,
        FishingConfig fishing,
        SittingConfig sitting,
        CampfireConfig campfire,
        WorldConfig worlds,
        Map<String, SoundSettings> sounds
) {
    public boolean isModuleEnabled(String moduleId) {
        return modules.getOrDefault(moduleId, true);
    }
}
