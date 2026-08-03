package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import java.util.Objects;

public record ExperienceAwardRequest(
    String playerKey,
    SkillId skill,
    long baseExperience,
    ExperienceContext context,
    ExperienceFingerprint fingerprint
) {
    public ExperienceAwardRequest {
        Objects.requireNonNull(playerKey, "playerKey");
        Objects.requireNonNull(skill, "skill");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (playerKey.isBlank()) {
            throw new IllegalArgumentException("Player key cannot be blank");
        }
        if (!skill.gainsExperience() || baseExperience < 1) {
            throw new IllegalArgumentException("Direct experience award must target a gameplay skill");
        }
        if (!playerKey.equals(fingerprint.playerKey())
            || skill != fingerprint.skill()
            || skill != context.skill()) {
            throw new IllegalArgumentException("Experience request identity and skill must match its context");
        }
    }
}
