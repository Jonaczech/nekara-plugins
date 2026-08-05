package cz.nekara.rpg.configuration;

import cz.nekara.rpg.items.weapons.WeaponFamily;

/** Server-side balance values for the six supported weapon families. */
public record WeaponCombatConfig(
    double daggerCriticalChance,
    double daggerRearAttackBonus,
    double swordBleedChance,
    double spearArmorPenetration,
    double axeCriticalChance,
    double greatswordBleedChance,
    double greatswordCleaveDamageMultiplier,
    double hammerArmorPenetration,
    double hammerStunChance
) {
    public static WeaponCombatConfig defaults() {
        return new WeaponCombatConfig(0.06, 0.25, 0.04, 0.12, 0.04, 0.05, 0.35, 0.20, 0.10);
    }

    public double criticalChance(WeaponFamily family) {
        return switch (family) {
            case DAGGER -> daggerCriticalChance;
            case AXE -> axeCriticalChance;
            default -> 0.0;
        };
    }

    public double bleedChance(WeaponFamily family) {
        return switch (family) {
            case SWORD -> swordBleedChance;
            case GREATSWORD -> greatswordBleedChance;
            default -> 0.0;
        };
    }

    public double armorPenetration(WeaponFamily family) {
        return switch (family) {
            case SPEAR -> spearArmorPenetration;
            case HAMMER -> hammerArmorPenetration;
            default -> 0.0;
        };
    }

    public double rearAttackBonus(WeaponFamily family) {
        return family == WeaponFamily.DAGGER ? daggerRearAttackBonus : 0.0;
    }
}
