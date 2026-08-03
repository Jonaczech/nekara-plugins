package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import java.util.Objects;

public record ExperienceContext(
    SkillId skill,
    boolean cancelled,
    boolean creative,
    boolean spectator,
    boolean synthetic,
    boolean playerPlacedSource,
    boolean automatedSource,
    int recentChunkAwards
) {
    public ExperienceContext {
        Objects.requireNonNull(skill, "skill");
        if (!skill.gainsExperience()) {
            throw new IllegalArgumentException("Derived skills cannot receive direct experience");
        }
        if (recentChunkAwards < 0) {
            throw new IllegalArgumentException("Recent chunk award count cannot be negative");
        }
    }
}
