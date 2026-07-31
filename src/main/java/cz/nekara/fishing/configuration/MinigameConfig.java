package cz.nekara.fishing.configuration;

public record MinigameConfig(
        boolean enabled,
        DisplayMode display,
        int barLength,
        int updatePeriodTicks,
        int targetWidth,
        int targetRelocationMaxDistance,
        int requiredHits,
        int maxRequiredHits,
        int maxMisses,
        int timeoutTicks,
        int timeBonusTicks,
        double hookPullDistance,
        boolean moveTargetAfterHit,
        boolean randomizeStartPosition,
        IndicatorDirection indicatorDirection,
        long inputDebounceMilliseconds
) {
    /**
     * Backwards-compatible fixed-hit constructor for tests and integrations using the old schema.
     */
    public MinigameConfig(
            boolean enabled,
            DisplayMode display,
            int barLength,
            int updatePeriodTicks,
            int targetWidth,
            int targetRelocationMaxDistance,
            int requiredHits,
            int maxMisses,
            int timeoutTicks,
            int timeBonusTicks,
            boolean moveTargetAfterHit,
            boolean randomizeStartPosition,
            IndicatorDirection indicatorDirection,
            long inputDebounceMilliseconds
    ) {
        this(enabled, display, barLength, updatePeriodTicks, targetWidth,
                targetRelocationMaxDistance, requiredHits, requiredHits, maxMisses,
                timeoutTicks, timeBonusTicks, 1.0, moveTargetAfterHit, randomizeStartPosition,
                indicatorDirection, inputDebounceMilliseconds);
    }

    public MinigameConfig withDifficulty(int newRequiredHits, int newMaxRequiredHits, int newMaxMisses) {
        return new MinigameConfig(
                enabled,
                display,
                barLength,
                updatePeriodTicks,
                targetWidth,
                targetRelocationMaxDistance,
                newRequiredHits,
                newMaxRequiredHits,
                newMaxMisses,
                timeoutTicks,
                timeBonusTicks,
                hookPullDistance,
                moveTargetAfterHit,
                randomizeStartPosition,
                indicatorDirection,
                inputDebounceMilliseconds
        );
    }
}
