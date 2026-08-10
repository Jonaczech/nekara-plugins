package cz.nekara.rpg.items.armor;

import org.bukkit.Material;

enum ChainmailUpgrade {
    HELMET("helmet", Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET),
    CHESTPLATE("chestplate", Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE),
    LEGGINGS("leggings", Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS),
    BOOTS("boots", Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS);

    private final String id;
    private final Material leatherBase;
    private final Material chainmailResult;

    ChainmailUpgrade(String id, Material leatherBase, Material chainmailResult) {
        this.id = id;
        this.leatherBase = leatherBase;
        this.chainmailResult = chainmailResult;
    }

    String id() {
        return id;
    }

    Material leatherBase() {
        return leatherBase;
    }

    Material chainmailResult() {
        return chainmailResult;
    }
}
