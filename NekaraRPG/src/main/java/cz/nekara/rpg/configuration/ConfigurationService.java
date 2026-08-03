package cz.nekara.rpg.configuration;

import cz.nekara.rpg.campfire.CampFeature;
import cz.nekara.rpg.echovein.EchoVeinMath;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
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
    private final ModuleConfigurationStore moduleConfigurations;
    private PluginConfig current;

    public ConfigurationService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.moduleConfigurations = new ModuleConfigurationStore(plugin);
    }

    public PluginConfig reload() {
        plugin.reloadConfig();
        FileConfiguration config = moduleConfigurations.reload(plugin.getConfig());
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

        SittingConfig sitting = new SittingConfig(
                config.getBoolean("sitting.require-ground", true),
                parseSeatYOffset(config, warning),
                config.getBoolean("sitting.allow-creative", true),
                config.getBoolean("sitting.allow-flying", false),
                config.getBoolean("sitting.stand-on-damage", true),
                config.getBoolean("sitting.detect-external-seats", true),
                parseEntityTypes(config, "sitting.external-seat-entity-types",
                        Set.of(EntityType.ARMOR_STAND), warning)
        );

        int configuredRestedDuration = config.getInt("campfire.rested.duration-seconds", 300);
        boolean migratePreReleaseRestedDefaults = configuredRestedDuration == 15
                && config.getBoolean("campfire.visuals.rested.boss-bar", true)
                && config.getBoolean("campfire.visuals.rested.particles.enabled", true);
        if (migratePreReleaseRestedDefaults) {
            warning.accept("Migrating pre-release Rested defaults to 300 seconds with the Rested timer.");
        }

        CampfireVisualConfig campfireVisuals = new CampfireVisualConfig(
                config.getBoolean("campfire.visuals.resting-particles.enabled", true),
                parseParticle(config.getString("campfire.visuals.resting-particles.particle", "HAPPY_VILLAGER"),
                        "HAPPY_VILLAGER", warning),
                validateInt(config.getInt("campfire.visuals.resting-particles.count", 4),
                        1, 64, 4, "campfire.visuals.resting-particles.count", warning),
                validateDouble(config.getDouble("campfire.visuals.resting-particles.radius", 0.7),
                        0.05, 5.0, 0.7, "campfire.visuals.resting-particles.radius", warning),
                validateDouble(config.getDouble("campfire.visuals.resting-particles.y-offset", 0.35),
                        -2.0, 3.0, 0.35, "campfire.visuals.resting-particles.y-offset", warning),
                parseRestedIndicator(config, warning),
                migratePreReleaseRestedDefaults
                        ? false : config.getBoolean("campfire.visuals.rested.particles.enabled", false),
                parseParticle(config.getString("campfire.visuals.rested.particles.particle", "END_ROD"),
                        "END_ROD", warning),
                validateInt(config.getInt("campfire.visuals.rested.particles.count", 2),
                        1, 64, 2, "campfire.visuals.rested.particles.count", warning),
                validateDouble(config.getDouble("campfire.visuals.rested.particles.radius", 0.55),
                        0.05, 5.0, 0.55, "campfire.visuals.rested.particles.radius", warning),
                validateDouble(config.getDouble("campfire.visuals.rested.particles.y-offset", 0.75),
                        -2.0, 3.0, 0.75, "campfire.visuals.rested.particles.y-offset", warning)
        );

        RestedEffectConfig restedEffect = new RestedEffectConfig(
                config.getBoolean("campfire.rested.haste.enabled", true),
                validateInt(config.getInt("campfire.rested.haste.amplifier", 0),
                        0, 9, 0, "campfire.rested.haste.amplifier", warning),
                config.getBoolean("campfire.rested.haste.ambient", true),
                config.getBoolean("campfire.rested.haste.particles", true),
                config.getBoolean("campfire.rested.haste.icon", true)
        );
        RestedValhallaConfig restedValhalla = new RestedValhallaConfig(
                config.getBoolean("campfire.rested.valhalla-experience.enabled", true),
                validateDouble(config.getDouble(
                                "campfire.rested.valhalla-experience.multiplier", 1.10),
                        1.0, 10.0, 1.10,
                        "campfire.rested.valhalla-experience.multiplier", warning)
        );

        String mythicHostileFaction = config.getString(
                "campfire.camping.spawn-protection.mythic-hostile-faction", "NekaraHostile");
        if (mythicHostileFaction == null || mythicHostileFaction.isBlank()) {
            warning.accept("Invalid campfire.camping.spawn-protection.mythic-hostile-faction; using NekaraHostile.");
            mythicHostileFaction = "NekaraHostile";
        }
        CampingConfig camping = new CampingConfig(
                validateDouble(config.getDouble("campfire.camping.feature-radius", 5.0),
                        1.0, 16.0, 5.0, "campfire.camping.feature-radius", warning),
                validateInt(config.getInt("campfire.camping.duration-per-feature-seconds", 60),
                        0, 3_600, 60, "campfire.camping.duration-per-feature-seconds", warning),
                parseCampFeatures(config, warning),
                config.getBoolean("campfire.camping.spawn-protection.enabled", true),
                validateDouble(config.getDouble("campfire.camping.spawn-protection.radius", 24.0),
                        1.0, 128.0, 24.0, "campfire.camping.spawn-protection.radius", warning),
                config.getBoolean("campfire.camping.spawn-protection.natural-only", true),
                mythicHostileFaction.trim()
        );

        CampfireConfig campfire = new CampfireConfig(
                validateDouble(config.getDouble("campfire.radius", 5.0),
                        1.0, 16.0, 5.0, "campfire.radius", warning),
                validateInt(config.getInt("campfire.update-period-ticks", 20),
                        1, 200, 20, "campfire.update-period-ticks", warning),
                validateDouble(config.getDouble("campfire.healing.amount", 1.0),
                        0.0, 20.0, 1.0, "campfire.healing.amount", warning),
                validateInt(config.getInt("campfire.healing.period-seconds", 5),
                        1, 300, 5, "campfire.healing.period-seconds", warning),
                validateInt(config.getInt("campfire.hunger.restore-amount", 1),
                        0, 20, 1, "campfire.hunger.restore-amount", warning),
                validateInt(config.getInt("campfire.hunger.restore-period-seconds", 10),
                        1, 300, 10, "campfire.hunger.restore-period-seconds", warning),
                validateInt(config.getInt("campfire.rested.charge-seconds", 20),
                        1, 3_600, 20, "campfire.rested.charge-seconds", warning),
                validateInt(migratePreReleaseRestedDefaults ? 300 : configuredRestedDuration,
                        1, 3_600, 300, "campfire.rested.duration-seconds", warning),
                validateDouble(config.getDouble("campfire.rested.hunger-loss-multiplier", 0.5),
                        0.0, 1.0, 0.5, "campfire.rested.hunger-loss-multiplier", warning),
                restedValhalla,
                restedEffect,
                camping,
                validateDouble(config.getDouble("campfire.group.multiplier-per-extra-player", 0.15),
                        0.0, 2.0, 0.15, "campfire.group.multiplier-per-extra-player", warning),
                validateDouble(config.getDouble("campfire.group.maximum-multiplier", 1.75),
                        1.0, 10.0, 1.75, "campfire.group.maximum-multiplier", warning),
                campfireVisuals
        );

        double configuredEchoVeinChance = config.getDouble("echo-vein.trigger-chance", 0.05);
        double migratedEchoVeinChance = EchoVeinMath.migratePreviousDefaultTriggerChance(
                configuredEchoVeinChance);
        boolean migratePreviousEchoVeinChance = Double.compare(
                configuredEchoVeinChance, migratedEchoVeinChance) != 0;
        if (migratePreviousEchoVeinChance) {
            warning.accept("Migrating the previous Echo Vein trigger default from 4% to 5%.");
        }
        EchoVeinConfig echoVein = new EchoVeinConfig(
                validateDouble(migratedEchoVeinChance,
                        0.0, 1.0, 0.05, "echo-vein.trigger-chance", warning),
                validateDouble(config.getDouble("echo-vein.chain-chance", 0.50),
                        0.0, 1.0, 0.50, "echo-vein.chain-chance", warning),
                validateInt(config.getInt("echo-vein.duration-ticks", 120),
                        20, 600, 120, "echo-vein.duration-ticks", warning),
                validateInt(config.getInt("echo-vein.search-radius", 4),
                        1, 8, 4, "echo-vein.search-radius", warning),
                validateInt(config.getInt("echo-vein.pulse-interval-ticks", 10),
                        1, 40, 10, "echo-vein.pulse-interval-ticks", warning),
                validateDouble(config.getDouble("echo-vein.experience-bonus-multiplier", 0.25),
                        0.0, 5.0, 0.25, "echo-vein.experience-bonus-multiplier", warning),
                config.getBoolean("echo-vein.bonus-drop-enabled", true),
                validateDouble(config.getDouble("echo-vein.ore-reveal.chance", 0.25),
                        0.0, 1.0, 0.25, "echo-vein.ore-reveal.chance", warning),
                parseDataFreeParticle(config.getString("echo-vein.particles.particle", "END_ROD"),
                        "END_ROD", "echo-vein.particles.particle", warning),
                validateInt(config.getInt("echo-vein.particles.count", 8),
                        1, 64, 8, "echo-vein.particles.count", warning),
                validateDouble(config.getDouble("echo-vein.particles.spread", 0.35),
                        0.0, 2.0, 0.35, "echo-vein.particles.spread", warning)
        );

        WorldMode worldMode = ConfigurationValidator.parseWorldMode(
                config.getString("worlds.mode", "ALL"), warning);
        Set<String> worlds = new HashSet<>(config.getStringList("worlds.list"));
        WorldConfig worldConfig = new WorldConfig(worldMode, Set.copyOf(worlds));

        if (config.contains("plugin.check-for-updates") && !config.contains("updater.enabled")) {
            warning.accept("plugin.check-for-updates is obsolete; the new verified updater defaults to enabled. "
                    + "Use updater.enabled to control it.");
        }
        UpdaterConfig updater = new UpdaterConfig(
                config.getBoolean("updater.enabled", true),
                config.getBoolean("updater.automatic-checks", true),
                config.getBoolean("updater.auto-download", true),
                config.getBoolean("updater.notify-admins", true),
                validateInt(config.getInt("updater.startup-delay-seconds", 30),
                        0, 3_600, 30, "updater.startup-delay-seconds", warning),
                validateInt(config.getInt("updater.check-interval-hours", 6),
                        1, 168, 6, "updater.check-interval-hours", warning),
                validateInt(config.getInt("updater.request-timeout-seconds", 20),
                        5, 120, 20, "updater.request-timeout-seconds", warning),
                validateInt(config.getInt("updater.maximum-jar-size-megabytes", 16),
                        1, 256, 16, "updater.maximum-jar-size-megabytes", warning)
        );

        String authStorageFile = config.getString("auth.storage.file", "auth/accounts.yml");
        if (authStorageFile == null || authStorageFile.isBlank()
                || authStorageFile.contains("..") || authStorageFile.startsWith("/")
                || authStorageFile.startsWith("\\") || authStorageFile.matches("^[A-Za-z]:.*")) {
            warning.accept("Invalid auth.storage.file; using auth/accounts.yml.");
            authStorageFile = "auth/accounts.yml";
        }
        int authPasswordMinimumLength = validateInt(
                config.getInt("auth.password.minimum-length", 8),
                6, 64, 8, "auth.password.minimum-length", warning);
        int authPasswordMaximumLength = validateInt(
                config.getInt("auth.password.maximum-length", 64),
                authPasswordMinimumLength, 128, Math.max(64, authPasswordMinimumLength),
                "auth.password.maximum-length", warning);
        AuthConfig auth = new AuthConfig(
                authStorageFile.replace('\\', '/'),
                authPasswordMinimumLength,
                authPasswordMaximumLength,
                validateInt(config.getInt("auth.password.pbkdf2-iterations", 600_000),
                        100_000, 5_000_000, 600_000, "auth.password.pbkdf2-iterations", warning),
                validateInt(config.getInt("auth.login.maximum-attempts", 5),
                        1, 20, 5, "auth.login.maximum-attempts", warning),
                validateInt(config.getInt("auth.login.lockout-seconds", 60),
                        5, 3_600, 60, "auth.login.lockout-seconds", warning),
                validateInt(config.getInt("auth.login.timeout-seconds", 120),
                        15, 3_600, 120, "auth.login.timeout-seconds", warning),
                config.getBoolean("auth.session.enabled", true),
                validateInt(config.getInt("auth.session.duration-seconds", 600),
                        30, 86_400, 600, "auth.session.duration-seconds", warning),
                config.getBoolean("auth.commands.fallback-enabled", false),
                config.getBoolean("auth.nickname.exact-case", true),
                config.getBoolean("auth.gui.open-on-join", true)
        );

        String mountStorageFile = validateRelativeStoragePath(
                config.getString("mounts.storage.file", "mounts/data.yml"),
                "mounts.storage.file", "mounts/data.yml", warning);
        String mountDatabaseFile = validateRelativeStoragePath(
                config.getString("mounts.storage.database-file", "mounts/data.db"),
                "mounts.storage.database-file", "mounts/data.db", warning);
        Set<String> mountWorlds = new HashSet<>();
        for (String world : config.getStringList("mounts.summoning.allowed-worlds")) {
            if (world != null && !world.isBlank()) {
                mountWorlds.add(world.trim().toLowerCase(Locale.ROOT));
            }
        }
        int mountMinimumNameLength = validateInt(config.getInt("mounts.naming.minimum-length", 2),
                1, 16, 2, "mounts.naming.minimum-length", warning);
        int mountMaximumNameLength = validateInt(config.getInt("mounts.naming.maximum-length", 24),
                2, 64, 24, "mounts.naming.maximum-length", warning);
        if (mountMinimumNameLength > mountMaximumNameLength) {
            warning.accept("Mount minimum name length exceeds maximum; using defaults 2 and 24.");
            mountMinimumNameLength = 2;
            mountMaximumNameLength = 24;
        }
        MountConfig mounts = new MountConfig(
                mountStorageFile,
                mountDatabaseFile,
                validateInt(config.getInt("mounts.death.cooldown-seconds", 60),
                        1, 2_592_000, 60, "mounts.death.cooldown-seconds", warning),
                validateInt(config.getInt("mounts.combat.block-seconds", 15),
                        1, 600, 15, "mounts.combat.block-seconds", warning),
                validateInt(config.getInt("mounts.summoning.cooldown-seconds", 30),
                        1, 3_600, 30, "mounts.summoning.cooldown-seconds", warning),
                validateInt(config.getInt("mounts.summoning.active-recall-cooldown-seconds", 3),
                        1, 60, 3, "mounts.summoning.active-recall-cooldown-seconds", warning),
                validateInt(config.getInt("mounts.summoning.minimum-spawn-distance", 7),
                        2, 32, 7, "mounts.summoning.minimum-spawn-distance", warning),
                validateInt(config.getInt("mounts.summoning.maximum-spawn-distance", 12),
                        3, 48, 12, "mounts.summoning.maximum-spawn-distance", warning),
                validateDouble(config.getDouble("mounts.summoning.waiting-radius", 3.0),
                        1.0, 16.0, 3.0, "mounts.summoning.waiting-radius", warning),
                validateDouble(config.getDouble("mounts.summoning.wandering-radius", 5.0),
                        2.0, 16.0, 5.0, "mounts.summoning.wandering-radius", warning),
                validateDouble(config.getDouble("mounts.summoning.pathfinding-speed", 1.35),
                        0.5, 3.0, 1.35, "mounts.summoning.pathfinding-speed", warning),
                validateInt(config.getInt("mounts.persistence.autosave-period-ticks", 100),
                        20, 1_200, 100, "mounts.persistence.autosave-period-ticks", warning),
                config.getBoolean("mounts.persistence.recall-on-quit", true),
                Set.copyOf(mountWorlds),
                validateDouble(config.getDouble("mounts.defaults.max-health", 30.0),
                        1.0, 2_048.0, 30.0, "mounts.defaults.max-health", warning),
                validateDouble(config.getDouble("mounts.defaults.movement-speed", 0.225),
                        0.01, 10.0, 0.225, "mounts.defaults.movement-speed", warning),
                validateDouble(config.getDouble("mounts.defaults.jump-strength", 0.70),
                        0.0, 2.0, 0.70, "mounts.defaults.jump-strength", warning),
                mountMinimumNameLength,
                mountMaximumNameLength,
                parseMaterial(config.getString("mounts.whistle.material", "GOAT_HORN"),
                        Material.GOAT_HORN, "mounts.whistle.material", warning),
                validateInt(config.getInt("mounts.whistle.custom-model-data", 260102),
                        0, 16_777_215, 260102, "mounts.whistle.custom-model-data", warning)
        );

        Map<String, SoundSettings> sounds = new HashMap<>();
        for (String key : new String[]{"bite", "hit", "miss", "timeout", "escape", "minigame-success",
                "catch-success", "campfire-rested", "echo-vein-pulse", "echo-vein-ore-reveal",
                "echo-vein-success", "echo-vein-failure"}) {
            ConfigurationSection section = config.getConfigurationSection("sounds." + key);
            if (section == null) {
                if ("campfire-rested".equals(key)) {
                    sounds.put(key, new SoundSettings(true,
                            "minecraft:block.amethyst_block.chime", 0.65f, 1.15f));
                    continue;
                }
                if ("echo-vein-ore-reveal".equals(key)) {
                    sounds.put(key, new SoundSettings(true,
                            "minecraft:block.amethyst_cluster.place", 0.9f, 1.15f));
                    continue;
                }
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

        boolean miningModuleEnabled = config.contains("modules.mining.enabled", true)
                ? config.getBoolean("modules.mining.enabled", true)
                : config.getBoolean("modules.echo-vein.enabled", true);

        current = new PluginConfig(
                plugin.getDescription().getVersion(),
                config.getBoolean("plugin.debug", false),
                Map.of(
                        "fishing", config.getBoolean("modules.fishing.enabled", true),
                        "sitting", config.getBoolean("modules.sitting.enabled", true),
                        "campfire", config.getBoolean("modules.campfire.enabled", true),
                        "auth", config.getBoolean("modules.auth.enabled", true),
                        "mining", miningModuleEnabled,
                        "mounts", config.getBoolean("modules.mounts.enabled", true)
                ),
                minigame,
                hookParticles,
                successEffect,
                failureEffect,
                valhallaFishing,
                fishing,
                sitting,
                campfire,
                auth,
                echoVein,
                mounts,
                updater,
                worldConfig,
                Map.copyOf(sounds)
        );
        return current;
    }

    private String validateRelativeStoragePath(String configured, String key, String fallback,
                                               Consumer<String> warning) {
        if (configured == null || configured.isBlank() || configured.contains("..")
                || configured.startsWith("/") || configured.startsWith("\\")
                || configured.matches("^[A-Za-z]:.*")) {
            warning.accept("Invalid " + key + "; using " + fallback + ".");
            return fallback;
        }
        return configured.replace('\\', '/');
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

    private org.bukkit.Particle parseDataFreeParticle(
            String value,
            String fallback,
            String path,
            Consumer<String> warning
    ) {
        org.bukkit.Particle particle = parseParticle(value, fallback, warning);
        if (particle.getDataType() == Void.class) {
            return particle;
        }
        warning.accept(path + " requires a particle without extra data; using " + fallback + ".");
        return org.bukkit.Particle.valueOf(fallback);
    }

    private double parseSeatYOffset(FileConfiguration config, Consumer<String> warning) {
        double value = config.getDouble("sitting.seat-y-offset", 0.20);
        if (Double.compare(value, -1.15) == 0
                || Double.compare(value, -0.35) == 0
                || Double.compare(value, 0.35) == 0
                || Double.compare(value, 0.25) == 0) {
            warning.accept("Migrating the pre-release sitting.seat-y-offset to 0.20.");
            return 0.20;
        }
        return validateDouble(value, -2.0, 1.0, 0.20, "sitting.seat-y-offset", warning);
    }

    private boolean parseRestedIndicator(FileConfiguration config, Consumer<String> warning) {
        String value = config.getString("campfire.visuals.rested.indicator", "ACTION_BAR");
        if (value == null) {
            return true;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ACTION_BAR", "BOSS_BAR" -> true;
            case "NONE" -> false;
            default -> {
                warning.accept("Invalid campfire.visuals.rested.indicator; using ACTION_BAR.");
                yield true;
            }
        };
    }

    private Set<EntityType> parseEntityTypes(
            FileConfiguration config,
            String path,
            Set<EntityType> fallback,
            Consumer<String> warning
    ) {
        List<String> values = config.getStringList(path);
        if (values.isEmpty()) {
            return fallback;
        }
        Set<EntityType> types = new HashSet<>();
        for (String value : values) {
            try {
                types.add(EntityType.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (RuntimeException exception) {
                warning.accept("Invalid entity type '" + value + "' in " + path + "; ignoring it.");
            }
        }
        if (types.isEmpty()) {
            warning.accept("No valid entity types in " + path + "; using ARMOR_STAND.");
            return fallback;
        }
        return Set.copyOf(types);
    }

    private Set<CampFeature> parseCampFeatures(FileConfiguration config, Consumer<String> warning) {
        Set<CampFeature> fallback = Set.of(
                CampFeature.CRAFTING_TABLE,
                CampFeature.BED,
                CampFeature.SMOKER,
                CampFeature.BARREL,
                CampFeature.WATER_CAULDRON,
                CampFeature.CARTOGRAPHY_TABLE,
                CampFeature.GRINDSTONE
        );
        List<String> values = config.getStringList("campfire.camping.features");
        if (values.isEmpty()) {
            return fallback;
        }
        Set<CampFeature> features = new HashSet<>();
        for (String value : values) {
            try {
                features.add(CampFeature.valueOf(value.trim().toUpperCase(Locale.ROOT)));
            } catch (RuntimeException exception) {
                warning.accept("Invalid camp feature '" + value
                        + "' in campfire.camping.features; ignoring it.");
            }
        }
        if (features.isEmpty()) {
            warning.accept("No valid campfire.camping.features; using all default camp features.");
            return fallback;
        }
        return Set.copyOf(features);
    }

    private int validateInt(int value, int minimum, int maximum, int fallback,
                            String path, Consumer<String> warning) {
        if (value < minimum || value > maximum) {
            warning.accept("Invalid " + path + "; using " + fallback + ".");
            return fallback;
        }
        return value;
    }

    private Material parseMaterial(String value, Material fallback, String path,
                                   Consumer<String> warning) {
        Material material = value == null ? null : Material.matchMaterial(value.trim());
        if (material == null || material.isAir()) {
            warning.accept("Invalid " + path + "; using " + fallback.name() + ".");
            return fallback;
        }
        return material;
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
