package cz.nekara.rpg.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillPresentationTest {
    @Test
    void usesApprovedCzechSkillNames() {
        assertEquals("Hlavní úroveň", SkillPresentation.czechName(SkillId.POWER));
        assertEquals("Řemeslo", SkillPresentation.czechName(SkillId.SMITHING));
        assertEquals("Runotepectví", SkillPresentation.czechName(SkillId.ENCHANTING));
        assertEquals("Hornictví", SkillPresentation.czechName(SkillId.MINING));
        assertEquals("Rubačina", SkillPresentation.czechName(SkillId.WOODCUTTING));
        assertEquals("Zeměrytectví", SkillPresentation.czechName(SkillId.DIGGING));
        assertEquals("Sedlačení", SkillPresentation.czechName(SkillId.FARMING));
        assertEquals("Sekání a bodání", SkillPresentation.czechName(SkillId.LIGHT_WEAPONS));
        assertEquals("Brutální boj", SkillPresentation.czechName(SkillId.HEAVY_WEAPONS));
        assertEquals("Umění dlaně", SkillPresentation.czechName(SkillId.MARTIAL_ARTS));
        assertEquals("Obchodování", SkillPresentation.czechName(SkillId.TRADING));
        assertEquals("Udičkářství", SkillPresentation.czechName(SkillId.FISHING));
        assertEquals("Umění střelby", SkillPresentation.czechName(SkillId.ARCHERY));
        assertEquals("Stínový oděv", SkillPresentation.czechName(SkillId.LIGHT_ARMOR));
        assertEquals("Plátová ochrana", SkillPresentation.czechName(SkillId.HEAVY_ARMOR));
    }
}
