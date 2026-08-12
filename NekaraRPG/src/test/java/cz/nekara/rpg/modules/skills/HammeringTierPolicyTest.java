package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cz.nekara.rpg.items.weapons.WeaponTier;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class HammeringTierPolicyTest {
    @Test
    void mapsArmorAndToolMaterialsToRequiredTier() {
        assertEquals(WeaponTier.COPPER, HammeringTierPolicy.requiredTier(Material.COPPER_CHESTPLATE).orElseThrow());
        assertEquals(WeaponTier.IRON, HammeringTierPolicy.requiredTier(Material.CHAINMAIL_BOOTS).orElseThrow());
        assertEquals(WeaponTier.DIAMOND, HammeringTierPolicy.requiredTier(Material.DIAMOND_PICKAXE).orElseThrow());
    }

    @Test
    void rejectsWeakerHammerAndAcceptsEqualOrHigherHammer() {
        assertFalse(HammeringTierPolicy.isSufficient(WeaponTier.COPPER, WeaponTier.DIAMOND));
        assertTrue(HammeringTierPolicy.isSufficient(WeaponTier.IRON, WeaponTier.IRON));
        assertTrue(HammeringTierPolicy.isSufficient(WeaponTier.NETHERITE, WeaponTier.DIAMOND));
    }

    @Test
    void goldRemainsBelowIronInSmithingProgression() {
        assertFalse(HammeringTierPolicy.isSufficient(WeaponTier.GOLDEN, WeaponTier.IRON));
        assertTrue(HammeringTierPolicy.isSufficient(WeaponTier.IRON, WeaponTier.GOLDEN));
    }
}
