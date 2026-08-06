package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightWeaponTreeTest {
    private static final double EPSILON = 0.000_001;

    @Test
    void keepsStableIdsWhileProvidingTheRedesignedCombatStats() {
        DefaultPerkTree tree = DefaultPerkTree.create();
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(
                new PerkId("lehke_zbrane.damage"), 5,
                new PerkId("lehke_zbrane.critical"), 5,
                new PerkId("lehke_zbrane.parry"), 1,
                new PerkId("lehke_zbrane.coating"), 1,
                new PerkId("lehke_zbrane.immunity"), 1,
                new PerkId("lehke_zbrane.master"), 1
            ), 14, 1);

        var stats = new PerkStatResolver(tree.catalog()).resolve(profile, SkillId.LIGHT_WEAPONS);

        assertEquals(1.10, stats.value(StatId.DAMAGE_MULTIPLIER), EPSILON);
        assertEquals(0.14, stats.value(StatId.LIGHT_WEAPON_ATTACK_SPEED), EPSILON);
        assertEquals(0.11, stats.value(StatId.CRITICAL_CHANCE), EPSILON);
        assertEquals(1.75, stats.value(StatId.CRITICAL_DAMAGE_MULTIPLIER), EPSILON);
        assertEquals(0.07, stats.value(StatId.BLEED_CHANCE), EPSILON);
        assertEquals(1.25, stats.value(StatId.BLEED_DAMAGE_MULTIPLIER), EPSILON);
        assertEquals(0.70, stats.value(StatId.CRITICAL_BLEED_CHANCE), EPSILON);
        assertEquals(1.0, stats.value(StatId.BLEED_FLAT_DAMAGE), EPSILON);
        assertTrue(tree.catalog().require(new PerkId("lehke_zbrane.critical")).effects().stream()
            .anyMatch(effect -> effect instanceof MechanicPerkEffect mechanic
                && mechanic.mechanicId() == MechanicId.LIGHT_WEAPON_IRON_MOBILITY));
        assertTrue(tree.catalog().require(new PerkId("lehke_zbrane.parry")).effects().stream()
            .anyMatch(effect -> effect instanceof MechanicPerkEffect mechanic
                && mechanic.mechanicId() == MechanicId.LIGHT_WEAPON_DIAMOND_MOBILITY));
        assertTrue(tree.catalog().require(new PerkId("lehke_zbrane.immunity")).effects().stream()
            .anyMatch(effect -> effect instanceof MechanicPerkEffect mechanic
                && mechanic.mechanicId() == MechanicId.LIGHT_WEAPON_NETHERITE_MOBILITY));
        assertTrue(tree.catalog().require(new PerkId("alchemy.merging")).effects().stream()
            .anyMatch(effect -> effect instanceof MechanicPerkEffect mechanic
                && mechanic.mechanicId() == MechanicId.WEAPON_COATING));
    }

    @Test
    void usesDedicatedWeaponRoutingWithoutChangingOtherTrees() {
        assertEquals(PerkConnectionPath.BendOrder.VERTICAL_FIRST,
            PerkTreeLayout.connectionBendOrder(SkillId.LIGHT_WEAPONS, 1));
        assertEquals(PerkConnectionPath.BendOrder.HORIZONTAL_FIRST,
            PerkTreeLayout.connectionBendOrder(SkillId.LIGHT_WEAPONS, 2));
        assertEquals(PerkConnectionPath.BendOrder.VERTICAL_FIRST,
            PerkTreeLayout.connectionBendOrder(SkillId.HEAVY_WEAPONS, 1));
        assertEquals(PerkConnectionPath.BendOrder.VERTICAL_FIRST,
            PerkTreeLayout.connectionBendOrder(SkillId.HEAVY_WEAPONS, 2));
        assertEquals(PerkConnectionPath.BendOrder.HORIZONTAL_FIRST,
            PerkTreeLayout.connectionBendOrder(SkillId.ARCHERY, 1));
    }
}
