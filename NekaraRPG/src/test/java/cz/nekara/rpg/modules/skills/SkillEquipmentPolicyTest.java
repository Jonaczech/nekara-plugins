package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.skills.SkillId;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillEquipmentPolicyTest {
    @Test
    void classifiesOnlySupportedMeleeFamilies() {
        assertEquals(SkillId.MARTIAL_ARTS,
            SkillEquipmentPolicy.meleeSkill(Material.AIR).orElseThrow());
        assertEquals(SkillId.LIGHT_WEAPONS,
            SkillEquipmentPolicy.meleeSkill(Material.IRON_SWORD).orElseThrow());
        assertEquals(SkillId.HEAVY_WEAPONS,
            SkillEquipmentPolicy.meleeSkill(Material.DIAMOND_AXE).orElseThrow());
        assertTrue(SkillEquipmentPolicy.meleeSkill(Material.IRON_PICKAXE).isEmpty());
    }

    @Test
    void fullArmorSetsMustUseOneWeightClass() {
        assertEquals(SkillId.LIGHT_ARMOR, SkillEquipmentPolicy.armorSkill(new Material[]{
            Material.DIAMOND_BOOTS, Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_CHESTPLATE, Material.DIAMOND_HELMET
        }).orElseThrow());
        assertEquals(SkillId.HEAVY_ARMOR, SkillEquipmentPolicy.armorSkill(new Material[]{
            Material.IRON_BOOTS, Material.GOLDEN_LEGGINGS,
            Material.NETHERITE_CHESTPLATE, Material.IRON_HELMET
        }).orElseThrow());
        assertTrue(SkillEquipmentPolicy.armorSkill(new Material[]{
            Material.LEATHER_BOOTS, Material.IRON_LEGGINGS,
            Material.DIAMOND_CHESTPLATE, Material.IRON_HELMET
        }).isEmpty());
    }

    @Test
    void recognizesEquipmentCraftsWithoutTreatingMaterialsAsProducts() {
        assertTrue(SkillEquipmentPolicy.isSmithingProduct(Material.BOW));
        assertTrue(SkillEquipmentPolicy.isSmithingProduct(Material.NETHERITE_CHESTPLATE));
        assertTrue(!SkillEquipmentPolicy.isSmithingProduct(Material.IRON_INGOT));
    }
}
