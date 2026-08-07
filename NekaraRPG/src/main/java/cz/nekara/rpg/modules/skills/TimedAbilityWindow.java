package cz.nekara.rpg.modules.skills;

/**
 * A one-shot activation window followed by its cooldown. Time values use epoch milliseconds.
 */
record TimedAbilityWindow(long activeUntil, long cooldownUntil) {
    static TimedAbilityWindow start(long now, int durationSeconds, int cooldownSeconds) {
        long activeUntil = now + durationSeconds * 1_000L;
        return new TimedAbilityWindow(activeUntil, activeUntil + cooldownSeconds * 1_000L);
    }

    boolean isActiveAt(long now) {
        return now < activeUntil;
    }

    long cooldownRemainingAt(long now) {
        if (isActiveAt(now)) {
            return 0L;
        }
        return Math.max(0L, cooldownUntil - now);
    }
}
