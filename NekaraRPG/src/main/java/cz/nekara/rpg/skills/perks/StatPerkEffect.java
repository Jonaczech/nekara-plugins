package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.Objects;

public record StatPerkEffect(
    StatId statId,
    ModifierOperation operation,
    double amountPerRank
) implements PerkEffectDefinition {
    public StatPerkEffect {
        Objects.requireNonNull(statId, "statId");
        Objects.requireNonNull(operation, "operation");
        if (!Double.isFinite(amountPerRank)) {
            throw new IllegalArgumentException("Per-rank amount must be finite");
        }
        if (operation == ModifierOperation.MULTIPLY && amountPerRank < 0) {
            throw new IllegalArgumentException("Per-rank multiplication factor cannot be negative");
        }
    }
}
