package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.modules.skills.GatheringMaterialPolicy.GatheringTool;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatheringMaterialPolicyTest {
    @Test
    void axesNeverAcceptPickaxes() {
        assertTrue(GatheringMaterialPolicy.suitableTool(
            GatheringTool.AXE, Material.IRON_AXE));
        assertFalse(GatheringMaterialPolicy.suitableTool(
            GatheringTool.AXE, Material.IRON_PICKAXE));
    }

    @Test
    void recognizesOnlyHoesAsFarmingTools() {
        assertTrue(GatheringMaterialPolicy.isHoe(Material.IRON_HOE));
        assertFalse(GatheringMaterialPolicy.isHoe(Material.IRON_SHOVEL));
    }

    @Test
    void oreAndWoodFamiliesIncludeTheirNaturalVariants() {
        assertTrue(GatheringMaterialPolicy.sameOreFamily(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE));
        assertTrue(GatheringMaterialPolicy.sameWoodFamily(
            Material.OAK_LOG, Material.STRIPPED_OAK_WOOD));
        assertFalse(GatheringMaterialPolicy.sameWoodFamily(
            Material.OAK_LOG, Material.SPRUCE_LOG));
    }
}
