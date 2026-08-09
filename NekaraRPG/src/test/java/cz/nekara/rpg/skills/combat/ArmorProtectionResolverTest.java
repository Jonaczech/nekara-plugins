package cz.nekara.rpg.skills.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArmorProtectionResolverTest {
    @Test
    void plateResistsSlashMoreThanImpact() {
        String[] armor = fullSet("IRON_HELMET", "IRON_CHESTPLATE", "IRON_LEGGINGS", "IRON_BOOTS");

        assertEquals(0.91, ArmorProtectionResolver.damageMultiplier(armor, DamageType.SLASH), 0.000001);
        assertEquals(0.97, ArmorProtectionResolver.damageMultiplier(armor, DamageType.IMPACT), 0.000001);
    }

    @Test
    void penetrationOnlyBypassesTypeProtection() {
        String[] armor = fullSet("NETHERITE_HELMET", "NETHERITE_CHESTPLATE", "NETHERITE_LEGGINGS", "NETHERITE_BOOTS");

        assertEquals(0.937, ArmorProtectionResolver.damageMultiplier(armor, DamageType.PIERCE, 1.0, 0.30), 0.000001);
        assertEquals(1.0, ArmorProtectionResolver.damageMultiplier(
            new String[] {"AIR", "AIR", "AIR", "AIR"}, DamageType.PIERCE, 1.0, 0.30), 0.000001);
    }

    @Test
    void copperAndGoldenArmorHaveTheirOwnTypeProtection() {
        assertEquals(0.93, ArmorProtectionResolver.damageMultiplier(
            fullSet("COPPER_HELMET", "COPPER_CHESTPLATE", "COPPER_LEGGINGS", "COPPER_BOOTS"), DamageType.SLASH), 0.000001);
        assertEquals(0.96, ArmorProtectionResolver.damageMultiplier(
            fullSet("GOLDEN_HELMET", "GOLDEN_CHESTPLATE", "GOLDEN_LEGGINGS", "GOLDEN_BOOTS"), DamageType.PIERCE), 0.000001);
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
