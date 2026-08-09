package cz.nekara.rpg.items.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class WeaponAttributesTest {
    @Test
    void netheriteWeaponsHaveDistinctFinalCombatAttributes() {
        assertAttributes(WeaponFamily.DAGGER, 8.0, 2.0);
        assertAttributes(WeaponFamily.SWORD, 9.0, 1.6);
        assertAttributes(WeaponFamily.SPEAR, 9.0, 1.35);
        assertAttributes(WeaponFamily.AXE, 11.0, 0.95);
        assertAttributes(WeaponFamily.GREATSWORD, 12.0, 0.8);
        assertAttributes(WeaponFamily.HAMMER, 13.0, 0.7);
    }

    @Test
    void goldenWeaponsTradeDamageProgressionForSpeed() {
        WeaponAttributes goldenSword = WeaponAttributes.forWeapon(WeaponFamily.SWORD, WeaponTier.GOLDEN);
        WeaponAttributes copperSword = WeaponAttributes.forWeapon(WeaponFamily.SWORD, WeaponTier.COPPER);

        assertEquals(copperSword.attackDamage(), goldenSword.attackDamage());
        assertEquals(0.15, goldenSword.attackSpeed() - copperSword.attackSpeed(), 0.000001);
    }

    @Test
    void diamondVanillaFamiliesUseTheSharedWeaponTable() {
        assertAttributes(WeaponFamily.SWORD, WeaponTier.DIAMOND, 8.0, 1.6);
        assertAttributes(WeaponFamily.SPEAR, WeaponTier.DIAMOND, 8.0, 1.35);
        assertAttributes(WeaponFamily.AXE, WeaponTier.DIAMOND, 10.0, 0.95);
    }

    private static void assertAttributes(WeaponFamily family, double damage, double speed) {
        assertAttributes(family, WeaponTier.NETHERITE, damage, speed);
    }

    private static void assertAttributes(WeaponFamily family, WeaponTier tier, double damage, double speed) {
        WeaponAttributes attributes = WeaponAttributes.forWeapon(family, tier);
        assertEquals(damage, attributes.attackDamage());
        assertEquals(speed, attributes.attackSpeed(), 0.000001);
    }
}
