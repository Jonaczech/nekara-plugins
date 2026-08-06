package cz.nekara.rpg.configuration;

import cz.nekara.rpg.skills.SkillId;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModuleConfigurationStore {
    private static final List<ModuleFile> MODULE_FILES = moduleFiles();
    private static final Map<SkillId, List<String>> SKILL_ABILITIES = Map.of(
            SkillId.MINING, List.of("vein-mining", "drilling"),
            SkillId.WOODCUTTING, List.of("tree-feller")
    );

    private static List<ModuleFile> moduleFiles() {
        List<ModuleFile> files = new java.util.ArrayList<>(List.of(
            new ModuleFile("auth/config.yml", "auth", List.of("auth"), List.of()),
            new ModuleFile("fishing/config.yml",
                    null,
                    List.of("minigame", "fishing", "worlds", "effects"),
                    List.of("bite", "hit", "miss", "timeout", "escape",
                            "minigame-success", "catch-success")),
            new ModuleFile("campfire/config.yml", "campfire", List.of("campfire", "sitting"),
                    List.of("campfire-rested")),
            new ModuleFile("mining/config.yml", null, List.of("echo-vein"),
                    List.of("echo-vein-pulse", "echo-vein-ore-reveal",
                            "echo-vein-success", "echo-vein-failure")),
            new ModuleFile("mounts/config.yml", "mounts", List.of("mounts"), List.of()),
            new ModuleFile("skills/config.yml", "skills", List.of("skills"), List.of())
        ));
        for (SkillId skill : SkillId.gameplaySkills()) {
            files.add(new ModuleFile(
                    "skills/" + skill.id() + "/config.yml",
                    "skills." + skill.id(), List.of(), List.of()));
            files.add(new ModuleFile(
                    "skills/" + skill.id() + "/experience-sources.yml",
                    "skills." + skill.id() + ".experience-sources", List.of(), List.of()));
        }
        files.add(new ModuleFile("skills/lesnictvi/loot-tables.yml",
                "skills.lesnictvi.rewards", List.of(), List.of()));
        files.add(new ModuleFile("skills/kopani/loot-tables.yml",
                "skills.kopani.rewards", List.of(), List.of()));
        files.add(new ModuleFile("skills/rybareni/loot-tables.yml",
                "skills.rybareni.rewards", List.of(), List.of()));
        return List.copyOf(files);
    }

    private final JavaPlugin plugin;

    ModuleConfigurationStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    FileConfiguration reload(FileConfiguration root) {
        migrateRenamedSkillIds(root);
        boolean migrateLegacy = ConfigurationLayout.requiresMigration(
                root.contains("configuration-layout", true),
                root.getInt("configuration-layout", 1));
        Map<ModuleFile, LoadedModule> loaded = new LinkedHashMap<>();

        for (ModuleFile definition : MODULE_FILES) {
            LoadedModule module = load(definition);
            if (migrateLegacy && !module.existedBeforeLoad()
                    && containsLegacyValues(root, definition)) {
                migrate(root, module.configuration(), definition);
                save(module.configuration(), module.file());
            }
            loaded.put(definition, module);
        }

        migrateLegacySkillLayout(loaded);

        if (migrateLegacy) {
            for (ModuleFile definition : MODULE_FILES) {
                removeLegacyValues(root, definition);
            }
            root.set("configuration-layout", ConfigurationLayout.CURRENT);
            plugin.saveConfig();
            plugin.getLogger().info("Migrated the monolithic configuration to per-module config files.");
        }

        YamlConfiguration merged = new YamlConfiguration();
        copyValues(root, merged, true);
        for (Map.Entry<ModuleFile, LoadedModule> entry : loaded.entrySet()) {
            copyModuleValues(entry.getValue().defaults(), merged, entry.getKey().mountPath(), false);
            copyModuleValues(entry.getValue().configuration(), merged, entry.getKey().mountPath(), true);
        }
        return merged;
    }

    private void migrateRenamedSkillIds(FileConfiguration root) {
        boolean changed = false;
        File skillsDirectory = new File(plugin.getDataFolder(), "skills");
        for (Map.Entry<String, String> entry : SkillId.renamedIds().entrySet()) {
            File legacy = new File(skillsDirectory, entry.getKey());
            File current = new File(skillsDirectory, entry.getValue());
            if (legacy.isDirectory()) {
                if (current.exists()) {
                    throw new IllegalStateException("Cannot migrate skill configuration " + entry.getKey()
                        + "; target folder " + entry.getValue() + " already exists");
                }
                try {
                    Files.move(legacy.toPath(), current.toPath(), StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException exception) {
                    throw new IllegalStateException("Cannot migrate skill configuration " + entry.getKey(), exception);
                }
                changed = true;
            }
            String legacyPath = "skills." + entry.getKey();
            String currentPath = "skills." + entry.getValue();
            if (root.contains(legacyPath, true)) {
                if (root.contains(currentPath, true)) {
                    throw new IllegalStateException("Cannot migrate legacy skill configuration " + entry.getKey()
                        + "; target key " + entry.getValue() + " already exists");
                }
                root.set(currentPath, root.get(legacyPath));
                root.set(legacyPath, null);
                changed = true;
            }
        }
        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("Migrated Nekara Skills IDs to the current ASCII identifiers.");
        }
    }

    private void migrateLegacySkillLayout(Map<ModuleFile, LoadedModule> loaded) {
        LoadedModule shared = loaded.entrySet().stream()
                .filter(entry -> "skills/config.yml".equals(entry.getKey().resourcePath()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
        boolean sharedChanged = false;

        for (SkillId skill : SkillId.gameplaySkills()) {
            LoadedModule target = findLoaded(loaded, "skills/" + skill.id() + "/config.yml");
            if (!target.existedBeforeLoad()) {
                sharedChanged |= migrateLegacySkillValues(
                        shared.configuration(), target.configuration(), skill);
                save(target.configuration(), target.file());
            }
        }

        sharedChanged |= migrateLootTable(loaded, SkillId.WOODCUTTING);
        sharedChanged |= migrateLootTable(loaded, SkillId.DIGGING);
        if (sharedChanged) {
            save(shared.configuration(), shared.file());
            plugin.getLogger().info("Migrated Nekara Skills configuration to per-skill folders.");
        }
    }

    static boolean migrateLegacySkillValues(
            YamlConfiguration shared,
            YamlConfiguration target,
            SkillId skill
    ) {
        boolean changed = copyAndRemoveSection(shared, target, skill.id(), "");
        String legacyExperience = "activities.experience." + skill.id();
        if (shared.contains(legacyExperience, true)) {
            target.set("experience.amount", shared.get(legacyExperience));
            shared.set(legacyExperience, null);
            changed = true;
        }
        for (String ability : SKILL_ABILITIES.getOrDefault(skill, List.of())) {
            changed |= copyAndRemoveSection(
                    shared, target, "abilities." + ability, "abilities." + ability);
        }
        return changed;
    }

    private boolean migrateLootTable(Map<ModuleFile, LoadedModule> loaded, SkillId skill) {
        LoadedModule config = findLoaded(loaded, "skills/" + skill.id() + "/config.yml");
        LoadedModule loot = findLoaded(loaded, "skills/" + skill.id() + "/loot-tables.yml");
        if (loot.existedBeforeLoad()
                || !config.configuration().contains("rewards.rare-drops", true)) {
            return false;
        }
        copyPath(config.configuration(), loot.configuration(),
                "rewards.rare-drops", "rare-drops");
        config.configuration().set("rewards.rare-drops", null);
        save(config.configuration(), config.file());
        save(loot.configuration(), loot.file());
        return true;
    }

    private static boolean copyAndRemoveSection(
            YamlConfiguration source,
            YamlConfiguration target,
            String sourcePath,
            String targetPath
    ) {
        if (!source.contains(sourcePath, true)) {
            return false;
        }
        copyPath(source, target, sourcePath, targetPath);
        source.set(sourcePath, null);
        return true;
    }

    private LoadedModule findLoaded(Map<ModuleFile, LoadedModule> loaded, String resourcePath) {
        return loaded.entrySet().stream()
                .filter(entry -> resourcePath.equals(entry.getKey().resourcePath()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Missing loaded module configuration: " + resourcePath));
    }

    private LoadedModule load(ModuleFile definition) {
        File file = new File(plugin.getDataFolder(), definition.resourcePath());
        boolean existedBeforeLoad = file.isFile();
        if (!existedBeforeLoad) {
            plugin.saveResource(definition.resourcePath(), false);
        }
        YamlConfiguration defaults = loadResource(definition.resourcePath());
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        configuration.setDefaults(defaults);
        migrateSittingIntoCampfire(definition, configuration, file);
        migrateLyingVisualDefault(definition, configuration, file);
        migrateMountsPreReleaseDefaults(definition, configuration, file);
        return new LoadedModule(file, defaults, configuration, existedBeforeLoad);
    }

    private void migrateSittingIntoCampfire(
            ModuleFile definition,
            YamlConfiguration configuration,
            File file
    ) {
        if (!"campfire/config.yml".equals(definition.resourcePath())
                || configuration.contains("sitting", true)) {
            return;
        }
        File legacyFile = new File(plugin.getDataFolder(), "sitting/config.yml");
        if (!legacyFile.isFile()) {
            return;
        }
        YamlConfiguration legacy = YamlConfiguration.loadConfiguration(legacyFile);
        if (!migrateLegacySittingValues(legacy, configuration)) {
            return;
        }
        save(configuration, file);
        plugin.getLogger().info("Moved sitting settings under the Campfire module configuration.");
    }

    static boolean migrateLegacySittingValues(
            YamlConfiguration legacy,
            YamlConfiguration campfire
    ) {
        if (campfire.contains("sitting", true)) {
            return false;
        }
        boolean copied = false;
        for (Map.Entry<String, Object> entry : legacy.getValues(true).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection) {
                continue;
            }
            campfire.set("sitting." + entry.getKey(), entry.getValue());
            copied = true;
        }
        return copied;
    }

    private void migrateLyingVisualDefault(
            ModuleFile definition,
            YamlConfiguration configuration,
            File file
    ) {
        if (!"campfire/config.yml".equals(definition.resourcePath())
                || !migrateLyingVisualDefault(configuration)) {
            return;
        }
        save(configuration, file);
        plugin.getLogger().info("Corrected the former lying mannequin yaw default from -90.0 to 0.0.");
    }

    static boolean migrateLyingVisualDefault(YamlConfiguration configuration) {
        String path = "lying.mannequin.yaw-offset-degrees";
        if (!configuration.contains(path, true)
                || Double.compare(configuration.getDouble(path), -90.0) != 0) {
            return false;
        }
        configuration.set(path, 0.0);
        return true;
    }

    private void migrateMountsPreReleaseDefaults(
            ModuleFile definition,
            YamlConfiguration configuration,
            File file
    ) {
        if (!"mounts/config.yml".equals(definition.resourcePath())
                || !migrateMountsPreReleaseDefaults(configuration)) {
            return;
        }
        save(configuration, file);
        plugin.getLogger().info("Migrated NekaraMounts configuration to version 3.");
    }

    static boolean migrateMountsPreReleaseDefaults(YamlConfiguration configuration) {
        int version = configuration.getInt("configuration-version", 0);
        if (version >= 3) {
            return false;
        }
        if (version == 0 && configuration.getInt("death.cooldown-seconds", 86_400) == 86_400) {
            configuration.set("death.cooldown-seconds", 60);
        }
        if (!configuration.contains("storage.database-file", true)) {
            configuration.set("storage.database-file", "mounts/data.db");
        }
        configuration.set("configuration-version", 3);
        return true;
    }

    private YamlConfiguration loadResource(String resourcePath) {
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing bundled configuration resource: " + resourcePath);
        }
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read bundled configuration: " + resourcePath, exception);
        }
    }

    private boolean containsLegacyValues(FileConfiguration root, ModuleFile definition) {
        for (String path : definition.rootPaths()) {
            if (root.contains(path, true)) {
                return true;
            }
        }
        for (String sound : definition.soundKeys()) {
            if (root.contains("sounds." + sound, true)) {
                return true;
            }
        }
        return false;
    }

    private void migrate(FileConfiguration root, YamlConfiguration target, ModuleFile definition) {
        for (String path : definition.rootPaths()) {
            String targetPath = path.equals(definition.mountPath()) ? "" : path;
            copyPath(root, target, path, targetPath);
        }
        for (String sound : definition.soundKeys()) {
            copyPath(root, target, "sounds." + sound, "sounds." + sound);
        }
    }

    private static void copyPath(FileConfiguration source, YamlConfiguration target,
                          String sourcePath, String targetPath) {
        ConfigurationSection section = source.getConfigurationSection(sourcePath);
        if (section == null) {
            if (source.contains(sourcePath, true) && !targetPath.isEmpty()) {
                target.set(targetPath, source.get(sourcePath));
            }
            return;
        }
        for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                String path = targetPath.isEmpty()
                        ? entry.getKey() : targetPath + "." + entry.getKey();
                target.set(path, entry.getValue());
            }
        }
    }

    private void removeLegacyValues(FileConfiguration root, ModuleFile definition) {
        for (String path : definition.rootPaths()) {
            root.set(path, null);
        }
        for (String sound : definition.soundKeys()) {
            root.set("sounds." + sound, null);
        }
        ConfigurationSection sounds = root.getConfigurationSection("sounds");
        if (sounds != null && sounds.getKeys(false).isEmpty()) {
            root.set("sounds", null);
        }
    }

    private void copyValues(FileConfiguration source, YamlConfiguration target, boolean explicitOnly) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)
                    && (!explicitOnly || source.contains(entry.getKey(), true))) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private void copyModuleValues(FileConfiguration source, YamlConfiguration target,
                                  String mountPath, boolean explicitOnly) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection
                    || (explicitOnly && !source.contains(entry.getKey(), true))) {
                continue;
            }
            String path = entry.getKey();
            if (mountPath != null && !path.startsWith("sounds.")) {
                path = mountPath + "." + path;
            }
            target.set(path, entry.getValue());
        }
    }

    private void save(YamlConfiguration configuration, File file) {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot save migrated module configuration: " + file, exception);
        }
    }

    private record ModuleFile(
            String resourcePath,
            String mountPath,
            List<String> rootPaths,
            List<String> soundKeys
    ) {
    }

    private record LoadedModule(
            File file,
            YamlConfiguration defaults,
            YamlConfiguration configuration,
            boolean existedBeforeLoad
    ) {
    }
}
