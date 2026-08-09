package cz.nekara.rpg.items.weapons;

import java.util.Optional;

/** Validates the vanilla-sword conversion used to forge a two-handed sword. */
final class GreatswordSmithingPolicy {
    private GreatswordSmithingPolicy() {
    }

    static Optional<String> result(String template, String base, String addition) {
        if (template != null && !"AIR".equals(template)) {
            return Optional.empty();
        }
        for (String tier : new String[]{"STONE", "COPPER", "IRON", "GOLDEN", "DIAMOND"}) {
            if ((tier + "_SWORD").equals(base) && craftingIngredient(tier).equals(addition)) {
                return Optional.of(tier);
            }
        }
        return Optional.empty();
    }

    private static String craftingIngredient(String tier) {
        return switch (tier) {
            case "STONE" -> "COBBLESTONE";
            case "COPPER" -> "COPPER_INGOT";
            case "IRON" -> "IRON_INGOT";
            case "GOLDEN" -> "GOLD_INGOT";
            case "DIAMOND" -> "DIAMOND";
            default -> throw new IllegalArgumentException("Unsupported greatsword tier: " + tier);
        };
    }
}
