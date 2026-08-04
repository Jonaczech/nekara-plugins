package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.skills.SkillLevelProgress;
import cz.nekara.rpg.skills.SkillProgressBar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillExperienceFeedbackTest {
    @Test
    void showsTheCurrentProgressAsAPercentage() {
        assertEquals(
            "28 %",
            SkillExperienceFeedback.progressText(new SkillLevelProgress(138, 1, 38, 137))
        );
    }

    @Test
    void showsAFullPercentageAtTheLevelCap() {
        assertEquals(
            "100 %",
            SkillExperienceFeedback.progressText(new SkillLevelProgress(839_950, 100, 0, 0))
        );
    }

    @Test
    void detectsOnlyAwardsThatCrossIntoTheNextSkillLevel() {
        assertEquals(false, SkillExperienceFeedback.levelledUp(
            new SkillLevelProgress(138, 1, 38, 137), 4));
        assertEquals(true, SkillExperienceFeedback.levelledUp(
            new SkillLevelProgress(300, 2, 1, 174), 4));
        assertEquals(true, SkillExperienceFeedback.levelledUp(
            new SkillLevelProgress(839_950, 100, 0, 0), 5));
    }

    @Test
    void rendersACompactRpgBarWithFilledAndEmptySegments() {
        SkillLevelProgress progress = new SkillLevelProgress(138, 1, 38, 137);

        assertEquals(4, SkillProgressBar.filledSegments(progress));
        assertEquals("<green>▰▰▰▰</green><dark_gray>▱▱▱▱▱▱▱▱▱▱▱▱</dark_gray>",
            SkillProgressBar.miniMessage(progress));
        assertEquals("Zbývá: 99 XP do další úrovně", SkillProgressBar.remainingText(progress));
    }

    @Test
    void fillsEverySegmentAtTheLevelCap() {
        SkillLevelProgress progress = new SkillLevelProgress(839_950, 100, 0, 0);

        assertEquals(SkillProgressBar.SEGMENT_COUNT, SkillProgressBar.filledSegments(progress));
        assertEquals("Úroveň 100 (maximum)", SkillProgressBar.remainingText(progress));
    }
}
