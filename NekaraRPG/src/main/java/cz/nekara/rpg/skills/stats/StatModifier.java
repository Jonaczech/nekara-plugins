package cz.nekara.rpg.skills.stats;

import java.util.Objects;

public record StatModifier(
    String sourceId,
    StatId statId,
    ModifierOperation operation,
    double amount
) {
    public StatModifier {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(statId, "statId");
        Objects.requireNonNull(operation, "operation");
        if (sourceId.isBlank()) {
            throw new IllegalArgumentException("Modifier source ID cannot be blank");
        }
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Modifier amount must be finite");
        }
        if (operation == ModifierOperation.MULTIPLY && amount < 0) {
            throw new IllegalArgumentException("Multiplication factor cannot be negative");
        }
    }
}
