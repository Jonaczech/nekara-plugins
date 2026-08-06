package cz.nekara.rpg.items.weapons;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.combat.DamageType;

public enum WeaponFamily {
    SWORD("Me\u010d", SkillId.LIGHT_WEAPONS, DamageType.SLASH, false, false),
    DAGGER("D\u00fdka", SkillId.LIGHT_WEAPONS, DamageType.PIERCE, true, false),
    SPEAR("Kop\u00ed", SkillId.LIGHT_WEAPONS, DamageType.PIERCE, false, true),
    AXE("Sekera", SkillId.HEAVY_WEAPONS, DamageType.SLASH, false, false),
    GREATSWORD("Obouru\u010dn\u00ed me\u010d", SkillId.HEAVY_WEAPONS, DamageType.SLASH, true, true),
    HAMMER("Kladivo", SkillId.HEAVY_WEAPONS, DamageType.IMPACT, true, true);

    private final String displayName;
    private final SkillId skill;
    private final DamageType damageType;
    private final boolean custom;
    private final boolean requiresEmptyOffhand;

    WeaponFamily(String displayName, SkillId skill, DamageType damageType, boolean custom, boolean requiresEmptyOffhand) {
        this.displayName = displayName;
        this.skill = skill;
        this.damageType = damageType;
        this.custom = custom;
        this.requiresEmptyOffhand = requiresEmptyOffhand;
    }

    public String displayName() { return displayName; }
    public SkillId skill() { return skill; }
    public DamageType damageType() { return damageType; }
    public boolean custom() { return custom; }
    public boolean requiresEmptyOffhand() { return requiresEmptyOffhand; }

    public double interactionRangeModifier() {
        return switch (this) {
            case DAGGER -> -0.5;
            case SPEAR -> 1.25;
            default -> 0.0;
        };
    }
}
