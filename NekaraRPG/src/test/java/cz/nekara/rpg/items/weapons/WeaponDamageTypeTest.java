package cz.nekara.rpg.items.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cz.nekara.rpg.skills.combat.DamageType;
import org.junit.jupiter.api.Test;

class WeaponDamageTypeTest {
    @Test
    void weaponFamiliesUseTheIntendedPhysicalCategories() {
        assertEquals(DamageType.SLASH, WeaponFamily.SWORD.damageType());
        assertEquals(DamageType.PIERCE, WeaponFamily.DAGGER.damageType());
        assertEquals(DamageType.IMPACT, WeaponFamily.HAMMER.damageType());
    }
}
