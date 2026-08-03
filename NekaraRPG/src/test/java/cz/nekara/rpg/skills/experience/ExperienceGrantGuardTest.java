package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperienceGrantGuardTest {
    @Test
    void sameSourceEventCanOnlyBeClaimedOnceWithinTimeToLive() {
        AtomicLong now = new AtomicLong(1_000);
        ExperienceGrantGuard guard = new ExperienceGrantGuard(Duration.ofSeconds(5), 10, now::get);
        ExperienceFingerprint fingerprint = fingerprint("block:1");

        assertTrue(guard.tryAcquire(fingerprint));
        assertFalse(guard.tryAcquire(fingerprint));

        now.addAndGet(5_000);
        assertTrue(guard.tryAcquire(fingerprint));
    }

    @Test
    void boundedGuardEvictsOldestFingerprints() {
        AtomicLong now = new AtomicLong(1_000);
        ExperienceGrantGuard guard = new ExperienceGrantGuard(Duration.ofMinutes(1), 2, now::get);

        assertTrue(guard.tryAcquire(fingerprint("first")));
        assertTrue(guard.tryAcquire(fingerprint("second")));
        assertTrue(guard.tryAcquire(fingerprint("third")));
        assertEquals(2, guard.trackedFingerprintCount());
        assertTrue(guard.tryAcquire(fingerprint("first")));
    }

    private static ExperienceFingerprint fingerprint(String sourceKey) {
        return new ExperienceFingerprint("player-1", SkillId.MINING, "block_break", sourceKey);
    }
}
