package cz.nekara.rpg.skills;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SkillPresentation {
    private static final Map<SkillId, String> CZECH_NAMES;

    static {
        EnumMap<SkillId, String> names = new EnumMap<>(SkillId.class);
        names.put(SkillId.POWER, "Hlavní úroveň");
        names.put(SkillId.SMITHING, "Řemeslo");
        names.put(SkillId.ENCHANTING, "Runotepectví");
        names.put(SkillId.ALCHEMY, "Alchymie");
        names.put(SkillId.MINING, "Hornictví");
        names.put(SkillId.WOODCUTTING, "Rubačina");
        names.put(SkillId.DIGGING, "Zeměrytectví");
        names.put(SkillId.FARMING, "Hospodářství");
        names.put(SkillId.LIGHT_WEAPONS, "Sekání a bodání");
        names.put(SkillId.HEAVY_WEAPONS, "Brutální boj");
        names.put(SkillId.MARTIAL_ARTS, "Umění dlaně");
        names.put(SkillId.TRADING, "Obchodování");
        names.put(SkillId.FISHING, "Udičkářství");
        names.put(SkillId.ARCHERY, "Umění střelby");
        names.put(SkillId.LIGHT_ARMOR, "Stínový oděv");
        names.put(SkillId.HEAVY_ARMOR, "Plátová ochrana");
        CZECH_NAMES = Map.copyOf(names);
    }

    private SkillPresentation() {
    }

    public static String czechName(SkillId skill) {
        String name = CZECH_NAMES.get(Objects.requireNonNull(skill, "skill"));
        if (name == null) {
            throw new IllegalArgumentException("Missing Czech skill name: " + skill);
        }
        return name;
    }
}
