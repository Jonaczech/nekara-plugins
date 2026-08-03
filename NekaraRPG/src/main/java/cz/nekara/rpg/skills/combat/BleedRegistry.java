package cz.nekara.rpg.skills.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class BleedRegistry {
    private final int maximumEntries;
    private final Map<UUID, BleedState> active = new HashMap<>();

    public BleedRegistry(int maximumEntries) {
        if (maximumEntries < 1) {
            throw new IllegalArgumentException("Maximum bleed entries must be positive");
        }
        this.maximumEntries = maximumEntries;
    }

    public boolean apply(UUID targetId, UUID sourceId, double damagePerTick, int ticks) {
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(sourceId, "sourceId");
        if (!Double.isFinite(damagePerTick) || damagePerTick <= 0.0 || ticks < 1) {
            throw new IllegalArgumentException("Bleed damage and duration must be positive");
        }
        BleedState current = active.get(targetId);
        if (current == null && active.size() >= maximumEntries) {
            return false;
        }
        if (current == null || damagePerTick >= current.damagePerTick()) {
            active.put(targetId, new BleedState(sourceId, damagePerTick, ticks));
        } else {
            active.put(targetId, new BleedState(
                current.sourceId(), current.damagePerTick(), Math.max(current.ticks(), ticks)));
        }
        return true;
    }

    public List<BleedTick> advance() {
        List<BleedTick> ticks = new ArrayList<>(active.size());
        var iterator = active.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, BleedState> entry = iterator.next();
            BleedState state = entry.getValue();
            ticks.add(new BleedTick(entry.getKey(), state.sourceId(), state.damagePerTick()));
            if (state.ticks() <= 1) {
                iterator.remove();
            } else {
                entry.setValue(new BleedState(
                    state.sourceId(), state.damagePerTick(), state.ticks() - 1));
            }
        }
        return List.copyOf(ticks);
    }

    public void remove(UUID targetId) {
        active.remove(targetId);
    }

    public void clear() {
        active.clear();
    }

    public int size() {
        return active.size();
    }

    private record BleedState(UUID sourceId, double damagePerTick, int ticks) {
    }

    public record BleedTick(UUID targetId, UUID sourceId, double damage) {
    }
}
