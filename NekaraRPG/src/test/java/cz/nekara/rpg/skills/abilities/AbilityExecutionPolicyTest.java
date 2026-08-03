package cz.nekara.rpg.skills.abilities;

import cz.nekara.rpg.skills.perks.MechanicId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityExecutionPolicyTest {
    private final AbilityExecutionPolicy policy = new AbilityExecutionPolicy(64);

    @Test
    void safeActivationIsCappedToConfiguredBlockBudget() {
        AbilityDecision decision = policy.evaluate(context(true, false, false, false, 0, 200, true));

        assertTrue(decision.allowed());
        assertEquals(64, decision.permittedBlockCount());
        assertEquals(AbilityReason.ALLOWED, decision.reason());
    }

    @Test
    void protectionAndCancelledEventsAlwaysWin() {
        AbilityDecision cancelled = policy.evaluate(context(true, true, false, false, 0, 10, true));
        assertFalse(cancelled.allowed());
        assertEquals(AbilityReason.CANCELLED_SOURCE_EVENT, cancelled.reason());

        AbilityDecision protectedLocation = policy.evaluate(context(true, false, false, true, 0, 10, true));
        assertFalse(protectedLocation.allowed());
        assertEquals(AbilityReason.PROTECTED_LOCATION, protectedLocation.reason());
    }

    @Test
    void cooldownUnlockAndToolRequirementsAreEnforced() {
        assertEquals(AbilityReason.NOT_UNLOCKED,
            policy.evaluate(context(false, false, false, false, 0, 10, true)).reason());
        assertEquals(AbilityReason.COOLDOWN,
            policy.evaluate(context(true, false, false, false, 1, 10, true)).reason());
        assertEquals(AbilityReason.UNSUITABLE_TOOL,
            policy.evaluate(context(true, false, false, false, 0, 10, false)).reason());
    }

    private static AbilityExecutionContext context(
        boolean unlocked,
        boolean cancelled,
        boolean creative,
        boolean protectedLocation,
        long cooldown,
        int blocks,
        boolean suitableTool
    ) {
        return new AbilityExecutionContext(
            MechanicId.VEIN_MINING,
            unlocked,
            cancelled,
            creative,
            protectedLocation,
            cooldown,
            blocks,
            suitableTool
        );
    }
}
