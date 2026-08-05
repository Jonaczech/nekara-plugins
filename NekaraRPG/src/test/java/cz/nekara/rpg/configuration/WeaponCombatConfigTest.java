package cz.nekara.rpg.configuration;

import cz.nekara.rpg.items.weapons.WeaponFamily;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponCombatConfigTest {
    @Test
    void defaultsMapEffectsToTheirOwnWeaponFamily() {
        WeaponCombatConfig config = WeaponCombatConfig.defaults();

        assertEquals(0.06, config.criticalChance(WeaponFamily.DAGGER));
        assertEquals(0.04, config.criticalChance(WeaponFamily.AXE));
        assertEquals(0.04, config.bleedChance(WeaponFamily.SWORD));
        assertEquals(0.05, config.bleedChance(WeaponFamily.GREATSWORD));
        assertEquals(0.12, config.armorPenetration(WeaponFamily.SPEAR));
        assertEquals(0.20, config.armorPenetration(WeaponFamily.HAMMER));
        assertEquals(0.25, config.rearAttackBonus(WeaponFamily.DAGGER));
    }
}
