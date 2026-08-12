package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.items.weapons.WeaponTier;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CraftingTierPolicyTest {
    @Test
    void materialRequirementsFollowTheCraftsmanshipProgression() {
        assertEquals(0, CraftingTierPolicy.requiredSmithingLevel(Material.WOODEN_SWORD));
        assertEquals(0, CraftingTierPolicy.requiredSmithingLevel(Material.LEATHER_CHESTPLATE));
        assertEquals(0, CraftingTierPolicy.requiredSmithingLevel(Material.STONE_AXE));
        assertEquals(10, CraftingTierPolicy.requiredSmithingLevel(Material.COPPER_SPEAR));
        assertEquals(15, CraftingTierPolicy.requiredSmithingLevel(Material.GOLDEN_HELMET));
        assertEquals(20, CraftingTierPolicy.requiredSmithingLevel(Material.IRON_CHESTPLATE));
        assertEquals(20, CraftingTierPolicy.requiredSmithingLevel(Material.CHAINMAIL_BOOTS));
        assertEquals(50, CraftingTierPolicy.requiredSmithingLevel(Material.DIAMOND_SWORD));
        assertEquals(80, CraftingTierPolicy.requiredSmithingLevel(Material.NETHERITE_PICKAXE));
    }

    @Test
    void customWeaponTiersUseTheSameThresholds() {
        assertEquals(0, CraftingTierPolicy.requiredSmithingLevel(WeaponTier.WOODEN));
        assertEquals(0, CraftingTierPolicy.requiredSmithingLevel(WeaponTier.STONE));
        assertEquals(10, CraftingTierPolicy.requiredSmithingLevel(WeaponTier.COPPER));
        assertEquals(15, CraftingTierPolicy.requiredSmithingLevel(WeaponTier.GOLDEN));
        assertEquals(20, CraftingTierPolicy.requiredSmithingLevel(WeaponTier.IRON));
        assertEquals(50, CraftingTierPolicy.requiredSmithingLevel(WeaponTier.DIAMOND));
        assertEquals(80, CraftingTierPolicy.requiredSmithingLevel(WeaponTier.NETHERITE));
    }
}
