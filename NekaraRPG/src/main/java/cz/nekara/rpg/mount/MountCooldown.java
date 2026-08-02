package cz.nekara.rpg.mount;

import java.time.Duration;
import java.time.Instant;

public final class MountCooldown {
    private MountCooldown() {
    }

    public static boolean isActive(Instant until, Instant now) {
        return until != null && until.isAfter(now);
    }

    public static long remainingSeconds(Instant until, Instant now) {
        if (!isActive(until, now)) {
            return 0L;
        }
        long milliseconds = Duration.between(now, until).toMillis();
        return Math.max(1L, (milliseconds + 999L) / 1_000L);
    }

    public static String format(long totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long days = seconds / 86_400L;
        long hours = (seconds % 86_400L) / 3_600L;
        long minutes = (seconds % 3_600L) / 60L;
        long remainder = seconds % 60L;
        if (days > 0L) {
            return days + " d " + hours + " h";
        }
        if (hours > 0L) {
            return hours + " h " + minutes + " min";
        }
        if (minutes > 0L) {
            return minutes + " min " + remainder + " s";
        }
        return remainder + " s";
    }
}
