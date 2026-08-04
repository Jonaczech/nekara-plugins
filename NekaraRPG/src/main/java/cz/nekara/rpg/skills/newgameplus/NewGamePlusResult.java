package cz.nekara.rpg.skills.newgameplus;

import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.Objects;

public record NewGamePlusResult(NewGamePlusStatus status, SkillProfile profile,
                                SkillProgressSnapshot progress, int refundedPoints) {
    public NewGamePlusResult { Objects.requireNonNull(status); Objects.requireNonNull(profile); Objects.requireNonNull(progress); }
}
