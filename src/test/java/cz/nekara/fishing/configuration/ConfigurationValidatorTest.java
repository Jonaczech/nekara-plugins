package cz.nekara.fishing.configuration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationValidatorTest {
    @Test
    void replacesInvalidValuesWithSafeDefaultsAndWarns() {
        List<String> warnings = new ArrayList<>();
        MinigameConfig result = ConfigurationValidator.validate(
                new MinigameConfig(true, DisplayMode.ACTION_BAR, 0, 0, 30, -1, 0, -1, 0, -1,
                        true, true, IndicatorDirection.RIGHT, -1), warnings::add);

        assertEquals(20, result.barLength());
        assertEquals(6, result.updatePeriodTicks());
        assertEquals(5, result.targetWidth());
        assertEquals(6, result.targetRelocationMaxDistance());
        assertEquals(3, result.requiredHits());
        assertEquals(5, result.maxRequiredHits());
        assertEquals(1, result.maxMisses());
        assertEquals(160, result.timeoutTicks());
        assertEquals(30, result.timeBonusTicks());
        assertEquals(150, result.inputDebounceMilliseconds());
        assertTrue(warnings.size() >= 7);
    }

    @Test
    void targetWidthRemainsStrictlySmallerThanBar() {
        List<String> warnings = new ArrayList<>();
        MinigameConfig result = ConfigurationValidator.validate(
                new MinigameConfig(true, DisplayMode.ACTION_BAR, 4, 1, 4, 6, 1, 0, 20, 0,
                        true, true, IndicatorDirection.RANDOM, 0), warnings::add);

        assertTrue(result.targetWidth() > 0);
        assertTrue(result.targetWidth() < result.barLength());
    }
}
