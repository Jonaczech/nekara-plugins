package cz.nekara.rpg.skills.experience;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkActivityTrackerTest {
    @Test
    void countsOnlyAwardsInsideTheConfiguredWindow() {
        AtomicLong now = new AtomicLong(1_000);
        ChunkActivityTracker tracker = new ChunkActivityTracker(
            Duration.ofSeconds(5), 10, now::get);
        ChunkActivityTracker.ChunkKey key = new ChunkActivityTracker.ChunkKey(
            UUID.randomUUID(), 4, -2);

        tracker.recordAward(key);
        tracker.recordAward(key);
        assertEquals(2, tracker.recentAwards(key));

        now.addAndGet(5_000);
        assertEquals(0, tracker.recentAwards(key));
    }

    @Test
    void keepsChunksIndependent() {
        AtomicLong now = new AtomicLong(1_000);
        ChunkActivityTracker tracker = new ChunkActivityTracker(
            Duration.ofMinutes(1), 10, now::get);
        UUID world = UUID.randomUUID();
        ChunkActivityTracker.ChunkKey first = new ChunkActivityTracker.ChunkKey(world, 0, 0);
        ChunkActivityTracker.ChunkKey second = new ChunkActivityTracker.ChunkKey(world, 1, 0);

        tracker.recordAward(first);

        assertEquals(1, tracker.recentAwards(first));
        assertEquals(0, tracker.recentAwards(second));
    }

    @Test
    void reservationReturnsTheCountBeforeTheNewAward() {
        AtomicLong now = new AtomicLong(1_000);
        ChunkActivityTracker tracker = new ChunkActivityTracker(
            Duration.ofMinutes(1), 10, now::get);
        ChunkActivityTracker.ChunkKey key = new ChunkActivityTracker.ChunkKey(
            UUID.randomUUID(), 0, 0);

        assertEquals(0, tracker.reserveAward(key));
        assertEquals(1, tracker.reserveAward(key));
        assertEquals(2, tracker.recentAwards(key));
    }
}
