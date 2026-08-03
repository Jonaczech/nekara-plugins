package cz.nekara.rpg.configuration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleConfigurationStoreTest {
    @Test
    void missingExplicitLayoutStillMigratesWhenBundledDefaultsAlreadyContainVersionTwo() {
        assertTrue(ConfigurationLayout.requiresMigration(
                false, ConfigurationLayout.CURRENT));
    }

    @Test
    void explicitCurrentLayoutDoesNotMigrateAgain() {
        assertFalse(ConfigurationLayout.requiresMigration(
                true, ConfigurationLayout.CURRENT));
    }

    @Test
    void preReleaseMountDeathDefaultMigratesToOneMinute() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("death.cooldown-seconds", 86_400);

        assertTrue(ModuleConfigurationStore.migrateMountsPreReleaseDefaults(configuration));
        assertEquals(3, configuration.getInt("configuration-version"));
        assertEquals("mounts/data.db", configuration.getString("storage.database-file"));
        assertEquals(60, configuration.getInt("death.cooldown-seconds"));
        assertFalse(ModuleConfigurationStore.migrateMountsPreReleaseDefaults(configuration));
    }

    @Test
    void explicitCustomDeathCooldownSurvivesPreReleaseMigration() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("death.cooldown-seconds", 300);

        assertTrue(ModuleConfigurationStore.migrateMountsPreReleaseDefaults(configuration));
        assertEquals(3, configuration.getInt("configuration-version"));
        assertEquals(300, configuration.getInt("death.cooldown-seconds"));
    }

    @Test
    void versionTwoMountConfigurationGainsSqlitePathWithoutChangingCustomValues() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("configuration-version", 2);
        configuration.set("storage.file", "custom/legacy.yml");

        assertTrue(ModuleConfigurationStore.migrateMountsPreReleaseDefaults(configuration));
        assertEquals(3, configuration.getInt("configuration-version"));
        assertEquals("custom/legacy.yml", configuration.getString("storage.file"));
        assertEquals("mounts/data.db", configuration.getString("storage.database-file"));
        assertFalse(ModuleConfigurationStore.migrateMountsPreReleaseDefaults(configuration));
    }
}
