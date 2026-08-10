package cz.nekara.rpg.skills.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LightArmorTreeTest {
    private static final double EPSILON = 0.000_001;

    @Test
    void providesTheShadowArmorSetBonuses() {
        DefaultPerkTree tree = DefaultPerkTree.create();
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(
                new PerkId("light_armor.armor"), 5,
                new PerkId("light_armor.mobility"), 1,
                new PerkId("light_armor.dodge"), 1,
                new PerkId("light_armor.sustenance"), 1,
                new PerkId("light_armor.adrenaline"), 1,
                new PerkId("light_armor.master"), 1
            ), 14, 1);

        var stats = new PerkStatResolver(tree.catalog()).resolve(profile, SkillId.LIGHT_ARMOR);

        assertEquals(1.30, stats.value(StatId.ARMOR_MULTIPLIER), EPSILON);
        assertEquals(0.20, stats.value(StatId.DODGE_CHANCE), EPSILON);
        assertEquals(0.30, stats.value(StatId.HUNGER_CONSUMPTION_REDUCTION), EPSILON);
        assertEquals(1.0, stats.value(StatId.MOVEMENT_PENALTY_REDUCTION), EPSILON);
        assertEquals(0.05, stats.value(StatId.LIGHT_ARMOR_MOVEMENT_SPEED), EPSILON);
        assertTrue(tree.catalog().require(new PerkId("light_armor.sustenance")).effects().stream()
            .anyMatch(effect -> effect instanceof MechanicPerkEffect mechanic
                && mechanic.mechanicId() == MechanicId.LIGHT_ARMOR_THREE_PIECE_SET_BONUS));
    }
}
