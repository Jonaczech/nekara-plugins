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
            "player-1", Map.of(), Map.of(new PerkId("mining.yield"), 3), 3, 1);

        StatSnapshot stats = resolver.resolve(profile, SkillId.MINING);

        assertEquals(0.075, stats.value(StatId.DOUBLE_DROP_CHANCE), 0.000_001);
        assertEquals(0.0, stats.value(StatId.TRIPLE_DROP_CHANCE), 0.000_001);
    }

    @Test
    void ignoresPerksOwnedByAnotherSkill() {
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(new PerkId("woodcutting.yield"), 2), 2, 1);

        assertEquals(0.0, resolver.resolve(profile, SkillId.MINING)
            .value(StatId.DOUBLE_DROP_CHANCE), 0.000_001);
    }

    @Test
    void rejectsCorruptRanksInsteadOfAmplifyingThem() {
        SkillProfile profile = new SkillProfile(
            "player-1", Map.of(), Map.of(new PerkId("mining.yield"), 6), 6, 1);

        assertThrows(IllegalStateException.class,
            () -> resolver.resolve(profile, SkillId.MINING));
    }
}
