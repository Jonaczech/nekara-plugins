package cz.nekara.rpg.skills.stats;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PerkStatResolverTest {
    private final DefaultPerkTree tree = DefaultPerkTree.create();
    private final PerkStatResolver resolver = new PerkStatResolver(tree.catalog());

    @Test
    void scalesPurchasedMiningYieldByStoredRank() {
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(new PerkId("tezba.yield"), 3), 3, 1);

        StatSnapshot stats = resolver.resolve(profile, SkillId.MINING);

        assertEquals(1.06, stats.value(StatId.MINING_SPEED), 0.000_001);
        assertEquals(0.12, stats.value(StatId.MINING_BLOCK_EXPERIENCE), 0.000_001);
    }

    @Test
    void ignoresPerksOwnedByAnotherSkill() {
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(new PerkId("lesnictvi.yield"), 2), 2, 1);

        assertEquals(0.0, resolver.resolve(profile, SkillId.MINING)
            .value(StatId.DOUBLE_DROP_CHANCE), 0.000_001);
    }

    @Test
    void rejectsCorruptRanksInsteadOfAmplifyingThem() {
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(new PerkId("tezba.yield"), 6), 6, 1);

        assertThrows(IllegalStateException.class,
            () -> resolver.resolve(profile, SkillId.MINING));
    }

    @Test
    void newGamePlusStrengthensFarmingBonusesWithoutChangingTheirCaps() {
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(
                new PerkId("statkarstvi.yield"), 5,
                new PerkId("statkarstvi.husbandry"), 1
            ), 7, 1);

        StatSnapshot stats = resolver.resolve(profile, SkillId.FARMING, 1.25);

        assertEquals(0.25, stats.value(StatId.FARMING_BONUS_DROP_CHANCE), 0.000_001);
        assertEquals(1.75, stats.value(StatId.ANIMAL_GROWTH_MULTIPLIER), 0.000_001);
        assertEquals(0.375, stats.value(StatId.ANIMAL_BONUS_DROP_CHANCE), 0.000_001);
        assertEquals(4.75, stats.value(StatId.ANIMAL_DAMAGE_MULTIPLIER), 0.000_001);
    }

    @Test
    void newGamePlusStrengthensWoodcuttingAndGoldenLeafBonuses() {
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(
                new PerkId("lesnictvi.yield"), 5,
                new PerkId("lesnictvi.tempo"), 5,
                new PerkId("lesnictvi.leaves"), 1,
                new PerkId("lesnictvi.triple"), 1
            ), 12, 1);

        StatSnapshot stats = resolver.resolve(profile, SkillId.WOODCUTTING, 1.25);

        assertEquals(1.375, stats.value(StatId.WOODCUTTING_SPEED), 0.000_001);
        assertEquals(0.3125, stats.value(StatId.WOODCUTTING_LOG_EXPERIENCE), 0.000_001);
        assertEquals(2.25, stats.value(StatId.SAPLING_GROWTH_MULTIPLIER), 0.000_001);
        assertEquals(0.00625, stats.value(StatId.GOLDEN_LEAF_APPLE_CHANCE), 0.000_001);
    }
}
