package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cz.nekara.rpg.items.weapons.WeaponDefinition;
import cz.nekara.rpg.items.weapons.WeaponFamily;
import cz.nekara.rpg.items.weapons.WeaponTier;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class EquipmentProficiencyPolicyTest {
    @Test
    void penaltiesAreBoundedAndPreservePartialArmorFeedback() {
        assertEquals(-0.10, EquipmentProficiencyPolicy.armorMovementPenalty(4), 0.000001);
        assertEquals(1.60, EquipmentProficiencyPolicy.armorDamageMultiplier(4), 0.000001);
        assertEquals(-0.85, EquipmentProficiencyPolicy.toolBreakSpeedModifier(), 0.000001);
        assertEquals(0.40, EquipmentProficiencyPolicy.weaponDamageMultiplier(), 0.000001);
    }

    @Test
    void woodenWeaponHasNoProficiencyRequirement() {
        WeaponDefinition woodenSword = new WeaponDefinition(
            WeaponFamily.SWORD, WeaponTier.WOODEN, Material.WOODEN_SWORD);

        assertTrue(EquipmentProficiencyPolicy.weapon(woodenSword).isEmpty());
    }
}
