package cz.nekara.rpg.skills.experience;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentActionGuardTest {
    @Test
    void consumesMatchingActionOnlyOnce() {
        AtomicLong now = new AtomicLong(1_000);
        RecentActionGuard guard = new RecentActionGuard(Duration.ofSeconds(5), now::get);

        guard.record("player-1", "block-a");

        assertTrue(guard.consume("player-1", "block-a"));
        assertFalse(guard.consume("player-1", "block-a"));
    }

    @Test
    void mismatchConsumesTheOldActionAndExpiryRejectsIt() {
        AtomicLong now = new AtomicLong(1_000);
        RecentActionGuard guard = new RecentActionGuard(Duration.ofSeconds(5), now::get);

        guard.record("player-1", "block-a");
        assertFalse(guard.consume("player-1", "block-b"));
        assertFalse(guard.consume("player-1", "block-a"));

        guard.record("player-1", "block-a");
        now.addAndGet(5_000);
        assertFalse(guard.consume("player-1", "block-a"));
    }
}
