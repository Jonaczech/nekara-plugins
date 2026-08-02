package cz.nekara.rpg.configuration;

import org.junit.jupiter.api.Test;

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
}
