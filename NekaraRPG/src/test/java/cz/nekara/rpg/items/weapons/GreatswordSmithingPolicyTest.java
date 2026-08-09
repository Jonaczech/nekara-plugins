package cz.nekara.rpg.items.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GreatswordSmithingPolicyTest {
    @Test
    void turnsMatchingVanillaSwordAndMaterialIntoEachAvailableGreatswordTier() {
        for (String tier : new String[]{"STONE", "COPPER", "IRON", "GOLDEN", "DIAMOND"}) {
            assertEquals(tier, GreatswordSmithingPolicy.result(
                "AIR", tier + "_SWORD", ingredient(tier)).orElseThrow());
        }
    }

    @Test
    void rejectsTemplatesAndMismatchedMaterials() {
        assertTrue(GreatswordSmithingPolicy.result(
            "NETHERITE_UPGRADE_SMITHING_TEMPLATE", "IRON_SWORD", "IRON_INGOT").isEmpty());
        assertTrue(GreatswordSmithingPolicy.result(
            "AIR", "IRON_SWORD", "GOLD_INGOT").isEmpty());
    }

    private static String ingredient(String tier) {
        return switch (tier) {
            case "STONE" -> "COBBLESTONE";
            case "COPPER" -> "COPPER_INGOT";
            case "IRON" -> "IRON_INGOT";
            case "GOLDEN" -> "GOLD_INGOT";
            case "DIAMOND" -> "DIAMOND";
            default -> throw new IllegalArgumentException(tier);
        };
    }
}
