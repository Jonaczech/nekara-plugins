package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.items.weapons.WeaponCatalog;
import cz.nekara.rpg.items.weapons.WeaponTier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Progression gate for equipment material tiers; authority remains in craft events. */
final class CraftingTierPolicy {
    private CraftingTierPolicy() {
    }

    static int requiredSmithingLevel(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0;
        }
        return WeaponCatalog.resolve(item)
            .map(definition -> requiredSmithingLevel(definition.tier()))
            .orElseGet(() -> requiredSmithingLevel(item.getType()));
    }

    static int requiredSmithingLevel(Material material) {
        if (material == null || material == Material.AIR || !SkillEquipmentPolicy.isSmithingProduct(material)) {
            return 0;
        }
        String name = material.name();
        if (name.startsWith("NETHERITE_")) return 80;
        if (name.startsWith("DIAMOND_")) return 50;
        if (name.startsWith("IRON_") || name.startsWith("CHAINMAIL_")) return 20;
        if (name.startsWith("GOLDEN_")) return 15;
        if (name.startsWith("COPPER_")) return 10;
        if (name.startsWith("STONE_")) return 0;
        return 0;
    }

    static int requiredSmithingLevel(WeaponTier tier) {
        return switch (tier) {
            case WOODEN -> 0;
            case STONE -> 0;
            case COPPER -> 10;
            case GOLDEN -> 15;
            case IRON -> 20;
            case DIAMOND -> 50;
            case NETHERITE -> 80;
        };
    }
}
