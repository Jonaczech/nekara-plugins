package cz.nekara.rpg.skills.admin;

import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.List;
import java.util.Objects;

public record SkillAdminInspection(
    SkillProfile profile,
    SkillProgressSnapshot progress,
    List<SkillAuditEntry> recentAuditEntries
) {
    public SkillAdminInspection {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(progress, "progress");
        recentAuditEntries = List.copyOf(Objects.requireNonNull(recentAuditEntries, "recentAuditEntries"));
    }
}
