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
        return damageMultiplier(materialNames, type);
    }

    static double damageMultiplier(String[] materialNames, DamageType type) {
        if (type == DamageType.BLEED || materialNames == null || materialNames.length == 0) {
            return 1.0;
        }
        double protection = 0.0;
        for (String materialName : materialNames) {
            protection += protection(materialName == null ? "AIR" : materialName, type);
        }
        // Values describe a complete four-piece set. Partial armor receives a proportional benefit.
        return 1.0 - Math.min(0.25, protection / 4.0);
    }

    private static double protection(String name, DamageType type) {
        if (name.startsWith("LEATHER_")) {
            return switch (type) {
                case SLASH -> 0.07;
                case PIERCE -> 0.05;
                case IMPACT -> 0.08;
                case BLEED -> 0.0;
            };
        }
        if (name.startsWith("CHAINMAIL_")) {
            return switch (type) {
                case SLASH -> 0.10;
                case PIERCE -> 0.10;
                case IMPACT -> 0.04;
                case BLEED -> 0.0;
            };
        }
        if (name.startsWith("IRON_")) {
            return plate(type, 0.14, 0.12, 0.04);
        }
        if (name.startsWith("DIAMOND_")) {
            return plate(type, 0.15, 0.13, 0.05);
        }
        if (name.startsWith("NETHERITE_")) {
            return plate(type, 0.16, 0.14, 0.05);
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
