package cz.nekara.rpg.campfire;

public final class CampfireRestMath {
    private CampfireRestMath() {
    }

    public static double groupMultiplier(int playerCount, double perExtraPlayer, double maximum) {
        int extraPlayers = Math.max(0, playerCount - 1);
        return Math.min(maximum, 1.0 + extraPlayers * perExtraPlayer);
    }

    public static HungerLossResult scaleHungerLoss(int rawLoss, double multiplier, double carry) {
        if (rawLoss <= 0) {
            return new HungerLossResult(0, carry);
        }
        double scaledLoss = rawLoss * multiplier + carry;
        int appliedLoss = (int) Math.floor(scaledLoss);
        return new HungerLossResult(appliedLoss, scaledLoss - appliedLoss);
    }

    public static int restedDurationSeconds(int baseDurationSeconds, int featureCount,
                                            int durationPerFeatureSeconds) {
        return baseDurationSeconds
                + Math.max(0, featureCount) * Math.max(0, durationPerFeatureSeconds);
    }

    public static String formatCountdown(long remainingSeconds) {
        long safeSeconds = Math.max(0L, remainingSeconds);
        return "%d:%02d".formatted(safeSeconds / 60L, safeSeconds % 60L);
    }

    public static boolean shouldShowRestedTimer(
            boolean indicatorEnabled,
            boolean campfireCharging,
            boolean fishingMinigameActive
    ) {
        return indicatorEnabled && !campfireCharging && !fishingMinigameActive;
    }

    public record HungerLossResult(int appliedLoss, double carry) {
    }
}
