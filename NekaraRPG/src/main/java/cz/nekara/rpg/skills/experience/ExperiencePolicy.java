package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import java.util.Objects;
import java.util.Set;

public final class ExperiencePolicy {
    private final int chunkSoftLimit;
    private final int chunkHardLimit;
    private final double farmFloorMultiplier;
    private final Set<SkillId> playerPlacedAllowedSkills;

    public ExperiencePolicy(
        int chunkSoftLimit,
        int chunkHardLimit,
        double farmFloorMultiplier,
        Set<SkillId> playerPlacedAllowedSkills
    ) {
        if (chunkSoftLimit < 0 || chunkHardLimit <= chunkSoftLimit) {
            throw new IllegalArgumentException("Chunk hard limit must be greater than its non-negative soft limit");
        }
        if (!Double.isFinite(farmFloorMultiplier)
            || farmFloorMultiplier <= 0
            || farmFloorMultiplier > 1) {
            throw new IllegalArgumentException("Farm floor multiplier must be between 0 (exclusive) and 1");
        }
        Objects.requireNonNull(playerPlacedAllowedSkills, "playerPlacedAllowedSkills");
        if (playerPlacedAllowedSkills.stream().anyMatch(skill -> !skill.gainsExperience())) {
            throw new IllegalArgumentException("Only gameplay skills may allow placed sources");
        }
        this.chunkSoftLimit = chunkSoftLimit;
        this.chunkHardLimit = chunkHardLimit;
        this.farmFloorMultiplier = farmFloorMultiplier;
        this.playerPlacedAllowedSkills = Set.copyOf(playerPlacedAllowedSkills);
    }

    public static ExperiencePolicy defaultPolicy() {
        return new ExperiencePolicy(32, 128, 0.1, Set.of(SkillId.FARMING, SkillId.WOODCUTTING));
    }

    public ExperienceDecision evaluate(ExperienceContext context) {
        Objects.requireNonNull(context, "context");
        if (context.cancelled()) {
            return ExperienceDecision.deny(ExperienceReason.CANCELLED_EVENT);
        }
        if (context.creative() || context.spectator()) {
            return ExperienceDecision.deny(ExperienceReason.UNSUPPORTED_GAME_MODE);
        }
        if (context.synthetic()) {
            return ExperienceDecision.deny(ExperienceReason.SYNTHETIC_SOURCE);
        }
        if (context.automatedSource()) {
            return ExperienceDecision.deny(ExperienceReason.AUTOMATED_SOURCE);
        }
        if (context.playerPlacedSource() && !playerPlacedAllowedSkills.contains(context.skill())) {
            return ExperienceDecision.deny(ExperienceReason.PLAYER_PLACED_SOURCE);
        }
        if (context.recentChunkAwards() >= chunkHardLimit) {
            return ExperienceDecision.deny(ExperienceReason.FARM_LIMIT);
        }
        if (context.recentChunkAwards() <= chunkSoftLimit) {
            return ExperienceDecision.allow(1, ExperienceReason.NORMAL);
        }

        double progress = (double) (context.recentChunkAwards() - chunkSoftLimit)
            / (chunkHardLimit - chunkSoftLimit);
        double multiplier = 1 - ((1 - farmFloorMultiplier) * progress);
        return ExperienceDecision.allow(Math.max(farmFloorMultiplier, multiplier), ExperienceReason.FARM_DECAY);
    }
}
