package cz.nekara.rpg.skills.abilities;

import java.util.Objects;

public record AbilityDecision(boolean allowed, int permittedBlockCount, AbilityReason reason) {
    public AbilityDecision {
        Objects.requireNonNull(reason, "reason");
        if (permittedBlockCount < 0) {
            throw new IllegalArgumentException("Permitted block count cannot be negative");
        }
        if (!allowed && permittedBlockCount != 0) {
            throw new IllegalArgumentException("Denied abilities cannot process blocks");
        }
        if (allowed && permittedBlockCount < 1) {
            throw new IllegalArgumentException("Allowed abilities must process at least one block");
        }
    }
}
