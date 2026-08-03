package cz.nekara.rpg.skills;

public record SkillLevelProgress(
    long totalExperience,
    int level,
    long experienceIntoLevel,
    long experienceForNextLevel
) {
    public SkillLevelProgress {
        if (totalExperience < 0) {
            throw new IllegalArgumentException("Total experience cannot be negative");
        }
        if (level < 0) {
            throw new IllegalArgumentException("Level cannot be negative");
        }
        if (experienceIntoLevel < 0 || experienceForNextLevel < 0) {
            throw new IllegalArgumentException("Level experience values cannot be negative");
        }
        if (experienceForNextLevel > 0 && experienceIntoLevel >= experienceForNextLevel) {
            throw new IllegalArgumentException("Experience into level must be below the next-level cost");
        }
        if (experienceForNextLevel == 0 && experienceIntoLevel != 0) {
            throw new IllegalArgumentException("Capped progress cannot retain in-level experience");
        }
    }

    public boolean capped() {
        return experienceForNextLevel == 0;
    }
}
