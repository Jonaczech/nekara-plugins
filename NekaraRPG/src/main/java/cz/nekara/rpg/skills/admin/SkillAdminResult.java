package cz.nekara.rpg.skills.admin;

import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.Objects;

public record SkillAdminResult(
    SkillProfile profile,
    SkillProgressSnapshot progress,
    SkillAdminOperation operation,
    SkillAdminStatus status,
    long affectedValue
) {
    public SkillAdminResult {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(status, "status");
        if (affectedValue < 0) {
            throw new IllegalArgumentException("Affected administrative value cannot be negative");
        }
    }

    public boolean changed() {
        return status == SkillAdminStatus.CHANGED;
    }
}
