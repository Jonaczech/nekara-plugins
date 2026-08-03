package cz.nekara.rpg.skills;

public final class SkillProgressionCurve {
    public static final int DEFAULT_MAX_LEVEL = 100;

    private final int maxLevel;
    private final long baseExperience;
    private final long linearGrowth;
    private final long quadraticGrowth;

    public SkillProgressionCurve(
        int maxLevel,
        long baseExperience,
        long linearGrowth,
        long quadraticGrowth
    ) {
        if (maxLevel < 1) {
            throw new IllegalArgumentException("Maximum level must be positive");
        }
        if (baseExperience < 1 || linearGrowth < 0 || quadraticGrowth < 0) {
            throw new IllegalArgumentException("Experience curve terms must be non-negative and base must be positive");
        }
        this.maxLevel = maxLevel;
        this.baseExperience = baseExperience;
        this.linearGrowth = linearGrowth;
        this.quadraticGrowth = quadraticGrowth;

        cumulativeExperienceForLevel(maxLevel);
    }

    public static SkillProgressionCurve defaultCurve() {
        return new SkillProgressionCurve(DEFAULT_MAX_LEVEL, 100, 35, 2);
    }

    public int maxLevel() {
        return maxLevel;
    }

    public long experienceForNextLevel(int currentLevel) {
        requireLevel(currentLevel);
        if (currentLevel == maxLevel) {
            return 0;
        }

        long level = currentLevel;
        long linear = Math.multiplyExact(linearGrowth, level);
        long quadratic = Math.multiplyExact(quadraticGrowth, Math.multiplyExact(level, level));
        return Math.addExact(baseExperience, Math.addExact(linear, quadratic));
    }

    public long cumulativeExperienceForLevel(int level) {
        requireLevel(level);
        long total = 0;
        for (int current = 0; current < level; current++) {
            total = Math.addExact(total, experienceForNextLevel(current));
        }
        return total;
    }

    public SkillLevelProgress resolve(long totalExperience) {
        if (totalExperience < 0) {
            throw new IllegalArgumentException("Total experience cannot be negative");
        }

        long remaining = totalExperience;
        int level = 0;
        while (level < maxLevel) {
            long next = experienceForNextLevel(level);
            if (remaining < next) {
                return new SkillLevelProgress(totalExperience, level, remaining, next);
            }
            remaining -= next;
            level++;
        }
        return new SkillLevelProgress(totalExperience, maxLevel, 0, 0);
    }

    private void requireLevel(int level) {
        if (level < 0 || level > maxLevel) {
            throw new IllegalArgumentException("Level must be between 0 and " + maxLevel);
        }
    }
}
