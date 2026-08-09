package cz.nekara.rpg.skills.combat;

import org.bukkit.inventory.ItemStack;

/** Small type-specific reduction layered on top of Minecraft's native armor calculation. */
public final class ArmorProtectionResolver {
    private ArmorProtectionResolver() {
    }

    public static double damageMultiplier(ItemStack[] armor, DamageType type) {
        if (type == DamageType.BLEED || armor == null || armor.length == 0) {
            return 1.0;
        }
        String[] materialNames = new String[armor.length];
        for (int index = 0; index < armor.length; index++) {
            materialNames[index] = armor[index] == null ? "AIR" : armor[index].getType().name();
        }
        return damageMultiplier(materialNames, type, 1.0, 0.0);
    }

    static double damageMultiplier(String[] materialNames, DamageType type) {
        return damageMultiplier(materialNames, type, 1.0, 0.0);
    }

    /**
     * Resolves Nekara's type-specific protection after vanilla armor, toughness and enchantments.
     * Penetration only bypasses this additional layer, never adds damage to an unarmored target.
     */
    public static double damageMultiplier(
        ItemStack[] armor, DamageType type, double armorEffectiveness, double armorPenetration
    ) {
        if (armor == null || armor.length == 0) {
            return 1.0;
        }
        String[] materialNames = new String[armor.length];
        for (int index = 0; index < armor.length; index++) {
            materialNames[index] = armor[index] == null ? "AIR" : armor[index].getType().name();
        }
        return damageMultiplier(materialNames, type, armorEffectiveness, armorPenetration);
    }

    static double damageMultiplier(
        String[] materialNames, DamageType type, double armorEffectiveness, double armorPenetration
    ) {
        if (type == DamageType.BLEED || materialNames == null || materialNames.length == 0) {
            return 1.0;
        }
        double protection = 0.0;
        for (String materialName : materialNames) {
            protection += protection(materialName == null ? "AIR" : materialName, type);
        }
        // Values describe a complete four-piece set. Partial armor receives a proportional benefit.
        double scaledProtection = protection / 4.0 * Math.max(0.0, armorEffectiveness);
        double effectivePenetration = Math.clamp(armorPenetration, 0.0, 0.35);
        return 1.0 - Math.min(0.25, scaledProtection * (1.0 - effectivePenetration));
    }

    private static double protection(String name, DamageType type) {
        if (name.startsWith("LEATHER_")) {
            return switch (type) {
                case SLASH -> 0.04;
                case PIERCE -> 0.03;
                case IMPACT -> 0.05;
                case BLEED -> 0.0;
            };
        }
        if (name.startsWith("CHAINMAIL_")) {
            return switch (type) {
                case SLASH -> 0.06;
                case PIERCE -> 0.06;
                case IMPACT -> 0.03;
                case BLEED -> 0.0;
            };
        }
        if (name.startsWith("COPPER_")) {
            return plate(type, 0.07, 0.06, 0.03);
        }
        if (name.startsWith("GOLDEN_")) {
            return plate(type, 0.05, 0.04, 0.03);
        }
        if (name.startsWith("IRON_")) {
            return plate(type, 0.09, 0.07, 0.03);
        }
        if (name.startsWith("DIAMOND_")) {
            return plate(type, 0.10, 0.08, 0.04);
        }
        if (name.startsWith("NETHERITE_")) {
            return plate(type, 0.11, 0.09, 0.04);
        }
        return 0.0;
    }

    private static double plate(DamageType type, double slash, double pierce, double impact) {
        return switch (type) {
            case SLASH -> slash;
            case PIERCE -> pierce;
            case IMPACT -> impact;
            case BLEED -> 0.0;
        };
    }
}
