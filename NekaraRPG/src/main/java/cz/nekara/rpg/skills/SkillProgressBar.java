package cz.nekara.rpg.skills;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Shared compact visual presentation of a skill's progress within its current level.
 */
public final class SkillProgressBar {
    public static final int SEGMENT_COUNT = 16;
    private static final String FILLED_SEGMENT = "▰";
    private static final String EMPTY_SEGMENT = "▱";

    private SkillProgressBar() {
    }

    public static Component component(SkillLevelProgress progress) {
        int filled = filledSegments(progress);
        return Component.text(FILLED_SEGMENT.repeat(filled), NamedTextColor.GREEN)
            .append(Component.text(EMPTY_SEGMENT.repeat(SEGMENT_COUNT - filled), NamedTextColor.DARK_GRAY));
    }

    public static String miniMessage(SkillLevelProgress progress) {
        int filled = filledSegments(progress);
        return "<green>" + FILLED_SEGMENT.repeat(filled) + "</green>"
            + "<dark_gray>" + EMPTY_SEGMENT.repeat(SEGMENT_COUNT - filled) + "</dark_gray>";
    }

    public static String amountText(SkillLevelProgress progress) {
        if (progress.capped()) {
            return "MAX";
        }
        return progress.experienceIntoLevel() + "/" + progress.experienceForNextLevel() + " XP";
    }

    public static String percentageText(SkillLevelProgress progress) {
        if (progress.capped()) {
            return "100 %";
        }
        long percentage = Math.round((double) progress.experienceIntoLevel()
            / progress.experienceForNextLevel() * 100.0);
        return percentage + " %";
    }

    public static String remainingText(SkillLevelProgress progress) {
        if (progress.capped()) {
            return "Úroveň " + progress.level() + " (maximum)";
        }
        return "Zbývá: " + (progress.experienceForNextLevel() - progress.experienceIntoLevel())
            + " XP do další úrovně";
    }

    public static int filledSegments(SkillLevelProgress progress) {
        if (progress.capped()) {
            return SEGMENT_COUNT;
        }
        return (int) Math.floor((double) progress.experienceIntoLevel()
            / progress.experienceForNextLevel() * SEGMENT_COUNT);
    }
}
