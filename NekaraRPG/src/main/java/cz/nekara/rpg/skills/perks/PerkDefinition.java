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
    List<Integer> requiredSkillLevelsByRank,
    Set<PerkRequirement> requirements,
    List<PerkEffectDefinition> effects,
    PerkPosition position
) {
    public PerkDefinition(
        PerkId id,
        SkillId skill,
        int maxRank,
        int pointCostPerRank,
        int requiredSkillLevel,
        Set<PerkRequirement> requirements,
        List<PerkEffectDefinition> effects,
        PerkPosition position
    ) {
        this(id, skill, maxRank, pointCostPerRank, requiredSkillLevel,
            defaultRankRequirements(maxRank, requiredSkillLevel), requirements, effects, position);
    }

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
        Objects.requireNonNull(requiredSkillLevelsByRank, "requiredSkillLevelsByRank");
        if (requiredSkillLevelsByRank.size() != maxRank
            || requiredSkillLevelsByRank.stream().anyMatch(level -> level == null || level < 0 || level > 100)
            || requiredSkillLevelsByRank.getFirst() != requiredSkillLevel) {
            throw new IllegalArgumentException("Rank level requirements must start at the perk requirement");
        }
        for (int index = 1; index < requiredSkillLevelsByRank.size(); index++) {
            if (requiredSkillLevelsByRank.get(index) < requiredSkillLevelsByRank.get(index - 1)) {
                throw new IllegalArgumentException("Rank level requirements must not decrease");
            }
        }
        if (effects.stream().anyMatch(effect -> effect instanceof RankedStatPerkEffect ranked
            && ranked.amountsByRank().size() != maxRank)) {
            throw new IllegalArgumentException("Ranked stat values must match perk max rank");
        }
        if (effects.isEmpty()) {
            throw new IllegalArgumentException("Perk must define at least one effect");
        }
        requirements = Set.copyOf(requirements);
        effects = List.copyOf(effects);
        requiredSkillLevelsByRank = List.copyOf(requiredSkillLevelsByRank);
    }

    public int requiredSkillLevelForRank(int rank) {
        if (rank < 1 || rank > maxRank) {
            throw new IllegalArgumentException("Rank must be between 1 and " + maxRank);
        }
        return requiredSkillLevelsByRank.get(rank - 1);
    }

    private static List<Integer> defaultRankRequirements(int maxRank, int requiredSkillLevel) {
        if (maxRank == 5 && requiredSkillLevel == 0) {
            return List.of(0, 10, 20, 35, 50);
        }
        if (maxRank == 5 && requiredSkillLevel == 20) {
            return List.of(20, 35, 50, 70, 85);
        }
        return java.util.Collections.nCopies(maxRank, requiredSkillLevel);
    }
}
