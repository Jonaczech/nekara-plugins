package cz.nekara.rpg.skills.stats;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class StatEngine {
    public StatSnapshot resolve(Collection<StatModifier> modifiers) {
        Objects.requireNonNull(modifiers, "modifiers");
        EnumMap<StatId, Double> resolved = new EnumMap<>(StatId.class);
        for (StatId stat : StatId.values()) {
            resolved.put(stat, resolve(stat, modifiers));
        }
        return new StatSnapshot(resolved);
    }

    public double resolve(StatId stat, Collection<StatModifier> modifiers) {
        Objects.requireNonNull(stat, "stat");
        Objects.requireNonNull(modifiers, "modifiers");

        Map<ModifierKey, StatModifier> uniqueModifiers = new LinkedHashMap<>();
        for (StatModifier modifier : modifiers) {
            Objects.requireNonNull(modifier, "modifier");
            if (modifier.statId() == stat) {
                uniqueModifiers.put(new ModifierKey(modifier.sourceId(), modifier.operation()), modifier);
            }
        }

        double value = stat.defaultValue();
        for (StatModifier modifier : uniqueModifiers.values()) {
            if (modifier.operation() == ModifierOperation.ADD) {
                value += modifier.amount();
            }
        }
        for (StatModifier modifier : uniqueModifiers.values()) {
            if (modifier.operation() == ModifierOperation.MULTIPLY) {
                value *= modifier.amount();
            }
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Resolved stat value is not finite for " + stat);
        }
        return stat.clamp(value);
    }

    private record ModifierKey(String sourceId, ModifierOperation operation) {
    }
}
