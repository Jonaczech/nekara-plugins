package cz.nekara.rpg.modules.skills;

import java.util.Locale;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

final class GatheringMaterialPolicy {
    private GatheringMaterialPolicy() {
    }

    static boolean suitableTool(GatheringTool tool, ItemStack item) {
        return item != null && suitableTool(tool, item.getType());
    }

    static boolean suitableTool(GatheringTool tool, Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return switch (tool) {
            case PICKAXE -> name.endsWith("_PICKAXE");
            case AXE -> name.endsWith("_AXE") && !name.endsWith("_PICKAXE");
            case SHOVEL -> name.endsWith("_SHOVEL");
        };
    }

    static boolean isOre(Material material) {
        return material != null && (material.name().endsWith("_ORE")
            || material == Material.ANCIENT_DEBRIS);
    }

    static String oreFamily(Material material) {
        String name = material.name().toUpperCase(Locale.ROOT);
        if (name.startsWith("DEEPSLATE_")) {
            return name.substring("DEEPSLATE_".length());
        }
        return name;
    }

    static boolean sameOreFamily(Material first, Material second) {
        return isOre(first) && isOre(second) && oreFamily(first).equals(oreFamily(second));
    }

    static boolean isLog(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD")
            || name.endsWith("_STEM") || name.endsWith("_HYPHAE")
            || material == Material.BAMBOO_BLOCK || material == Material.STRIPPED_BAMBOO_BLOCK;
    }

    static String woodFamily(Material material) {
        String name = material.name();
        if (name.startsWith("STRIPPED_")) {
            name = name.substring("STRIPPED_".length());
        }
        for (String suffix : new String[]{"_LOG", "_WOOD", "_STEM", "_HYPHAE", "_BLOCK"}) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    static boolean sameWoodFamily(Material first, Material second) {
        return isLog(first) && isLog(second) && woodFamily(first).equals(woodFamily(second));
    }

    static boolean isLeaves(Material material) {
        return material != null && material.name().endsWith("_LEAVES");
    }

    static boolean isVeinCluster(Material material) {
        return material == Material.ANDESITE || material == Material.DIORITE || material == Material.GRANITE
            || material == Material.TUFF || material == Material.DEEPSLATE;
    }

    enum GatheringTool {
        PICKAXE,
        AXE,
        SHOVEL
    }
}
