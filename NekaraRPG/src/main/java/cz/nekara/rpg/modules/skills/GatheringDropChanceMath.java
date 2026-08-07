package cz.nekara.rpg.modules.skills;

final class GatheringDropChanceMath {
    private GatheringDropChanceMath() {
    }

    static double atLeastOneBonusDrop(
        double doubleDropChance,
        double tripleDropChance,
        double... independentBonusDropChances
    ) {
        double noBonusChance = (1.0 - chance(doubleDropChance)) * (1.0 - chance(tripleDropChance));
        for (double independentChance : independentBonusDropChances) {
            noBonusChance *= 1.0 - chance(independentChance);
        }
        return 1.0 - noBonusChance;
    }

    private static double chance(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("Drop chance must be between zero and one");
        }
        return value;
    }
}
