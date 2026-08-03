package cz.nekara.rpg.skills.stats;

public enum StatId {
    DAMAGE_MULTIPLIER(1.0, 0.0, 100.0),
    CRITICAL_CHANCE(0.0, 0.0, 1.0),
    CRITICAL_DAMAGE_MULTIPLIER(1.5, 1.0, 100.0),
    BLEED_CHANCE(0.0, 0.0, 1.0),
    BLEED_DAMAGE_MULTIPLIER(1.0, 0.0, 100.0),
    STUN_CHANCE(0.0, 0.0, 1.0),
    DOUBLE_DROP_CHANCE(0.0, 0.0, 1.0),
    TRIPLE_DROP_CHANCE(0.0, 0.0, 1.0),
    MINING_SPEED(1.0, 0.0, 100.0),
    WOODCUTTING_SPEED(1.0, 0.0, 100.0),
    CROP_GROWTH_MULTIPLIER(1.0, 0.0, 100.0),
    ARMOR_MULTIPLIER(1.0, 0.0, 100.0),
    ARMOR_PENETRATION(0.0, 0.0, 1.0),
    DODGE_CHANCE(0.0, 0.0, 1.0),
    EXPERIENCE_MULTIPLIER(1.0, 0.0, 100.0);

    private final double defaultValue;
    private final double minimumValue;
    private final double maximumValue;

    StatId(double defaultValue, double minimumValue, double maximumValue) {
        this.defaultValue = defaultValue;
        this.minimumValue = minimumValue;
        this.maximumValue = maximumValue;
    }

    public double defaultValue() {
        return defaultValue;
    }

    public double clamp(double value) {
        return Math.max(minimumValue, Math.min(maximumValue, value));
    }
}
