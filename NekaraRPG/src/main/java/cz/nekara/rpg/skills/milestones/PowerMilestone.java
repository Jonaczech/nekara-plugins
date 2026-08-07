package cz.nekara.rpg.skills.milestones;

import java.util.Objects;
import java.util.regex.Pattern;

public record PowerMilestone(String id, int requiredPowerLevel) {
    private static final Pattern VALID_ID = Pattern.compile("[a-z][a-z0-9_]*");

    public PowerMilestone {
        Objects.requireNonNull(id, "id");
        if (!VALID_ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Milestone ID must use lowercase snake_case");
        }
        if (requiredPowerLevel < 1 || requiredPowerLevel > 200) {
            throw new IllegalArgumentException("Milestone power level must be between 1 and 200");
        }
    }
}
