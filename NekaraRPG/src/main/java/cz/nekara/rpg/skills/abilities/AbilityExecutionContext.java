package cz.nekara.rpg.skills.abilities;

import cz.nekara.rpg.skills.perks.MechanicId;
import java.util.Objects;

public record AbilityExecutionContext(
    MechanicId mechanic,
    boolean unlocked,
    boolean sourceEventCancelled,
    boolean creative,
    boolean protectedLocation,
    long cooldownRemainingMillis,
    int requestedBlockCount,
    boolean suitableTool
) {
    public AbilityExecutionContext {
        Objects.requireNonNull(mechanic, "mechanic");
        if (cooldownRemainingMillis < 0) {
            throw new IllegalArgumentException("Cooldown cannot be negative");
        }
    }
}
