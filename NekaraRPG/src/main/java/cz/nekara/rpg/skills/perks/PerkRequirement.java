package cz.nekara.rpg.skills.perks;

import java.util.Objects;

public record PerkRequirement(PerkId perkId, int minimumRank) {
    public PerkRequirement {
        Objects.requireNonNull(perkId, "perkId");
        if (minimumRank < 1) {
            throw new IllegalArgumentException("Required perk rank must be positive");
        }
    }
}
