package cz.nekara.rpg.items.weapons;

/** Final main-hand combat attributes for Nekara weapon families. */
public record WeaponAttributes(double attackDamage, double attackSpeed) {
    public static WeaponAttributes forWeapon(WeaponDefinition weapon) {
        return forWeapon(weapon.family(), weapon.tier());
    }

    public static WeaponAttributes forWeapon(WeaponFamily family, WeaponTier tier) {
        int materialIndex = tier.ordinal();
        double swordDamage = new double[] {4.0, 5.0, 6.0, 6.0, 6.0, 8.0, 9.0}[materialIndex];
        double damage = switch (family) {
            case DAGGER -> swordDamage - 1.0;
            case SWORD, SPEAR -> swordDamage;
            case AXE -> swordDamage + 2.0;
            case GREATSWORD -> swordDamage + 3.0;
            case HAMMER -> swordDamage + 4.0;
        };
        double speed = switch (family) {
            case DAGGER -> 2.0;
            case SWORD -> 1.6;
            case SPEAR -> 1.35;
            case AXE -> 0.95;
            case GREATSWORD -> 0.8;
            case HAMMER -> 0.7;
        };
        if (tier == WeaponTier.GOLDEN) {
            speed += 0.15;
        }
        return new WeaponAttributes(damage, speed);
    }
}
