package cz.nekara.rpg.minigame;

import cz.nekara.rpg.configuration.IndicatorDirection;
import cz.nekara.rpg.configuration.MinigameConfig;

import java.util.Random;

/** Pure timing-game state machine. It has no Bukkit dependency and is directly unit-testable. */
public final class MinigameEngine {
    private final MinigameConfig config;
    private final Random random;
    private final int requiredHits;
    private int indicatorPosition;
    private int direction;
    private int targetStart;
    private int hits;
    private int misses;
    private int elapsedTicks;
    private MinigameState state = MinigameState.ACTIVE;

    public MinigameEngine(MinigameConfig config) {
        this(config, new Random());
    }

    public MinigameEngine(MinigameConfig config, Random random) {
        this.config = config;
        this.random = random;
        this.requiredHits = random.nextInt(config.maxRequiredHits() - config.requiredHits() + 1)
                + config.requiredHits();
        this.direction = switch (config.indicatorDirection()) {
            case RIGHT -> 1;
            case LEFT -> -1;
            case RANDOM -> random.nextBoolean() ? 1 : -1;
        };
        this.indicatorPosition = config.randomizeStartPosition()
                ? random.nextInt(config.barLength())
                : direction > 0 ? 0 : config.barLength() - 1;
        this.targetStart = randomTargetStart(false);
    }

    public TickResult advanceTick() {
        if (state != MinigameState.ACTIVE) {
            return TickResult.IGNORED;
        }
        elapsedTicks++;
        if (elapsedTicks >= config.timeoutTicks()) {
            state = MinigameState.TIMED_OUT;
            return TickResult.TIMED_OUT;
        }
        if (elapsedTicks % config.updatePeriodTicks() != 0) {
            return TickResult.ACTIVE;
        }
        moveIndicator();
        return TickResult.MOVED;
    }

    public ClickResult click() {
        if (state != MinigameState.ACTIVE) {
            return ClickResult.IGNORED;
        }
        if (isIndicatorInTarget()) {
            hits++;
            if (hits >= requiredHits) {
                state = MinigameState.SUCCEEDED;
                return ClickResult.SUCCEEDED;
            }
            if (config.moveTargetAfterHit()) {
                targetStart = randomTargetStart(true);
            }
            return ClickResult.HIT;
        }
        misses++;
        if (misses > config.maxMisses()) {
            state = MinigameState.FAILED;
            return ClickResult.FAILED;
        }
        return ClickResult.MISS;
    }

    /** Adds time back to the current round without reopening a finished round. */
    public void addTimeBonus(int bonusTicks) {
        if (state != MinigameState.ACTIVE || bonusTicks <= 0) {
            return;
        }
        elapsedTicks = Math.max(0, elapsedTicks - bonusTicks);
    }

    private void moveIndicator() {
        int next = indicatorPosition + direction;
        if (next >= config.barLength() || next < 0) {
            direction = -direction;
            next = indicatorPosition + direction;
        }
        indicatorPosition = next;
    }

    private int randomTargetStart(boolean keepReachable) {
        int maxStart = config.barLength() - config.targetWidth();
        if (!keepReachable || config.targetRelocationMaxDistance() >= config.barLength()) {
            return random.nextInt(maxStart + 1);
        }

        int reachableCount = 0;
        for (int start = 0; start <= maxStart; start++) {
            if (targetIsReachable(start, config.targetRelocationMaxDistance())) {
                reachableCount++;
            }
        }
        if (reachableCount == 0) {
            return Math.min(Math.max(indicatorPosition - config.targetWidth() / 2, 0), maxStart);
        }

        int selected = random.nextInt(reachableCount);
        for (int start = 0; start <= maxStart; start++) {
            if (targetIsReachable(start, config.targetRelocationMaxDistance()) && selected-- == 0) {
                return start;
            }
        }
        return Math.min(Math.max(indicatorPosition - config.targetWidth() / 2, 0), maxStart);
    }

    private boolean targetIsReachable(int start, int maxSteps) {
        int simulatedPosition = indicatorPosition;
        int simulatedDirection = direction;
        for (int step = 0; step <= maxSteps; step++) {
            if (simulatedPosition >= start && simulatedPosition < start + config.targetWidth()) {
                return true;
            }
            int next = simulatedPosition + simulatedDirection;
            if (next >= config.barLength() || next < 0) {
                simulatedDirection = -simulatedDirection;
                next = simulatedPosition + simulatedDirection;
            }
            simulatedPosition = next;
        }
        return false;
    }

    public boolean isIndicatorInTarget() {
        return indicatorPosition >= targetStart
                && indicatorPosition < targetStart + config.targetWidth();
    }

    public MinigameState state() {
        return state;
    }

    public MinigameConfig config() {
        return config;
    }

    public int indicatorPosition() {
        return indicatorPosition;
    }

    public int direction() {
        return direction;
    }

    public int targetStart() {
        return targetStart;
    }

    public int targetEndExclusive() {
        return targetStart + config.targetWidth();
    }

    public int hits() {
        return hits;
    }

    public int requiredHits() {
        return requiredHits;
    }

    public int misses() {
        return misses;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public int timeLeftTicks() {
        return Math.max(0, config.timeoutTicks() - elapsedTicks);
    }
}
