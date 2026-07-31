package cz.nekara.fishing.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ConfigurationService {
    private final JavaPlugin plugin;
    private PluginConfig current;

    public ConfigurationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public PluginConfig reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        Consumer<String> warning = message -> plugin.getLogger().warning(message);

        int legacyRequiredHits = config.getInt("minigame.required-hits", -1);
        int requiredHitsMin = config.getInt("minigame.required-hits-min",
                legacyRequiredHits > 0 ? legacyRequiredHits : 3);
        int requiredHitsMax = config.getInt("minigame.required-hits-max",
                legacyRequiredHits > 0 ? legacyRequiredHits : 5);

        MinigameConfig rawMinigame = new MinigameConfig(
                config.getBoolean("minigame.enabled", true),
                parseDisplay(config.getString("minigame.display", "ACTION_BAR"), warning),
                config.getInt("minigame.bar-length", 20),
                config.getInt("minigame.update-period-ticks", 6),
                config.getInt("minigame.target-width", 5),
                config.getInt("minigame.target-relocation-max-distance", 6),
                requiredHitsMin,
                requiredHitsMax,
                config.getInt("minigame.max-misses", 1),
                config.getInt("minigame.timeout-ticks", 160),
                config.getInt("minigame.time-bonus-ticks", 30),
                validateDouble(config.getDouble("minigame.hook-pull-distance", 1.0),
                        0.0, 5.0, 1.0, "minigame.hook-pull-distance", warning),
                config.getBoolean("minigame.move-target-after-hit", true),
                config.getBoolean("minigame.randomize-start-position", true),
                ConfigurationValidator.parseDirection(config.getString("minigame.indicator-direction", "RANDOM"), warning),
                config.getLong("minigame.input-debounce-milliseconds", 150)
        );
        MinigameConfig minigame = ConfigurationValidator.validate(rawMinigame, warning);
        HookParticleConfig hookParticles = new HookParticleConfig(
                config.getBoolean("minigame.hook-particles.enabled", true),
                parseParticle(config.getString("minigame.hook-particles.particle", "SPLASH"), "SPLASH", warning),
                validateDouble(config.getDouble("minigame.hook-particles.radius", 0.8),
                        0.05, 5.0, 0.8, "minigame.hook-particles.radius", warning),
                validateInt(config.getInt("minigame.hook-particles.count", 8),
                        1, 32, 8, "minigame.hook-particles.count", warning),
                validateInt(config.getInt("minigame.hook-particles.interval-ticks", 4),
                        1, 100, 4, "minigame.hook-particles.interval-ticks", warning),
                validateDouble(config.getDouble("minigame.hook-particles.y-offset", 0.05),
                        -2.0, 2.0, 0.05, "minigame.hook-particles.y-offset", warning)
        );
        OutcomeEffectConfig successEffect = parseOutcomeEffect(
                config, "effects.success", "HAPPY_VILLAGER", warning);
        OutcomeEffectConfig failureEffect = parseOutcomeEffect(
                config, "effects.failure", "DAMAGE_INDICATOR", warning);
        ValhallaFishingConfig valhallaFishing = parseValhallaFishingConfig(config, warning);

        String preferredMode = config.getString("fishing.preferred-mode", "BITE_GATE");
        if ("BITE_GATE".equalsIgnoreCase(preferredMode)) {
            warning.accept("fishing.preferred-mode BITE_GATE is not reliable on this API; using DEFERRED_CATCH.");
        } else if (!"DEFERRED_CATCH".equalsIgnoreCase(preferredMode)) {
            warning.accept("fishing.preferred-mode '" + preferredMode + "' is not available on this API; using DEFERRED_CATCH.");
        }

        FishingConfig fishing = new FishingConfig(
                config.getBoolean("fishing.cancel-on-teleport", true),
                config.getBoolean("fishing.cancel-on-world-change", true),
                config.getBoolean("fishing.cancel-on-item-change", true),
                config.getBoolean("fishing.cancel-on-damage", false),
                config.getBoolean("fishing.require-fishing-rod-in-main-hand", true),
                config.getBoolean("fishing.allow-creative", false),
                config.getBoolean("fishing.allow-spectator", false)
        );

        WorldMode worldMode = ConfigurationValidator.parseWorldMode(
                config.getString("worlds.mode", "ALL"), warning);
        Set<String> worlds = new HashSet<>(config.getStringList("worlds.list"));
        WorldConfig worldConfig = new WorldConfig(worldMode, Set.copyOf(worlds));

        Map<String, SoundSettings> sounds = new HashMap<>();
        for (String key : new String[]{"bite", "hit", "miss", "timeout", "escape", "minigame-success", "catch-success"}) {
            ConfigurationSection section = config.getConfigurationSection("sounds." + key);
            if (section == null) {
                warning.accept("Missing sounds." + key + "; sound is disabled.");
                sounds.put(key, new SoundSettings(false, "", 1.0f, 1.0f));
                continue;
            }
            sounds.put(key, new SoundSettings(
                    section.getBoolean("enabled", true),
                    section.getString("sound", ""),
                    clampSoundNumber(section.getDouble("volume", 1.0), 1.0f, key, "volume", warning),
                    clampSoundNumber(section.getDouble("pitch", 1.0), 1.0f, key, "pitch", warning)
            ));
        }

        current = new PluginConfig(
                plugin.getDescription().getVersion(),
                config.getBoolean("plugin.debug", false),
                minigame,
                hookParticles,
                successEffect,
                failureEffect,
                valhallaFishing,
                fishing,
                worldConfig,
                Map.copyOf(sounds)
        );
        return current;
    }

    private DisplayMode parseDisplay(String value, Consumer<String> warning) {
        try {
            return DisplayMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            warning.accept("Invalid minigame.display; using ACTION_BAR.");
            return DisplayMode.ACTION_BAR;
        }
    }

    private ValhallaFishingConfig parseValhallaFishingConfig(
            FileConfiguration config, Consumer<String> warning) {
        String path = "valhalla.fishing-difficulty";
        int maxLevelRequiredHits = validateInt(config.getInt(path + ".max-level-required-hits", 2),
                1, 100, 2, path + ".max-level-required-hits", warning);
        int maxLevelMaxMisses = validateInt(config.getInt(path + ".max-level-max-misses", 3),
                0, 100, 3, path + ".max-level-max-misses", warning);

        List<ValhallaFishingTier> tiers = new ArrayList<>();
        parseValhallaTier(config, path + ".tiers.novice", "novice", 1, 30,
                3, 5, 1, tiers, warning);
        parseValhallaTier(config, path + ".tiers.skilled", "skilled", 31, 60,
                3, 4, 2, tiers, warning);
        parseValhallaTier(config, path + ".tiers.expert", "expert", 61, 0,
                2, 3, 3, tiers, warning);
        return new ValhallaFishingConfig(
                config.getBoolean(path + ".enabled", true),
                maxLevelRequiredHits,
                maxLevelMaxMisses,
                List.copyOf(tiers)
        );
    }

    private void parseValhallaTier(
            FileConfiguration config,
            String path,
            String name,
            int defaultMinLevel,
            int defaultMaxLevel,
            int defaultRequiredHitsMin,
            int defaultRequiredHitsMax,
            int defaultMaxMisses,
            List<ValhallaFishingTier> tiers,
            Consumer<String> warning
    ) {
        int minLevel = validateInt(config.getInt(path + ".min-level", defaultMinLevel),
                0, 100_000, defaultMinLevel, path + ".min-level", warning);
        int maxLevel = validateInt(config.getInt(path + ".max-level", defaultMaxLevel),
                0, 100_000, defaultMaxLevel, path + ".max-level", warning);
        if (maxLevel > 0 && maxLevel < minLevel) {
            warning.accept("Invalid " + path + ".max-level; using " + defaultMaxLevel + ".");
            maxLevel = defaultMaxLevel;
        }
        int requiredHitsMin = validateInt(config.getInt(path + ".required-hits-min", defaultRequiredHitsMin),
                1, 100, defaultRequiredHitsMin, path + ".required-hits-min", warning);
        int requiredHitsMax = validateInt(config.getInt(path + ".required-hits-max", defaultRequiredHitsMax),
                requiredHitsMin, 100, Math.max(requiredHitsMin, defaultRequiredHitsMax),
                path + ".required-hits-max", warning);
        int maxMisses = validateInt(config.getInt(path + ".max-misses", defaultMaxMisses),
                0, 100, defaultMaxMisses, path + ".max-misses", warning);
        tiers.add(new ValhallaFishingTier(name, minLevel, maxLevel,
                requiredHitsMin, requiredHitsMax, maxMisses));
    }

    private OutcomeEffectConfig parseOutcomeEffect(
            FileConfiguration config, String path, String defaultParticle, Consumer<String> warning) {
        return new OutcomeEffectConfig(
                config.getBoolean(path + ".enabled", true),
                parseParticle(config.getString(path + ".particle", defaultParticle), defaultParticle, warning),
                validateInt(config.getInt(path + ".count", 12),
                        1, 64, 12, path + ".count", warning),
                validateInt(config.getInt(path + ".duration-ticks", 8),
                        1, 40, 8, path + ".duration-ticks", warning),
                validateDouble(config.getDouble(path + ".radius", 0.9),
                        0.05, 5.0, 0.9, path + ".radius", warning),
                validateDouble(config.getDouble(path + ".y-offset", 0.05),
                        -2.0, 2.0, 0.05, path + ".y-offset", warning)
        );
    }

    private org.bukkit.Particle parseParticle(String value, String fallback, Consumer<String> warning) {
        try {
            return org.bukkit.Particle.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            warning.accept("Invalid particle name; using " + fallback + ".");
            return org.bukkit.Particle.valueOf(fallback);
        }
    }

    private int validateInt(int value, int minimum, int maximum, int fallback,
                            String path, Consumer<String> warning) {
        if (value < minimum || value > maximum) {
            warning.accept("Invalid " + path + "; using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    private double validateDouble(double value, double minimum, double maximum, double fallback,
                                  String path, Consumer<String> warning) {
        if (!Double.isFinite(value) || value < minimum || value > maximum) {
            warning.accept("Invalid " + path + "; using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    private float clampSoundNumber(double value, float fallback, String sound, String field, Consumer<String> warning) {
        if (!Double.isFinite(value) || value < 0.0 || value > 10.0) {
            warning.accept("Invalid sounds." + sound + "." + field + "; using " + fallback + ".");
            return fallback;
        }
        return (float) value;
    }

    public PluginConfig get() {
        if (current == null) {
            throw new IllegalStateException("Configuration has not been loaded.");
        }
        return current;
    }
}
