package cz.nekara.rpg.echovein;

import java.util.List;

public final class EchoVeinMath {
    private EchoVeinMath() {
    }

    public static boolean canTrigger(
            int miningLevel,
            int minimumLevel,
            long cooldownUntilMillis,
            long nowMillis,
            double randomRoll,
            double triggerChance
    ) {
        return miningLevel >= minimumLevel
                && cooldownUntilMillis <= nowMillis
                && Double.isFinite(randomRoll)
                && randomRoll >= 0.0
                && randomRoll < triggerChance;
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
