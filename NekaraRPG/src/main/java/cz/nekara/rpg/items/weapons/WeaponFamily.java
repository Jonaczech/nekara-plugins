package cz.nekara.rpg.items.weapons;

import cz.nekara.rpg.skills.SkillId;

public enum WeaponFamily {
    SWORD("Me\u010d", SkillId.LIGHT_WEAPONS, false, false),
    DAGGER("D\u00fdka", SkillId.LIGHT_WEAPONS, true, false),
    SPEAR("Kop\u00ed", SkillId.LIGHT_WEAPONS, false, true),
    AXE("Sekera", SkillId.HEAVY_WEAPONS, false, false),
    GREATSWORD("Obouru\u010dn\u00ed me\u010d", SkillId.HEAVY_WEAPONS, true, true),
    HAMMER("Kladivo", SkillId.HEAVY_WEAPONS, true, true);

    private final String displayName;
    private final SkillId skill;
    private final boolean custom;
    private final boolean requiresEmptyOffhand;

    WeaponFamily(String displayName, SkillId skill, boolean custom, boolean requiresEmptyOffhand) {
        this.displayName = displayName;
        this.skill = skill;
        this.custom = custom;
        this.requiresEmptyOffhand = requiresEmptyOffhand;
    }

    public String displayName() { return displayName; }
    public SkillId skill() { return skill; }
    public boolean custom() { return custom; }
    public boolean requiresEmptyOffhand() { return requiresEmptyOffhand; }
}
