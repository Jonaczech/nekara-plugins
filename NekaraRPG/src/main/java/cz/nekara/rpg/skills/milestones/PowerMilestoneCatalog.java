package cz.nekara.rpg.skills.milestones;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PowerMilestoneCatalog {
    private final List<PowerMilestone> milestones;

    public PowerMilestoneCatalog(Collection<PowerMilestone> milestones) {
        Objects.requireNonNull(milestones, "milestones");
        Set<String> ids = new HashSet<>();
        List<PowerMilestone> normalized = new ArrayList<>();
        for (PowerMilestone milestone : milestones) {
            Objects.requireNonNull(milestone, "milestone");
            if (!ids.add(milestone.id())) {
                throw new IllegalArgumentException("Duplicate milestone ID: " + milestone.id());
            }
            normalized.add(milestone);
        }
        normalized.sort(Comparator.comparingInt(PowerMilestone::requiredPowerLevel));
        this.milestones = List.copyOf(normalized);
    }

    public List<PowerMilestone> unlockedAt(int powerLevel) {
        if (powerLevel < 0) {
            throw new IllegalArgumentException("Power level cannot be negative");
        }
        return milestones.stream()
            .filter(milestone -> milestone.requiredPowerLevel() <= powerLevel)
            .toList();
    }
}
