package cz.nekara.rpg.skills.combat;

/** Physical damage categories used by Nekara weapons and armor. */
public enum DamageType {
    SLASH("Sečné"),
    PIERCE("Bodné"),
    IMPACT("Úderné"),
    BLEED("Krvácení");

    private final String czechName;

    DamageType(String czechName) {
        this.czechName = czechName;
    }

    public String czechName() {
        return czechName;
    }
}
