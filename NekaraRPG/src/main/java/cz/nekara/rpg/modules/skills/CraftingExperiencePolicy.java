package cz.nekara.rpg.modules.skills;

import org.bukkit.inventory.ItemStack;

/** Keeps Smithing experience tied to real, non-reversible player production. */
final class CraftingExperiencePolicy {
    private CraftingExperiencePolicy() {
    }

    static String sourceFor(ItemStack result) {
        if (result == null || result.getType().isAir()) {
            return null;
        }
        return sourceFor(result.getType().name(), SkillEquipmentPolicy.isSmithingProduct(result),
            result.getType().isEdible());
    }

    static String sourceFor(String materialName, boolean equipment, boolean edible) {
        if (materialName == null || materialName.isBlank() || edible) {
            return null;
        }
        if (equipment) {
            return "equipment_craft";
        }
        if (isReversibleResource(materialName)) {
            return null;
        }
        return isConstruction(materialName) ? "construction_craft" : "utility_craft";
    }

    private static boolean isReversibleResource(String name) {
        return name.endsWith("_INGOT") || name.endsWith("_NUGGET") || name.endsWith("_RAW")
            || name.startsWith("RAW_") || name.endsWith("_BLOCK") || name.equals("COAL")
            || name.equals("CHARCOAL") || name.equals("DIAMOND") || name.equals("EMERALD")
            || name.equals("LAPIS_LAZULI") || name.equals("REDSTONE") || name.equals("QUARTZ")
            || name.equals("AMETHYST_SHARD") || name.equals("RESIN_CLUMP");
    }

    private static boolean isConstruction(String name) {
        return name.endsWith("_PLANKS") || name.endsWith("_STAIRS") || name.endsWith("_SLAB")
            || name.endsWith("_WALL") || name.endsWith("_FENCE") || name.endsWith("_FENCE_GATE")
            || name.endsWith("_DOOR") || name.endsWith("_TRAPDOOR") || name.endsWith("_SIGN")
            || name.endsWith("_HANGING_SIGN") || name.endsWith("_PRESSURE_PLATE")
            || name.endsWith("_CARPET") || name.endsWith("_WOOL") || name.endsWith("_BRICKS")
            || name.endsWith("_TERRACOTTA") || name.endsWith("_GLASS") || name.endsWith("_CONCRETE")
            || name.equals("STICK") || name.equals("BOWL") || name.equals("LADDER")
            || name.equals("SCAFFOLDING") || name.equals("TORCH");
    }
}
