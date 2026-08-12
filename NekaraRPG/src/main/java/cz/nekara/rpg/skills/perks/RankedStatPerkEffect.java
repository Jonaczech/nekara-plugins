package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.List;
import java.util.Objects;

/** Defines an explicit cumulative stat value for every perk rank. */
public record RankedStatPerkEffect(
    StatId statId,
    ModifierOperation operation,
    List<Double> amountsByRank
) implements PerkEffectDefinition {
    public RankedStatPerkEffect {
        Objects.requireNonNull(statId, "statId");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(amountsByRank, "amountsByRank");
        if (amountsByRank.isEmpty() || amountsByRank.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalArgumentException("Ranked stat values must be finite and non-empty");
        }
        if (operation == ModifierOperation.MULTIPLY && amountsByRank.stream().anyMatch(value -> value < 0.0)) {
            throw new IllegalArgumentException("Ranked multiplication values cannot be negative");
        }
        amountsByRank = List.copyOf(amountsByRank);
    }

    public double amountForRank(int rank) {
        if (rank < 1 || rank > amountsByRank.size()) {
            throw new IllegalArgumentException("Rank must be between 1 and " + amountsByRank.size());
        }
        return amountsByRank.get(rank - 1);
    }
}