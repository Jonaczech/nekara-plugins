package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import java.util.Objects;

public record ExperienceFingerprint(
    String playerKey,
    SkillId skill,
    String sourceType,
    String sourceKey
) {
    public ExperienceFingerprint {
        Objects.requireNonNull(playerKey, "playerKey");
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(sourceType, "sourceType");
        Objects.requireNonNull(sourceKey, "sourceKey");
        if (playerKey.isBlank() || sourceType.isBlank() || sourceKey.isBlank()) {
            throw new IllegalArgumentException("Experience fingerprint fields cannot be blank");
        }
        if (!skill.gainsExperience()) {
            throw new IllegalArgumentException("Derived skills cannot receive direct experience");
        }
    }
}
