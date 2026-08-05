package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.items.weapons.WeaponCatalog;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Optional;

final class SkillEquipmentPolicy {
    private SkillEquipmentPolicy() {
    }

    static Optional<SkillId> meleeSkill(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        return WeaponCatalog.resolve(item).map(definition -> definition.family().skill());
    }

    static Optional<SkillId> meleeSkill(Material material) {
        if (material == Material.AIR) {
            return Optional.empty();
        }
        return WeaponCatalog.resolveVanilla(material).map(definition -> definition.family().skill());
    }

    static Optional<SkillId> armorSkill(PlayerInventory inventory) {
        return armorSkill(inventory.getArmorContents());
    }

    static Optional<SkillId> armorSkill(ItemStack[] armor) {
        Material[] materials = new Material[armor.length];
        for (int index = 0; index < armor.length; index++) {
            materials[index] = armor[index] == null ? Material.AIR : armor[index].getType();
        }
        return armorSkill(materials);
    }

    static Optional<SkillId> armorSkill(Material[] armor) {
        if (armor.length != 4) {
            return Optional.empty();
        }
        boolean light = true;
        boolean heavy = true;
        for (Material material : armor) {
            if (material == null || material == Material.AIR) {
                return Optional.empty();
            }
            String name = material.name();
            light &= name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_")
                || name.startsWith("DIAMOND_");
            heavy &= name.startsWith("COPPER_") || name.startsWith("IRON_")
                || name.startsWith("GOLDEN_") || name.startsWith("NETHERITE_");
        }
        if (light) {
            return Optional.of(SkillId.LIGHT_ARMOR);
        }
        if (heavy) {
            return Optional.of(SkillId.HEAVY_ARMOR);
        }
        return Optional.empty();
    }

    static boolean wearsLeatherArmor(PlayerInventory inventory) {
        for (ItemStack item : inventory.getArmorContents()) {
            if (item == null || item.getType().isAir() || !item.getType().name().startsWith("LEATHER_")) {
                return false;
            }
        }
        return true;
    }

    static boolean isSmithingProduct(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return WeaponCatalog.isCustomWeapon(item) || isSmithingProduct(item.getType());
    }

    static boolean isSmithingProduct(Material material) {
        if (material == null || material == Material.AIR) {
            return false;
        }
        String name = material.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE")
            || name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_HELMET")
            || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
            || name.endsWith("_SPEAR") || name.equals("MACE") || material == Material.TRIDENT
            || material == Material.SHIELD || material == Material.BOW
            || material == Material.CROSSBOW;
    }
}
