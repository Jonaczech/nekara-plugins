package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.Objects;
import java.util.Optional;

public record ExperienceAwardResult(
    ExperienceAwardStatus status,
    long awardedExperience,
    Optional<SkillProfile> profile,
    Optional<SkillProgressSnapshot> progress
) {
    public ExperienceAwardResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(progress, "progress");
        if (awardedExperience < 0) {
            throw new IllegalArgumentException("Awarded experience cannot be negative");
        }
        if ((status == ExperienceAwardStatus.AWARDED) != (awardedExperience > 0)) {
            throw new IllegalArgumentException("Only an awarded result can contain positive experience");
        }
        if (profile.isPresent() != progress.isPresent()) {
            throw new IllegalArgumentException("Profile and progress must be present together");
        }
    }

    public static ExperienceAwardResult withoutProfile(ExperienceAwardStatus status) {
        return new ExperienceAwardResult(status, 0, Optional.empty(), Optional.empty());
    }
}
