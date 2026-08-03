package cz.nekara.rpg.configuration;

import cz.nekara.rpg.skills.SkillId;
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

    @Test
    void legacyMiningValuesMoveIntoTheMiningFolder() {
        YamlConfiguration shared = new YamlConfiguration();
        shared.set("mining.experience.chunk-soft-limit", 77);
        shared.set("abilities.vein-mining.maximum-blocks", 42);
        YamlConfiguration mining = new YamlConfiguration();

        assertTrue(ModuleConfigurationStore.migrateLegacySkillValues(
                shared, mining, SkillId.MINING));
        assertEquals(77, mining.getInt("experience.chunk-soft-limit"));
        assertEquals(42, mining.getInt("abilities.vein-mining.maximum-blocks"));
        assertFalse(shared.contains("mining", true));
        assertFalse(shared.contains("abilities.vein-mining", true));
    }

    @Test
    void legacyActivityExperienceMovesIntoItsSkillFolder() {
        YamlConfiguration shared = new YamlConfiguration();
        shared.set("activities.experience.archery", 19);
        YamlConfiguration archery = new YamlConfiguration();

        assertTrue(ModuleConfigurationStore.migrateLegacySkillValues(
                shared, archery, SkillId.ARCHERY));
        assertEquals(19, archery.getInt("experience.amount"));
        assertFalse(shared.contains("activities.experience.archery", true));
    }

    @Test
    void legacySittingSettingsMoveUnderCampfireWithoutOverwritingNewSettings() {
        YamlConfiguration legacy = new YamlConfiguration();
        legacy.set("require-ground", false);
        legacy.set("seat-y-offset", 0.1);
        YamlConfiguration campfire = new YamlConfiguration();

        assertTrue(ModuleConfigurationStore.migrateLegacySittingValues(legacy, campfire));
        assertFalse(campfire.getBoolean("sitting.require-ground"));
        assertEquals(0.1, campfire.getDouble("sitting.seat-y-offset"));
        assertFalse(ModuleConfigurationStore.migrateLegacySittingValues(legacy, campfire));
    }
}
