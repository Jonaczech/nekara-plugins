package cz.nekara.rpg.skills.telemetry;

public record SkillRuntimeMetricsSnapshot(
    long startedAtEpochMillis,
    long submitted,
    long queueRejected,
    long completed,
    long awarded,
    long denied,
    long duplicate,
    long capped,
    long failed,
    long awardedExperience,
    int queueDepth,
    int queueHighWater,
    long averageLatencyMicros,
    long maximumLatencyMicros
) {
}
