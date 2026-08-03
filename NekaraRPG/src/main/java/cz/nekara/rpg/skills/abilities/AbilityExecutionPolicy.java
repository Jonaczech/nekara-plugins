package cz.nekara.rpg.skills.abilities;

import java.util.Objects;

public final class AbilityExecutionPolicy {
    private final int maximumBlocksPerActivation;

    public AbilityExecutionPolicy(int maximumBlocksPerActivation) {
        if (maximumBlocksPerActivation < 1) {
            throw new IllegalArgumentException("Maximum block count must be positive");
        }
        this.maximumBlocksPerActivation = maximumBlocksPerActivation;
    }

    public AbilityDecision evaluate(AbilityExecutionContext context) {
        Objects.requireNonNull(context, "context");
        if (!context.unlocked()) {
            return deny(AbilityReason.NOT_UNLOCKED);
        }
        if (context.sourceEventCancelled()) {
            return deny(AbilityReason.CANCELLED_SOURCE_EVENT);
        }
        if (context.creative()) {
            return deny(AbilityReason.CREATIVE_MODE);
        }
        if (context.cooldownRemainingMillis() > 0) {
            return deny(AbilityReason.COOLDOWN);
        }
        if (context.protectedLocation()) {
            return deny(AbilityReason.PROTECTED_LOCATION);
        }
        if (context.requestedBlockCount() < 1) {
            return deny(AbilityReason.INVALID_BLOCK_COUNT);
        }
        if (!context.suitableTool()) {
            return deny(AbilityReason.UNSUITABLE_TOOL);
        }
        return new AbilityDecision(
            true,
            Math.min(context.requestedBlockCount(), maximumBlocksPerActivation),
            AbilityReason.ALLOWED
        );
    }

    private static AbilityDecision deny(AbilityReason reason) {
        return new AbilityDecision(false, 0, reason);
    }
}
