package cz.nekara.rpg.skills.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArmorProtectionResolverTest {
    @Test
    void plateResistsSlashMoreThanImpact() {
        String[] armor = fullSet("IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS");

        assertEquals(0.86, ArmorProtectionResolver.damageMultiplier(armor, DamageType.SLASH), 0.000001);
        assertEquals(0.96, ArmorProtectionResolver.damageMultiplier(armor, DamageType.IMPACT), 0.000001);
    }

    @Test
    void bleedDoesNotReceiveAdditionalArmorProtection() {
        String[] armor = fullSet("NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS");

        assertEquals(1.0, ArmorProtectionResolver.damageMultiplier(armor, DamageType.BLEED), 0.000001);
    }

    private static String[] fullSet(String helmet, String chestplate, String leggings, String boots) {
        return new String[] { boots, leggings, chestplate, helmet };
    }
}
