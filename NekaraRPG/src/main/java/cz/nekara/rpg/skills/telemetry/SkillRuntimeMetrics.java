package cz.nekara.rpg.skills.telemetry;

import cz.nekara.rpg.skills.experience.ExperienceAwardResult;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class SkillRuntimeMetrics {
    private final long startedAtEpochMillis;
    private final LongAdder submitted = new LongAdder();
    private final LongAdder queueRejected = new LongAdder();
    private final LongAdder completed = new LongAdder();
    private final LongAdder awarded = new LongAdder();
    private final LongAdder denied = new LongAdder();
    private final LongAdder duplicate = new LongAdder();
    private final LongAdder capped = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder awardedExperience = new LongAdder();
    private final LongAdder totalLatencyNanos = new LongAdder();
    private final AtomicLong maximumLatencyNanos = new AtomicLong();
    private final AtomicInteger queueHighWater = new AtomicInteger();

    public SkillRuntimeMetrics(long startedAtEpochMillis) {
        this.startedAtEpochMillis = startedAtEpochMillis;
    }

    public void recordSubmitted(int queueDepth) {
        submitted.increment();
        queueHighWater.accumulateAndGet(queueDepth, Math::max);
    }

    public void recordQueueRejected() {
        queueRejected.increment();
    }

    public void recordCompleted(ExperienceAwardResult result, long latencyNanos) {
        Objects.requireNonNull(result, "result");
        completed.increment();
        recordLatency(latencyNanos);
        switch (result.status()) {
            case AWARDED -> {
                awarded.increment();
                awardedExperience.add(result.awardedExperience());
            }
            case DENIED -> denied.increment();
            case DUPLICATE -> duplicate.increment();
            case CAPPED -> capped.increment();
        }
    }

    public void recordFailure(long latencyNanos) {
        failed.increment();
        recordLatency(latencyNanos);
    }

    public SkillRuntimeMetricsSnapshot snapshot(int queueDepth) {
        long completedCount = completed.sum();
        long failedCount = failed.sum();
        long timedCount = completedCount + failedCount;
        long totalNanos = totalLatencyNanos.sum();
        return new SkillRuntimeMetricsSnapshot(
            startedAtEpochMillis,
            submitted.sum(),
            queueRejected.sum(),
            completedCount,
            awarded.sum(),
            denied.sum(),
            duplicate.sum(),
            capped.sum(),
            failedCount,
            awardedExperience.sum(),
            Math.max(0, queueDepth),
            queueHighWater.get(),
            timedCount == 0 ? 0 : (totalNanos / timedCount) / 1_000L,
            maximumLatencyNanos.get() / 1_000L
        );
    }

    private void recordLatency(long latencyNanos) {
        long safe = Math.max(0L, latencyNanos);
        totalLatencyNanos.add(safe);
        maximumLatencyNanos.accumulateAndGet(safe, Math::max);
    }
}
