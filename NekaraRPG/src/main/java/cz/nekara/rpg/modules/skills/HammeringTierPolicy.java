package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.items.weapons.WeaponTier;
import java.util.Optional;
import org.bukkit.Material;

/** Pure material progression rules for hammering heated smithing products. */
final class HammeringTierPolicy {
    private HammeringTierPolicy() {
    }

    static Optional<WeaponTier> requiredTier(Material material) {
        String name = material.name();
        if (name.startsWith("COPPER_")) return Optional.of(WeaponTier.COPPER);
        if (name.startsWith("GOLDEN_")) return Optional.of(WeaponTier.GOLDEN);
        if (name.startsWith("IRON_") || name.startsWith("CHAINMAIL_")) return Optional.of(WeaponTier.IRON);
        if (name.startsWith("DIAMOND_")) return Optional.of(WeaponTier.DIAMOND);
        if (name.startsWith("NETHERITE_")) return Optional.of(WeaponTier.NETHERITE);
        return Optional.empty();
    }

    static boolean isSufficient(WeaponTier hammer, WeaponTier workpiece) {
        return progressionRank(hammer) >= progressionRank(workpiece);
    }

    private static int progressionRank(WeaponTier tier) {
        return switch (tier) {
            case WOODEN -> 0;
            case STONE -> 1;
            case COPPER -> 2;
            case GOLDEN -> 3;
            case IRON -> 4;
            case DIAMOND -> 5;
            case NETHERITE -> 6;
        };
    }
}
