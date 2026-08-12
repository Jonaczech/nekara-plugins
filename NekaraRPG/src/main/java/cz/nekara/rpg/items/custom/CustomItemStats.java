package cz.nekara.rpg.items.custom;

/** Optional final equipment attributes stored with a custom item definition. */
public record CustomItemStats(
        Double attackDamage,
        Double attackSpeed,
        Double armor,
        Double armorToughness,
        Double maxHealthBonus
) {
    public static final CustomItemStats EMPTY = new CustomItemStats(null, null, null, null, null);

    public CustomItemStats {
        validate("attackDamage", attackDamage, -1024.0, 1024.0);
        validate("attackSpeed", attackSpeed, -10.0, 100.0);
        validate("armor", armor, 0.0, 100.0);
        validate("armorToughness", armorToughness, 0.0, 100.0);
        validate("maxHealthBonus", maxHealthBonus, -20.0, 2048.0);
    }

    public CustomItemStats with(CustomItemStat stat, Double value) {
        return switch (stat) {
            case ATTACK_DAMAGE -> new CustomItemStats(value, attackSpeed, armor, armorToughness, maxHealthBonus);
            case ATTACK_SPEED -> new CustomItemStats(attackDamage, value, armor, armorToughness, maxHealthBonus);
            case ARMOR -> new CustomItemStats(attackDamage, attackSpeed, value, armorToughness, maxHealthBonus);
            case ARMOR_TOUGHNESS -> new CustomItemStats(attackDamage, attackSpeed, armor, value, maxHealthBonus);
            case MAX_HEALTH_BONUS -> new CustomItemStats(attackDamage, attackSpeed, armor, armorToughness, value);
        };
    }

    public Double value(CustomItemStat stat) {
        return switch (stat) {
            case ATTACK_DAMAGE -> attackDamage;
            case ATTACK_SPEED -> attackSpeed;
            case ARMOR -> armor;
            case ARMOR_TOUGHNESS -> armorToughness;
            case MAX_HEALTH_BONUS -> maxHealthBonus;
        };
    }

    private static void validate(String field, Double value, double minimum, double maximum) {
        if (value != null && (!Double.isFinite(value) || value < minimum || value > maximum)) {
            throw new IllegalArgumentException(field + " must be between " + minimum + " and " + maximum);
        }
    }
}
