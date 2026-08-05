package cz.nekara.rpg.items.weapons;

import org.bukkit.Material;

public record WeaponDefinition(WeaponFamily family, WeaponTier tier, Material material) {
    public String id() {
        return family.name().toLowerCase(java.util.Locale.ROOT) + "/" + tier.id();
    }

    public String modelKey() {
        return "weapons/" + id();
    }

    public boolean custom() {
        return family.custom();
    }
}
