package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.items.weapons.WeaponDefinition;
import cz.nekara.rpg.items.weapons.WeaponFamily;
import cz.nekara.rpg.items.weapons.WeaponTier;
import org.bukkit.Material;

/** Material-gated mobility rules; copper deliberately remains an early-game tier. */
final class EquipmentMobilityPolicy {
    private EquipmentMobilityPolicy() {
    }

    static double armorPenalty(
        Material[] armor,
        boolean chainmailTrained,
        boolean diamondTrained,
        boolean ironTrained,
        boolean netheriteTrained
    ) {
        double penalty = 0.0;
        for (Material material : armor) {
            if (material == null) {
                continue;
            }
            String name = material.name();
            if (name.startsWith("CHAINMAIL_") && !chainmailTrained) penalty -= 0.0125;
            else if (name.startsWith("DIAMOND_") && !diamondTrained) penalty -= 0.0175;
            else if ((name.startsWith("IRON_") || name.startsWith("GOLDEN_")) && !ironTrained) penalty -= 0.03;
            else if (name.startsWith("NETHERITE_") && !netheriteTrained) penalty -= 0.0375;
        }
        return penalty;
    }

    static double weaponPenalty(
        WeaponDefinition weapon,
        boolean ironTrained,
        boolean diamondTrained,
        boolean netheriteTrained
    ) {
        WeaponTier tier = weapon.tier();
        if (tier == WeaponTier.WOODEN || tier == WeaponTier.STONE || tier == WeaponTier.COPPER) {
            return 0.0;
        }
        boolean cleared = switch (tier) {
            case IRON, GOLDEN -> ironTrained;
            case DIAMOND -> diamondTrained;
            case NETHERITE -> netheriteTrained;
            default -> true;
        };
        if (cleared) {
            return 0.0;
        }
        return weapon.family() == WeaponFamily.AXE || weapon.family() == WeaponFamily.GREATSWORD
            || weapon.family() == WeaponFamily.HAMMER ? switch (tier) {
                case IRON, GOLDEN -> -0.04;
                case DIAMOND -> -0.055;
                case NETHERITE -> -0.07;
                default -> 0.0;
            } : switch (tier) {
                case IRON, GOLDEN -> -0.02;
                case DIAMOND -> -0.03;
                case NETHERITE -> -0.04;
                default -> 0.0;
            };
    }
}
