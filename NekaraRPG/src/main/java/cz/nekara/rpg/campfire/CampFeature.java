package cz.nekara.rpg.campfire;

import org.bukkit.Material;
import org.bukkit.Tag;

public enum CampFeature {
    CRAFTING_TABLE,
    BED,
    SMOKER,
    BARREL,
    WATER_CAULDRON,
    CARTOGRAPHY_TABLE,
    GRINDSTONE;

    public boolean matches(Material material) {
        return switch (this) {
            case CRAFTING_TABLE -> material == Material.CRAFTING_TABLE;
            case BED -> Tag.BEDS.isTagged(material);
            case SMOKER -> material == Material.SMOKER;
            case BARREL -> material == Material.BARREL;
            case WATER_CAULDRON -> material == Material.WATER_CAULDRON;
            case CARTOGRAPHY_TABLE -> material == Material.CARTOGRAPHY_TABLE;
            case GRINDSTONE -> material == Material.GRINDSTONE;
        };
    }
}
