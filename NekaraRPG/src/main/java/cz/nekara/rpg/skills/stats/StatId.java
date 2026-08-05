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
    EXPERIENCE_MULTIPLIER(1.0, 0.0, 100.0),
    REPUTATION_GAIN(1.0, 0.0, 100.0),
    TRADE_DISCOUNT(0.0, 0.0, 1.0),
    TRADE_SELECTION_BONUS(0.0, 0.0, 100.0),
    VILLAGER_SKILL(1.0, 0.0, 100.0),
    ITEM_QUALITY(1.0, 0.0, 100.0),
    ENCHANTMENT_POWER(1.0, 0.0, 100.0),
    EXPERIENCE_COST_REDUCTION(0.0, 0.0, 1.0),
    RESOURCE_COST_REDUCTION(0.0, 0.0, 1.0),
    POTION_POWER(1.0, 0.0, 100.0),
    BREWING_SPEED(1.0, 0.0, 100.0),
    THROWING_SPEED(1.0, 0.0, 100.0),
    FURNACE_SPEED(1.0, 0.0, 100.0),
    TNT_POWER(1.0, 0.0, 100.0),
    DIGGING_SPEED(1.0, 0.0, 100.0),
    RARE_DROP_CHANCE(0.0, 0.0, 1.0),
    ANIMAL_DAMAGE_MULTIPLIER(1.0, 0.0, 100.0),
    BEEKEEPING_YIELD(1.0, 0.0, 100.0),
    LUCK(0.0, 0.0, 100.0),
    FISHING_SPEED(1.0, 0.0, 100.0),
    EXPERIENCE_ORB_MULTIPLIER(1.0, 0.0, 100.0),
    ACCURACY(0.0, 0.0, 1.0),
    AMMO_CONSUMPTION_REDUCTION(0.0, 0.0, 1.0),
    POWER_ATTACK_DAMAGE_MULTIPLIER(1.0, 0.0, 100.0),
    HUNGER_CONSUMPTION_REDUCTION(0.0, 0.0, 1.0),
    MOVEMENT_PENALTY_REDUCTION(0.0, 0.0, 1.0),
    DAMAGE_REFLECTION(0.0, 0.0, 1.0),
    HEALTH_REGENERATION(0.0, 0.0, 100.0),
    STATUS_IMMUNITY_REDUCTION(0.0, 0.0, 1.0);

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
