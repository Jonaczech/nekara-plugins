package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.items.weapons.WeaponDefinition;
import cz.nekara.rpg.items.weapons.WeaponFamily;
import cz.nekara.rpg.items.weapons.WeaponTier;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentMobilityPolicyTest {
    @Test
    void keepsLeatherAndCopperPenaltyFreeButGatesIronArmor() {
        assertEquals(0.0, EquipmentMobilityPolicy.armorPenalty(
            new Material[] {Material.LEATHER_HELMET, Material.COPPER_CHESTPLATE, Material.COPPER_LEGGINGS, Material.COPPER_BOOTS},
            false, false, false, false));
        assertEquals(-0.12, EquipmentMobilityPolicy.armorPenalty(
            new Material[] {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS},
            false, false, false, false));
        assertEquals(0.0, EquipmentMobilityPolicy.armorPenalty(
            new Material[] {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS},
            false, false, true, false));
    }

    @Test
    void makesCopperWeaponsEarlyGameButGatesIronAndAbove() {
        WeaponDefinition copper = new WeaponDefinition(WeaponFamily.SWORD, WeaponTier.COPPER, Material.COPPER_SWORD);
        WeaponDefinition iron = new WeaponDefinition(WeaponFamily.HAMMER, WeaponTier.IRON, Material.IRON_AXE);
        assertEquals(0.0, EquipmentMobilityPolicy.weaponPenalty(copper, false, false, false));
        assertEquals(-0.04, EquipmentMobilityPolicy.weaponPenalty(iron, false, false, false));
        assertEquals(0.0, EquipmentMobilityPolicy.weaponPenalty(iron, true, false, false));
    }
}
