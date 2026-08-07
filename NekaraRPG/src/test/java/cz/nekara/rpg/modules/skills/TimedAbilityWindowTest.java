package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimedAbilityWindowTest {
    @Test
    void staysActiveForItsFullDurationThenStartsCooldown() {
        TimedAbilityWindow window = TimedAbilityWindow.start(1_000L, 10, 12);

        assertTrue(window.isActiveAt(1_000L));
        assertTrue(window.isActiveAt(10_999L));
        assertFalse(window.isActiveAt(11_000L));
        assertEquals(0L, window.cooldownRemainingAt(10_999L));
        assertEquals(12_000L, window.cooldownRemainingAt(11_000L));
        assertEquals(1L, window.cooldownRemainingAt(22_999L));
        assertEquals(0L, window.cooldownRemainingAt(23_000L));
    }
}
