package cz.nekara.rpg.mount;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountCooldownTest {
    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

    @Test
    void cooldownUsesCeilingSecondsAndExpiresExactlyAtBoundary() {
        assertTrue(MountCooldown.isActive(NOW.plusMillis(1), NOW));
        assertEquals(1L, MountCooldown.remainingSeconds(NOW.plusMillis(1), NOW));
        assertFalse(MountCooldown.isActive(NOW, NOW));
        assertEquals(0L, MountCooldown.remainingSeconds(NOW, NOW));
    }

    @Test
    void cooldownFormatsPlayerFacingDurations() {
        assertEquals("9 s", MountCooldown.format(9));
        assertEquals("2 min 5 s", MountCooldown.format(125));
        assertEquals("3 h 4 min", MountCooldown.format(11_040));
        assertEquals("2 d 3 h", MountCooldown.format(183_600));
    }
}
