package cz.nekara.fishing.configuration;

import java.util.Locale;
import java.util.function.Consumer;

public final class ConfigurationValidator {
    private ConfigurationValidator() {
    }

    public static MinigameConfig validate(MinigameConfig input, Consumer<String> warning) {
        int barLength = input.barLength() >= 2 ? input.barLength() : invalid(warning, "minigame.bar-length", 20);
        int defaultTargetWidth = Math.min(5, barLength - 1);
        int targetWidth = input.targetWidth() > 0 && input.targetWidth() < barLength
                ? input.targetWidth()
                : invalid(warning, "minigame.target-width", defaultTargetWidth);
        int targetRelocationMaxDistance = input.targetRelocationMaxDistance() >= 0
                ? input.targetRelocationMaxDistance()
                : invalid(warning, "minigame.target-relocation-max-distance", 6);
        int period = input.updatePeriodTicks() >= 1
                ? input.updatePeriodTicks()
                : invalid(warning, "minigame.update-period-ticks", 6);
        int requiredHits = input.requiredHits() >= 1
                ? input.requiredHits()
                : invalid(warning, "minigame.required-hits-min", 3);
        int maxRequiredHits = input.maxRequiredHits() >= requiredHits
                ? input.maxRequiredHits()
                : invalid(warning, "minigame.required-hits-max", Math.max(requiredHits, 5));
        int maxMisses = input.maxMisses() >= 0
                ? input.maxMisses()
                : invalid(warning, "minigame.max-misses", 1);
        int timeout = input.timeoutTicks() > 0
                ? input.timeoutTicks()
                : invalid(warning, "minigame.timeout-ticks", 160);
        int timeBonus = input.timeBonusTicks() >= 0
                ? input.timeBonusTicks()
                : invalid(warning, "minigame.time-bonus-ticks", 30);
        double hookPullDistance = Double.isFinite(input.hookPullDistance())
                && input.hookPullDistance() >= 0.0
                && input.hookPullDistance() <= 5.0
                ? input.hookPullDistance()
                : invalidDouble(warning, "minigame.hook-pull-distance", 1.0);
        long debounce = input.inputDebounceMilliseconds() >= 0
                ? input.inputDebounceMilliseconds()
                : invalidLong(warning, "minigame.input-debounce-milliseconds", 150);

        return new MinigameConfig(
                input.enabled(),
                input.display() == null ? DisplayMode.ACTION_BAR : input.display(),
                barLength,
                period,
                targetWidth,
                targetRelocationMaxDistance,
                requiredHits,
                maxRequiredHits,
                maxMisses,
                timeout,
                timeBonus,
                hookPullDistance,
                input.moveTargetAfterHit(),
                input.randomizeStartPosition(),
                input.indicatorDirection() == null ? IndicatorDirection.RANDOM : input.indicatorDirection(),
                debounce
        );
    }

    public static IndicatorDirection parseDirection(String value, Consumer<String> warning) {
        try {
            return IndicatorDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            warning.accept("Invalid minigame.indicator-direction; using RANDOM.");
            return IndicatorDirection.RANDOM;
        }
    }

    public static WorldMode parseWorldMode(String value, Consumer<String> warning) {
        try {
            return WorldMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            warning.accept("Invalid worlds.mode; using ALL.");
            return WorldMode.ALL;
        }
    }

    private static int invalid(Consumer<String> warning, String path, int fallback) {
        warning.accept("Invalid " + path + "; using " + fallback + ".");
        return fallback;
    }

    private static long invalidLong(Consumer<String> warning, String path, long fallback) {
        warning.accept("Invalid " + path + "; using " + fallback + ".");
        return fallback;
    }

    private static double invalidDouble(Consumer<String> warning, String path, double fallback) {
        warning.accept("Invalid " + path + "; using " + fallback + ".");
        return fallback;
    }
}
