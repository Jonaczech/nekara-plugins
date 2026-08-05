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
        return calculate(skillLevels, Map.of());
    }

    public PowerProgress calculate(Map<SkillId, Integer> skillLevels, Map<SkillId, Integer> newGamePlusRanks) {
        Objects.requireNonNull(skillLevels, "skillLevels");
        Objects.requireNonNull(newGamePlusRanks, "newGamePlusRanks");
        int total = 0;
        for (SkillId skill : SkillId.activeGameplaySkills()) {
            Integer level = skillLevels.get(skill);
            if (level == null) {
                level = 0;
            }
            if (level < 0 || level > maxLevel) {
                throw new IllegalArgumentException("Invalid level for " + skill.id() + ": " + level);
            }
            int rebirth = newGamePlusRanks.getOrDefault(skill, 0);
            if (rebirth < 0 || rebirth > 1) throw new IllegalArgumentException("Invalid New Game+ rank for " + skill.id());
            total = Math.addExact(total, Math.addExact(level, rebirth * maxLevel));
        }

        int skillCount = SkillId.activeGameplaySkills().size();
        // The first trained skill is an onboarding milestone. Afterwards, each power level
        // still represents one full cross-skill average and cannot be farmed from one activity.
        int powerLevel = total == 0 ? 0 : Math.min(maxLevel * 2, 1 + (total - 1) / skillCount);
        int levelsUntilNext = powerLevel == maxLevel * 2 ? 0
            : total == 0 ? 1 : skillCount - ((total - 1) % skillCount);
        return new PowerProgress(powerLevel, total, levelsUntilNext);
    }
}
