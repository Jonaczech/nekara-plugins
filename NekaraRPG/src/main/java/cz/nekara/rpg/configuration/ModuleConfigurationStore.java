package cz.nekara.rpg.configuration;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ModuleConfigurationStore {
    private static final List<ModuleFile> MODULE_FILES = List.of(
            new ModuleFile("auth/config.yml", "auth", List.of("auth"), List.of()),
            new ModuleFile("fishing/config.yml",
                    null,
                    List.of("minigame", "fishing", "worlds", "effects", "valhalla"),
                    List.of("bite", "hit", "miss", "timeout", "escape",
                            "minigame-success", "catch-success")),
            new ModuleFile("sitting/config.yml", "sitting", List.of("sitting"), List.of()),
            new ModuleFile("campfire/config.yml", "campfire", List.of("campfire"),
                    List.of("campfire-rested")),
            new ModuleFile("mining/config.yml", null, List.of("echo-vein"),
                    List.of("echo-vein-pulse", "echo-vein-ore-reveal",
                            "echo-vein-success", "echo-vein-failure")),
            new ModuleFile("mounts/config.yml", "mounts", List.of("mounts"), List.of())
    );

    private final JavaPlugin plugin;

    ModuleConfigurationStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    FileConfiguration reload(FileConfiguration root) {
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

    private LoadedModule load(ModuleFile definition) {
        File file = new File(plugin.getDataFolder(), definition.resourcePath());
        boolean existedBeforeLoad = file.isFile();
        if (!existedBeforeLoad) {
            plugin.saveResource(definition.resourcePath(), false);
        }
        YamlConfiguration defaults = loadResource(definition.resourcePath());
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        configuration.setDefaults(defaults);
        migrateMountsPreReleaseDefaults(definition, configuration, file);
        return new LoadedModule(file, defaults, configuration, existedBeforeLoad);
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

    private void copyPath(FileConfiguration source, YamlConfiguration target,
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
