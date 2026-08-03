package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExperiencePolicyTest {
    private final ExperiencePolicy policy = ExperiencePolicy.defaultPolicy();

    @Test
    void cancelledCreativeSyntheticAndAutomatedEventsNeverGrantExperience() {
        assertDenied(context(SkillId.MINING, true, false, false, false, false, false, 0),
            ExperienceReason.CANCELLED_EVENT);
        assertDenied(context(SkillId.MINING, false, true, false, false, false, false, 0),
            ExperienceReason.UNSUPPORTED_GAME_MODE);
        assertDenied(context(SkillId.MINING, false, false, false, true, false, false, 0),
            ExperienceReason.SYNTHETIC_SOURCE);
        assertDenied(context(SkillId.MINING, false, false, false, false, false, true, 0),
            ExperienceReason.AUTOMATED_SOURCE);
    }

    @Test
    void placedOresAreRejectedButCultivatedSourcesCanProgressWithDecay() {
        assertDenied(context(SkillId.MINING, false, false, false, false, true, false, 0),
            ExperienceReason.PLAYER_PLACED_SOURCE);

        ExperienceDecision farming = policy.evaluate(
            context(SkillId.FARMING, false, false, false, false, true, false, 0));
        assertTrue(farming.allowed());
        assertEquals(ExperienceReason.NORMAL, farming.reason());
    }

    @Test
    void repeatedAwardsWithinOneChunkDecayAndEventuallyStop() {
        ExperienceDecision decayed = policy.evaluate(
            context(SkillId.WOODCUTTING, false, false, false, false, true, false, 64));
        assertTrue(decayed.allowed());
        assertEquals(ExperienceReason.FARM_DECAY, decayed.reason());
        assertTrue(decayed.multiplier() < 1);
        assertTrue(decayed.multiplier() >= 0.1);

        assertDenied(context(SkillId.WOODCUTTING, false, false, false, false, true, false, 128),
            ExperienceReason.FARM_LIMIT);
    }

    private void assertDenied(ExperienceContext context, ExperienceReason reason) {
        ExperienceDecision decision = policy.evaluate(context);
        assertFalse(decision.allowed());
        assertEquals(0, decision.multiplier());
        assertEquals(reason, decision.reason());
    }

    private static ExperienceContext context(
        SkillId skill,
        boolean cancelled,
        boolean creative,
        boolean spectator,
        boolean synthetic,
        boolean playerPlaced,
        boolean automated,
        int recentChunkAwards
    ) {
        return new ExperienceContext(
            skill,
            cancelled,
            creative,
            spectator,
            synthetic,
            playerPlaced,
            automated,
            recentChunkAwards
        );
    }
}
