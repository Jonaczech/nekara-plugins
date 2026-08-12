package cz.nekara.rpg.modules.runes;

import org.bukkit.Material;

final class RunePolicy {
    private RunePolicy() {
    }

    static RuneTier tierFor(int enchantingLevel, int basicRuneRank, int advancedRuneRank, int masterRuneRank) {
        if (basicRuneRank < 1 || enchantingLevel < 1) {
            throw new IllegalArgumentException("The basic rune perk and level 1 are required");
        }
        if (enchantingLevel >= 70 && advancedRuneRank >= 3 && masterRuneRank >= 3) return RuneTier.III;
        if (enchantingLevel >= 30 && advancedRuneRank >= 3) return RuneTier.II;
        return RuneTier.I;
    }

    static boolean canSelectTier(RuneTier tier, int enchantingLevel, int basicRuneRank,
                                 int advancedRuneRank, int masterRuneRank) {
        if (basicRuneRank < 1 || enchantingLevel < 1) return false;
        return switch (tier) {
            case I -> true;
            case II -> enchantingLevel >= 30 && advancedRuneRank >= 3;
            case III -> enchantingLevel >= 70 && advancedRuneRank >= 3 && masterRuneRank >= 3;
        };
    }

    static int baseExperienceCost(RuneTier tier) { return 3 + tier.value() * 3; }

    static int experienceCost(RuneTier tier, double reduction) {
        double bounded = Math.max(0.0, Math.min(0.90, reduction));
        return Math.max(1, (int) Math.ceil(baseExperienceCost(tier) * (1.0 - bounded)));
    }

    static int dyeCost(RuneTier tier) { return tier.value(); }

    static boolean preservesDye(double chance, double roll) {
        return roll < Math.max(0.0, Math.min(1.0, chance));
    }

    static double engravingReturnChance(boolean runeMemory, boolean newGamePlus) {
        if (!runeMemory) return 0.0;
        return newGamePlus ? 0.20 : 0.10;
    }

    static int memoryExperienceRefund(RuneTier tier) {
        return Math.max(1, (int) Math.round(baseExperienceCost(tier) * 0.25));
    }
    static boolean supports(RuneTarget target, Material material) {
        String name = material.name();
        return switch (target) {
            case WEAPON -> name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_SPEAR")
                || material == Material.MACE || material == Material.TRIDENT;
            case BOW -> material == Material.BOW || material == Material.CROSSBOW;
            case BOOTS -> name.endsWith("_BOOTS");
            case TOOL -> name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE");
            case EQUIPMENT -> name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.endsWith("_SWORD")
                || name.endsWith("_AXE") || name.endsWith("_SPEAR") || name.endsWith("_PICKAXE")
                || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || material == Material.MACE
                || material == Material.TRIDENT || material == Material.BOW || material == Material.CROSSBOW;
        };
    }
}
