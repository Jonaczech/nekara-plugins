package cz.nekara.rpg.skills.combat;

import cz.nekara.rpg.items.weapons.WeaponCatalog;
import java.util.Optional;
import org.bukkit.inventory.ItemStack;

/** Resolves a physical category from the weapon responsible for a hit. */
public final class DamageTypeResolver {
    private DamageTypeResolver() {
    }

    public static Optional<DamageType> resolve(ItemStack weapon, boolean projectile) {
        if (projectile) {
            return Optional.of(DamageType.PIERCE);
        }
        return WeaponCatalog.resolve(weapon).map(definition -> definition.family().damageType());
    }
}
