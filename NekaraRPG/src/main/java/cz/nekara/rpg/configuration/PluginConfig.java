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
        FishingDifficultyConfig fishingDifficulty,
        FishingConfig fishing,
        SittingConfig sitting,
        CampfireConfig campfire,
        AuthConfig auth,
        EchoVeinConfig echoVein,
        MountConfig mounts,
        SkillsConfig skills,
        UpdaterConfig updater,
        WorldConfig worlds,
        Map<String, SoundSettings> sounds
) {
    public boolean isModuleEnabled(String moduleId) {
        return modules.getOrDefault(moduleId, true);
    }
}
