package cz.nekara.rpg.skills.perks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HeavyWeaponTreeTest {
    private static final double EPSILON = 0.000_001;

    @Test
    void keepsStableIdsAndProvidesTheBrutalCombatStats() {
        DefaultPerkTree tree = DefaultPerkTree.create();
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(
                new PerkId("heavy_weapons.damage"), 5,
                new PerkId("heavy_weapons.power"), 5,
                new PerkId("heavy_weapons.critical"), 1,
                new PerkId("heavy_weapons.penetration"), 1,
                new PerkId("heavy_weapons.coating"), 1,
                new PerkId("heavy_weapons.master"), 1
            ), 14, 1);

        var stats = new PerkStatResolver(tree.catalog()).resolve(profile, SkillId.HEAVY_WEAPONS);

        assertEquals(1.10, stats.value(StatId.DAMAGE_MULTIPLIER), EPSILON);
        assertEquals(1.40, stats.value(StatId.POWER_ATTACK_DAMAGE_MULTIPLIER), EPSILON);
        assertEquals(0.07, stats.value(StatId.CRITICAL_CHANCE), EPSILON);
        assertEquals(0.30, stats.value(StatId.ARMOR_PENETRATION), EPSILON);
        assertEquals(0.12, stats.value(StatId.STUN_CHANCE), EPSILON);
        assertTrue(tree.catalog().require(new PerkId("heavy_weapons.penetration")).effects().stream()
            .anyMatch(effect -> effect instanceof MechanicPerkEffect mechanic
                && mechanic.mechanicId() == MechanicId.HEAVY_POWER_SWEEP));
    }

    @Test
    void usesTheAsymmetricAxeLayout() {
        PerkTreeLayout layout = PerkTreeLayout.forSkill(SkillId.HEAVY_WEAPONS);

        assertEquals(new PerkPosition(4, 9), layout.root());
        assertEquals(new PerkPosition(2, 7), layout.left());
        assertEquals(new PerkPosition(6, 7), layout.right());
        assertEquals(new PerkPosition(1, 5), layout.leftDeep());
        assertEquals(new PerkPosition(5, 5), layout.rightDeep());
        assertEquals(new PerkPosition(3, 3), layout.crown());
    }
}
