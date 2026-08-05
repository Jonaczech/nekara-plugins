package cz.nekara.rpg.items.weapons;

import org.bukkit.Material;

public enum WeaponTier {
    WOODEN("wooden", "D\u0159ev\u011bn\u00fd", Material.OAK_PLANKS),
    STONE("stone", "Kamenn\u00fd", Material.COBBLESTONE),
    COPPER("copper", "M\u011bd\u011bn\u00fd", Material.COPPER_INGOT),
    IRON("iron", "\u017delezn\u00fd", Material.IRON_INGOT),
    GOLDEN("golden", "Zlat\u00fd", Material.GOLD_INGOT),
    DIAMOND("diamond", "Diamantov\u00fd", Material.DIAMOND),
    NETHERITE("netherite", "Netheritov\u00fd", Material.NETHERITE_INGOT);

    private final String id;
    private final String displayPrefix;
    private final Material craftingIngredient;

    WeaponTier(String id, String displayPrefix, Material craftingIngredient) {
        this.id = id;
        this.displayPrefix = displayPrefix;
        this.craftingIngredient = craftingIngredient;
    }

    public String id() { return id; }
    public String displayPrefix() { return displayPrefix; }
    public Material craftingIngredient() { return craftingIngredient; }

    public Material vanillaMaterial(String suffix) {
        return Material.valueOf(name() + "_" + suffix);
    }
}
