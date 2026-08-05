package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.items.weapons.WeaponCatalog;
import cz.nekara.rpg.items.weapons.WeaponDefinition;
import cz.nekara.rpg.skills.SkillId;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Skill requirements for using equipment after it has been crafted. */
final class EquipmentProficiencyPolicy {
    private EquipmentProficiencyPolicy() {
    }

    static Optional<Requirement> armor(ItemStack item) {
        return item == null ? Optional.empty() : armor(item.getType());
    }

    static Optional<Requirement> armor(Material material) {
        int level = CraftingTierPolicy.requiredSmithingLevel(material);
        if (level == 0 || material == null) return Optional.empty();
        String name = material.name();
        if (!(name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
            || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS"))) {
            return Optional.empty();
        }
        SkillId skill = name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_")
            || name.startsWith("DIAMOND_") ? SkillId.LIGHT_ARMOR : SkillId.HEAVY_ARMOR;
        return Optional.of(new Requirement(skill, level));
    }

    static Optional<Requirement> heldTool(ItemStack item) {
        if (item == null || item.getType().isAir()) return Optional.empty();
        Material material = item.getType();
        int level = CraftingTierPolicy.requiredSmithingLevel(material);
        if (level == 0) return Optional.empty();
        String name = material.name();
        SkillId skill = name.endsWith("_PICKAXE") ? SkillId.MINING
            : name.endsWith("_AXE") && !name.endsWith("_PICKAXE") ? SkillId.WOODCUTTING
            : name.endsWith("_SHOVEL") ? SkillId.DIGGING
            : name.endsWith("_HOE") ? SkillId.FARMING : null;
        return skill == null ? Optional.empty() : Optional.of(new Requirement(skill, level));
    }

    static Optional<Requirement> weapon(ItemStack item) {
        return WeaponCatalog.resolve(item).map(EquipmentProficiencyPolicy::weapon);
    }

    static Requirement weapon(WeaponDefinition weapon) {
        return new Requirement(weapon.family().skill(),
            CraftingTierPolicy.requiredSmithingLevel(weapon.tier()));
    }

    static double armorMovementPenalty(int untrainedPieces) {
        return -0.025 * Math.max(0, untrainedPieces);
    }

    static double weaponMovementPenalty(SkillId skill) {
        return skill == SkillId.HEAVY_WEAPONS ? -0.09 : -0.05;
    }

    static double toolMovementPenalty() {
        return -0.04;
    }

    static double armorDamageMultiplier(int untrainedPieces) {
        return 1.0 + Math.min(4, Math.max(0, untrainedPieces)) * 0.15;
    }

    static double weaponDamageMultiplier() {
        return 0.40;
    }

    static double toolBreakSpeedModifier() {
        return -0.85;
    }

    record Requirement(SkillId skill, int requiredLevel) {
        Requirement {
            if (requiredLevel < 1) throw new IllegalArgumentException("Requirement level must be positive");
        }
    }
}
