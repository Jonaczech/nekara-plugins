package cz.nekara.rpg.compatibility;

import java.util.Set;

public final class ValhallaRestedExperienceMath {
    private static final Set<String> ELIGIBLE_REASONS = Set.of("SKILL_ACTION", "EXP_SHARE");

    private ValhallaRestedExperienceMath() {
    }

    public static boolean isEligibleReason(String reason) {
        return reason != null && ELIGIBLE_REASONS.contains(reason);
    }

    public static double applyMultiplier(double amount, double multiplier) {
        if (!Double.isFinite(amount) || amount <= 0.0
                || !Double.isFinite(multiplier) || multiplier < 1.0) {
            return amount;
        }
        double scaled = amount * multiplier;
        return Double.isFinite(scaled) ? scaled : amount;
    }
}
