package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.PowerLevelCalculator;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillLevelProgress;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class SkillProgressResolver {
    private final SkillProgressionCurve progressionCurve;
    private final PowerLevelCalculator powerLevelCalculator;

    public SkillProgressResolver(SkillProgressionCurve progressionCurve) {
        this.progressionCurve = Objects.requireNonNull(progressionCurve, "progressionCurve");
        this.powerLevelCalculator = new PowerLevelCalculator(progressionCurve.maxLevel());
    }

    public SkillProgressSnapshot resolve(SkillProfile profile) {
        Objects.requireNonNull(profile, "profile");
        EnumMap<SkillId, SkillLevelProgress> progress = new EnumMap<>(SkillId.class);
        EnumMap<SkillId, Integer> levels = new EnumMap<>(SkillId.class);
        for (SkillId skill : SkillId.gameplaySkills()) {
            SkillLevelProgress skillProgress = progressionCurve.resolve(profile.totalExperience(skill));
            progress.put(skill, skillProgress);
            levels.put(skill, skillProgress.level());
        }
        return new SkillProgressSnapshot(progress, powerLevelCalculator.calculate(levels, profile.newGamePlusRanks()));
    }
}
