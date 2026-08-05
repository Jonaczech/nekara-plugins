package cz.nekara.rpg.modules.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CraftingExperiencePolicyTest {
    @Test
    void rewardsEquipmentAndUsefulPermanentCrafts() {
        assertEquals("equipment_craft", CraftingExperiencePolicy.sourceFor("IRON_SWORD", true, false));
        assertEquals("construction_craft", CraftingExperiencePolicy.sourceFor("OAK_PLANKS", false, false));
        assertEquals("utility_craft", CraftingExperiencePolicy.sourceFor("CRAFTING_TABLE", false, false));
    }

    @Test
    void rejectsFoodAndReversibleResourceConversions() {
        assertNull(CraftingExperiencePolicy.sourceFor("COOKED_BEEF", false, true));
        assertNull(CraftingExperiencePolicy.sourceFor("IRON_INGOT", false, false));
        assertNull(CraftingExperiencePolicy.sourceFor("IRON_BLOCK", false, false));
    }
}
