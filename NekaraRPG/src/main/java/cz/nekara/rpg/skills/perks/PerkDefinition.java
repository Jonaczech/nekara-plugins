package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PerkDefinition(
    PerkId id,
    SkillId skill,
    int maxRank,
    int pointCostPerRank,
    int requiredSkillLevel,
    Set<PerkRequirement> requirements,
    List<PerkEffectDefinition> effects,
    PerkPosition position
) {
    public PerkDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(requirements, "requirements");
        Objects.requireNonNull(effects, "effects");
        Objects.requireNonNull(position, "position");
        if (maxRank < 1 || pointCostPerRank < 1) {
            throw new IllegalArgumentException("Perk rank and point cost must be positive");
        }
        if (requiredSkillLevel < 0 || requiredSkillLevel > 100) {
            throw new IllegalArgumentException("Required skill level must be between 0 and 100");
        }
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("Perk must define at least one effect");
        }
        requirements = Set.copyOf(requirements);
        effects = List.copyOf(effects);
    }
}
