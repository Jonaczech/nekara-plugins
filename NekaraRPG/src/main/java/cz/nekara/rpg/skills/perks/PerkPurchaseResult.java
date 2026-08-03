package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.Objects;

public record PerkPurchaseResult(
    PerkPurchaseStatus status,
    SkillProfile profile,
    SkillProgressSnapshot progress
) {
    public PerkPurchaseResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(progress, "progress");
    }
}
