package cz.nekara.rpg.modules.skills;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EquipmentProficiencyPolicyTest {
    @Test
    void penaltiesAreBoundedAndPreservePartialArmorFeedback() {
        assertEquals(-0.10, EquipmentProficiencyPolicy.armorMovementPenalty(4), 0.000001);
        assertEquals(1.60, EquipmentProficiencyPolicy.armorDamageMultiplier(4), 0.000001);
        assertEquals(-0.85, EquipmentProficiencyPolicy.toolBreakSpeedModifier(), 0.000001);
        assertEquals(0.40, EquipmentProficiencyPolicy.weaponDamageMultiplier(), 0.000001);
    }
}
