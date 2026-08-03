package cz.nekara.rpg.skills.stats;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class StatSnapshot {
    private final Map<StatId, Double> values;

    public StatSnapshot(Map<StatId, Double> values) {
        Objects.requireNonNull(values, "values");
        EnumMap<StatId, Double> normalized = new EnumMap<>(StatId.class);
        for (StatId stat : StatId.values()) {
            Double value = values.get(stat);
            if (value == null || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Missing or invalid value for " + stat);
            }
            normalized.put(stat, stat.clamp(value));
        }
        this.values = Map.copyOf(normalized);
    }

    public double value(StatId statId) {
        return values.get(Objects.requireNonNull(statId, "statId"));
    }

    public Map<StatId, Double> asMap() {
        return values;
    }
}
