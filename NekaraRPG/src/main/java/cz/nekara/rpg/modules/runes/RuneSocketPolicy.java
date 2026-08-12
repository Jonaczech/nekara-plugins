package cz.nekara.rpg.modules.runes;

/** Pure socket-capacity and experience-bonus rules. */
final class RuneSocketPolicy {
    private RuneSocketPolicy() {
    }

    static int capacityForQuality(int qualityTier) {
        return switch (qualityTier) {
            case 1, 2 -> 1;
            case 3, 4 -> 2;
            case 5 -> 3;
            default -> 0;
        };
    }

    static double experienceBonus(RuneTier tier) {
        return switch (tier) {
            case I -> 0.01;
            case II -> 0.03;
            case III -> 0.05;
        };
    }
}
