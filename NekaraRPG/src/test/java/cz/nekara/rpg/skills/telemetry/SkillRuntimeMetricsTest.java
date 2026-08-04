package cz.nekara.rpg.skills.telemetry;

import cz.nekara.rpg.skills.experience.ExperienceAwardResult;
import cz.nekara.rpg.skills.experience.ExperienceAwardStatus;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillRuntimeMetricsTest {
    @Test
    void recordsQueueOutcomesAndBoundedLatencySummary() {
        SkillRuntimeMetrics metrics = new SkillRuntimeMetrics(1_000L);
        metrics.recordSubmitted(1);
        metrics.recordSubmitted(7);
        metrics.recordQueueRejected();
        metrics.recordCompleted(new ExperienceAwardResult(
            ExperienceAwardStatus.AWARDED, 25L, Optional.empty(), Optional.empty()), 2_000_000L);
        metrics.recordCompleted(
            ExperienceAwardResult.withoutProfile(ExperienceAwardStatus.DUPLICATE), 4_000_000L);
        metrics.recordFailure(6_000_000L);

        SkillRuntimeMetricsSnapshot snapshot = metrics.snapshot(3);
        assertEquals(1_000L, snapshot.startedAtEpochMillis());
        assertEquals(2L, snapshot.submitted());
        assertEquals(1L, snapshot.queueRejected());
        assertEquals(2L, snapshot.completed());
        assertEquals(1L, snapshot.awarded());
        assertEquals(1L, snapshot.duplicate());
        assertEquals(1L, snapshot.failed());
        assertEquals(25L, snapshot.awardedExperience());
        assertEquals(3, snapshot.queueDepth());
        assertEquals(7, snapshot.queueHighWater());
        assertEquals(4_000L, snapshot.averageLatencyMicros());
        assertEquals(6_000L, snapshot.maximumLatencyMicros());
    }
}
