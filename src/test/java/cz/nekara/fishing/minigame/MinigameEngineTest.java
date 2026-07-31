package cz.nekara.fishing.minigame;

import cz.nekara.fishing.configuration.DisplayMode;
import cz.nekara.fishing.configuration.IndicatorDirection;
import cz.nekara.fishing.configuration.MinigameConfig;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinigameEngineTest {
    @Test
    void movesRightAndReflectsAtRightEdge() {
        MinigameEngine engine = new MinigameEngine(config(4, 1, 100, IndicatorDirection.RIGHT), new Random(1));
        engine.advanceTick();
        engine.advanceTick();
        engine.advanceTick();
        assertEquals(3, engine.indicatorPosition());
        engine.advanceTick();
        assertEquals(2, engine.indicatorPosition());
    }

    @Test
    void movesLeftAndReflectsAtLeftEdge() {
        MinigameEngine engine = new MinigameEngine(config(4, 1, 100, IndicatorDirection.LEFT), new Random(1));
        engine.advanceTick();
        engine.advanceTick();
        engine.advanceTick();
        assertEquals(0, engine.indicatorPosition());
        engine.advanceTick();
        assertEquals(1, engine.indicatorPosition());
    }

    @Test
    void acceptsClickAtTargetBoundaries() {
        MinigameEngine engine = new MinigameEngine(config(8, 1, 100, IndicatorDirection.RIGHT, 2), new Random(1));
        while (engine.indicatorPosition() != engine.targetStart()) {
            engine.advanceTick();
        }
        assertTrue(engine.isIndicatorInTarget());
        assertEquals(ClickResult.HIT, engine.click());

        while (engine.indicatorPosition() != engine.targetEndExclusive() - 1) {
            engine.advanceTick();
        }
        assertTrue(engine.isIndicatorInTarget());
    }

    @Test
    void missFailsOnlyAfterAllowedMissesAreExceeded() {
        MinigameEngine engine = new MinigameEngine(config(8, 1, 100, IndicatorDirection.RIGHT, 1, 1), new Random(0));
        moveOutsideTarget(engine);
        assertEquals(ClickResult.MISS, engine.click());
        moveOutsideTarget(engine);
        assertEquals(ClickResult.FAILED, engine.click());
        assertEquals(MinigameState.FAILED, engine.state());
    }

    @Test
    void timeoutStopsFurtherInput() {
        MinigameEngine engine = new MinigameEngine(config(8, 1, 3, IndicatorDirection.RIGHT));
        assertEquals(TickResult.MOVED, engine.advanceTick());
        assertEquals(TickResult.MOVED, engine.advanceTick());
        assertEquals(TickResult.TIMED_OUT, engine.advanceTick());
        assertEquals(ClickResult.IGNORED, engine.click());
        assertEquals(MinigameState.TIMED_OUT, engine.state());
    }

    @Test
    void successfulHitAddsConfiguredTimeWithoutExceedingInitialLimit() {
        MinigameEngine engine = new MinigameEngine(config(8, 1, 20, IndicatorDirection.RIGHT), new Random(1));
        engine.advanceTick();
        engine.advanceTick();
        assertEquals(2, engine.elapsedTicks());
        while (!engine.isIndicatorInTarget()) {
            engine.advanceTick();
        }
        assertEquals(ClickResult.HIT, engine.click());
        engine.addTimeBonus(20);
        assertEquals(0, engine.elapsedTicks());
        assertEquals(20, engine.timeLeftTicks());
    }

    @Test
    void targetAlwaysFitsInsideBar() {
        MinigameConfig config = config(25, 2, 100, IndicatorDirection.RANDOM, 5);
        for (int seed = 0; seed < 100; seed++) {
            MinigameEngine engine = new MinigameEngine(config, new Random(seed));
            assertTrue(engine.targetStart() >= 0);
            assertTrue(engine.targetEndExclusive() <= config.barLength());
        }
    }

    @Test
    void choosesRandomRequiredHitCountWithinConfiguredRange() {
        MinigameConfig config = new MinigameConfig(true, DisplayMode.ACTION_BAR, 20, 3, 5,
                6, 3, 5, 1, 100, 20, 1.0, true, false, IndicatorDirection.RANDOM, 150);

        for (int seed = 0; seed < 100; seed++) {
            MinigameEngine engine = new MinigameEngine(config, new Random(seed));
            assertTrue(engine.requiredHits() >= 3 && engine.requiredHits() <= 5);
        }
    }

    @Test
    void relocatedTargetStaysWithinConfiguredDistance() {
        MinigameConfig config = config(25, 2, 2_000, IndicatorDirection.RANDOM, 5);
        for (int seed = 0; seed < 100; seed++) {
            MinigameEngine engine = new MinigameEngine(config, new Random(seed));
            while (!engine.isIndicatorInTarget()) {
                engine.advanceTick();
            }
            assertEquals(ClickResult.HIT, engine.click());
            assertTrue(distanceToTarget(engine) <= config.targetRelocationMaxDistance());
        }
    }

    @Test
    void successCannotBeCompletedTwice() {
        MinigameEngine engine = new MinigameEngine(config(2, 1, 100, IndicatorDirection.RIGHT, 1, 0, 1), new Random(0));
        while (!engine.isIndicatorInTarget()) {
            engine.advanceTick();
        }
        assertEquals(ClickResult.SUCCEEDED, engine.click());
        assertEquals(ClickResult.IGNORED, engine.click());
        assertEquals(1, engine.hits());
        assertEquals(MinigameState.SUCCEEDED, engine.state());
    }

    private void moveOutsideTarget(MinigameEngine engine) {
        if (engine.isIndicatorInTarget()) {
            engine.advanceTick();
        }
        while (engine.isIndicatorInTarget()) {
            engine.advanceTick();
        }
    }

    private int distanceToTarget(MinigameEngine engine) {
        if (engine.indicatorPosition() < engine.targetStart()) {
            return engine.targetStart() - engine.indicatorPosition();
        }
        int end = engine.targetEndExclusive() - 1;
        return Math.max(0, engine.indicatorPosition() - end);
    }

    private MinigameConfig config(int barLength, int period, int timeout, IndicatorDirection direction) {
        return config(barLength, period, timeout, direction, 2, 1, 2);
    }

    private MinigameConfig config(
            int barLength,
            int period,
            int timeout,
            IndicatorDirection direction,
            int targetWidth
    ) {
        return config(barLength, period, timeout, direction, targetWidth, 1, 2);
    }

    private MinigameConfig config(
            int barLength,
            int period,
            int timeout,
            IndicatorDirection direction,
            int targetWidth,
            int maxMisses
    ) {
        return config(barLength, period, timeout, direction, targetWidth, maxMisses, 2);
    }

    private MinigameConfig config(
            int barLength,
            int period,
            int timeout,
            IndicatorDirection direction,
            int targetWidth,
            int maxMisses,
            int requiredHits
    ) {
        return new MinigameConfig(true, DisplayMode.ACTION_BAR, barLength, period, targetWidth,
                6, requiredHits, maxMisses, timeout, 20, true, false, direction, 150);
    }
}
