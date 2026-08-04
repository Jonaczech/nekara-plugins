package cz.nekara.rpg.skills;

public record PowerProgress(int level, int contributingLevelTotal, int levelsUntilNext) {
    public PowerProgress {
        if (level < 0 || level > SkillProgressionCurve.DEFAULT_MAX_LEVEL * 2) {
            throw new IllegalArgumentException("Power level must be between 0 and 200");
        }
        if (contributingLevelTotal < 0 || levelsUntilNext < 0) {
            throw new IllegalArgumentException("Power progress values cannot be negative");
        }
    }
}
