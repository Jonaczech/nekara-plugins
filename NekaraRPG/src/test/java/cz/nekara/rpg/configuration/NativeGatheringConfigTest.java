package cz.nekara.rpg.configuration;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeGatheringConfigTest {
    @Test
    void defaultsSeparateWoodcuttingAndDiggingSources() {
        var wood = NativeGatheringConfig.defaultWoodcuttingExperience();
        var digging = NativeGatheringConfig.defaultDiggingExperience();

        assertEquals(4L, wood.get(Material.OAK_LOG));
        assertFalse(wood.containsKey(Material.DIRT));
        assertEquals(2L, digging.get(Material.DIRT));
        assertEquals(3L, digging.get(Material.CLAY));
        assertFalse(digging.containsKey(Material.SUSPICIOUS_SAND));
    }

    @Test
    void abilityBudgetRejectsUnsafeTickConfiguration() {
        assertThrows(IllegalArgumentException.class,
            () -> new GatheringAbilityConfig(true, 8, 9, 10));
        assertTrue(new GatheringAbilityConfig(true, 64, 8, 12).enabled());
    }
}
