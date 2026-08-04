package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillLevelProgress;
import cz.nekara.rpg.skills.SkillProgressBar;
import cz.nekara.rpg.skills.experience.ExperienceAwardResult;
import cz.nekara.rpg.skills.experience.ExperienceAwardStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * Shows only persisted experience awards and combines consecutive identical notices.
 * All entry points are called from the server thread after the asynchronous write completes.
 */
final class SkillExperienceFeedback {
    private static final long MERGE_DELAY_TICKS = 8L;
    private static final long DISPLAY_RESERVATION_MILLIS = 1_500L;

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, PendingFeedback> pendingByPlayer = new HashMap<>();
    private final Map<UUID, Long> displayUntilByPlayer = new HashMap<>();
    private BukkitTask flushTask;

    SkillExperienceFeedback(NekaraRPGPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    void record(UUID playerId, SkillId skill, String source, ExperienceAwardResult result) {
        if (result.status() != ExperienceAwardStatus.AWARDED || result.awardedExperience() < 1) {
            return;
        }
        SkillLevelProgress progress = result.progress().orElseThrow().skill(skill);
        PendingFeedback current = pendingByPlayer.get(playerId);
        if (current != null && (!current.skill().equals(skill) || !current.source().equals(source))) {
            send(playerId, current);
            pendingByPlayer.remove(playerId);
            current = null;
        }
        if (current == null) {
            pendingByPlayer.put(playerId, new PendingFeedback(
                skill,
                source,
                result.awardedExperience(),
                progress,
                levelledUp(progress, result.awardedExperience())
            ));
        } else {
            pendingByPlayer.put(playerId, new PendingFeedback(
                skill,
                source,
                saturatedAdd(current.experience(), result.awardedExperience()),
                progress,
                current.levelledUp() || levelledUp(progress, result.awardedExperience())
            ));
        }
        if (flushTask == null) {
            flushTask = Bukkit.getScheduler().runTaskLater(plugin, this::flush, MERGE_DELAY_TICKS);
        }
    }

    void clear() {
        pendingByPlayer.clear();
        displayUntilByPlayer.clear();
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
    }

    private void flush() {
        flushTask = null;
        Map<UUID, PendingFeedback> feedback = new HashMap<>(pendingByPlayer);
        pendingByPlayer.clear();
        feedback.forEach(this::send);
    }

    private void send(UUID playerId, PendingFeedback feedback) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        messages.sendActionBar(player, "skills-experience-awarded", Map.of(
            "experience", feedback.experience(),
            "progress_bar", SkillProgressBar.miniMessage(feedback.progress()),
            "progress", progressText(feedback.progress())
        ));
        if (feedback.levelledUp()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.0F);
        }
        displayUntilByPlayer.put(playerId, System.currentTimeMillis() + DISPLAY_RESERVATION_MILLIS);
    }

    boolean isDisplaying(UUID playerId) {
        Long displayUntil = displayUntilByPlayer.get(playerId);
        if (displayUntil == null) {
            return false;
        }
        if (displayUntil > System.currentTimeMillis()) {
            return true;
        }
        displayUntilByPlayer.remove(playerId);
        return false;
    }

    private static long saturatedAdd(long first, long second) {
        return first > Long.MAX_VALUE - second ? Long.MAX_VALUE : first + second;
    }

    static String progressText(SkillLevelProgress progress) {
        return SkillProgressBar.percentageText(progress);
    }

    static boolean levelledUp(SkillLevelProgress progress, long awardedExperience) {
        return awardedExperience > 0 && (progress.capped()
            || progress.experienceIntoLevel() < awardedExperience);
    }

    private record PendingFeedback(
        SkillId skill,
        String source,
        long experience,
        SkillLevelProgress progress,
        boolean levelledUp
    ) {
    }
}
