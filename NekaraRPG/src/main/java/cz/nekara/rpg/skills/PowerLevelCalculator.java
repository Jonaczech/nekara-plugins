package cz.nekara.rpg.skills;

import java.util.Map;
import java.util.Objects;

public final class PowerLevelCalculator {
    private final int maxLevel;

    public PowerLevelCalculator(int maxLevel) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Maximum level must be positive");
        }
        this.maxLevel = maxLevel;
    }

    public PowerProgress calculate(Map<SkillId, Integer> skillLevels) {
        Objects.requireNonNull(skillLevels, "skillLevels");
        int total = 0;
        for (SkillId skill : SkillId.gameplaySkills()) {
            Integer level = skillLevels.get(skill);
            if (level == null) {
                level = 0;
            }
            if (level < 0 || level > maxLevel) {
                throw new IllegalArgumentException("Invalid level for " + skill.id() + ": " + level);
            }
            total = Math.addExact(total, level);
        }

        int skillCount = SkillId.gameplaySkills().size();
        int powerLevel = Math.min(maxLevel, total / skillCount);
        int levelsUntilNext = powerLevel == maxLevel
            ? 0
            : skillCount - (total % skillCount);
        return new PowerProgress(powerLevel, total, levelsUntilNext);
    }
}
