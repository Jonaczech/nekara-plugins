package cz.nekara.rpg.echovein;

import java.util.List;

public final class EchoVeinMath {
    private EchoVeinMath() {
    }

    public static boolean winsChance(double randomRoll, double chance) {
        return Double.isFinite(randomRoll)
                && randomRoll >= 0.0
                && Double.isFinite(chance)
                && chance > 0.0
                && randomRoll < chance;
    }

    public static double migratePreviousDefaultTriggerChance(double configuredChance) {
        return Double.isFinite(configuredChance)
                && Math.abs(configuredChance - 0.04) < 0.000_001
                ? 0.05
                : configuredChance;
    }

    public static double bonusExperience(double sourceExperience, double multiplier) {
        if (!Double.isFinite(sourceExperience) || sourceExperience <= 0.0
                || !Double.isFinite(multiplier) || multiplier <= 0.0) {
            return 0.0;
        }
        return sourceExperience * multiplier;
    }

    public static int weightedUnitIndex(List<Integer> amounts, long ticket) {
        long total = 0L;
        for (int amount : amounts) {
            if (amount > 0) {
                total = Long.MAX_VALUE - total < amount ? Long.MAX_VALUE : total + amount;
            }
        }
        if (total <= 0L || ticket < 0L || ticket >= total) {
            return -1;
        }

        long cursor = ticket;
        for (int index = 0; index < amounts.size(); index++) {
            int amount = amounts.get(index);
            if (amount <= 0) {
                continue;
            }
            if (cursor < amount) {
                return index;
            }
            cursor -= amount;
        }
        return -1;
    }
}
