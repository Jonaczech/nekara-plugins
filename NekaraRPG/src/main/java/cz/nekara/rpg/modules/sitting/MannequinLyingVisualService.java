package cz.nekara.rpg.modules.sitting;

import cz.nekara.rpg.NekaraRPGPlugin;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;

final class MannequinLyingVisualService implements LyingVisualService {
    private static final String VISUAL_TAG = "nekararpg-lying-visual";

    private final NekaraRPGPlugin plugin;
    private final Map<UUID, Mannequin> visuals = new HashMap<>();
    private final Map<UUID, Boolean> previousInvisibility = new HashMap<>();
    private boolean available = true;

    MannequinLyingVisualService(NekaraRPGPlugin plugin) {
        this.plugin = plugin;
        removeOrphans();
    }

    @Override
    public void show(Player subject, Collection<? extends Player> viewers) {
        ensureVisual(subject);
    }

    @Override
    public void show(Player subject, Player viewer) {
        ensureVisual(subject);
    }

    @Override
    public void hide(Player subject, Collection<? extends Player> viewers) {
        removeVisual(subject);
    }

    @Override
    public void hide(Player subject, Player viewer) {
        // A viewer leaving tracking range does not end the subject's lying visual.
    }

    @Override
    public void forgetViewer(UUID viewerId) {
        // Mannequins use normal server entity tracking and keep no viewer state.
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void close() {
        for (UUID subjectId : java.util.List.copyOf(visuals.keySet())) {
            Player subject = Bukkit.getPlayer(subjectId);
            Mannequin visual = visuals.remove(subjectId);
            if (visual != null && visual.isValid()) {
                visual.remove();
            }
            restoreVisibility(subjectId, subject);
        }
        previousInvisibility.clear();
    }

    private void ensureVisual(Player subject) {
        if (!available || !subject.isOnline()) {
            return;
        }
        Mannequin current = visuals.get(subject.getUniqueId());
        if (current != null && current.isValid()) {
            current.teleport(subject.getLocation());
            copyEquipment(subject, current);
            return;
        }
        try {
            previousInvisibility.putIfAbsent(subject.getUniqueId(), subject.isInvisible());
            Mannequin visual = subject.getWorld().spawn(subject.getLocation(), Mannequin.class);
            visuals.put(subject.getUniqueId(), visual);
            configure(subject, visual);
            subject.setInvisible(true);
        } catch (RuntimeException | LinkageError exception) {
            available = false;
            removeVisual(subject);
            plugin.getLogger().log(Level.WARNING,
                "Native mannequin lying visual failed; using the safe server pose fallback.",
                exception);
        }
    }

    private void configure(Player subject, Mannequin visual) {
        visual.addScoreboardTag(VISUAL_TAG);
        visual.setProfile(ResolvableProfile.resolvableProfile(subject.getPlayerProfile()));
        visual.setPose(Pose.SLEEPING, true);
        visual.setImmovable(true);
        visual.setAI(false);
        visual.setGravity(false);
        visual.setCollidable(false);
        visual.setInvulnerable(true);
        visual.setSilent(true);
        visual.setPersistent(false);
        visual.setCanPickupItems(false);
        visual.setCustomNameVisible(false);
        visual.setDescription(null);
        copyEquipment(subject, visual);
    }

    private void copyEquipment(Player subject, Mannequin visual) {
        EntityEquipment equipment = visual.getEquipment();
        equipment.setArmorContents(subject.getInventory().getArmorContents());
        equipment.setItemInMainHand(subject.getInventory().getItemInMainHand());
        equipment.setItemInOffHand(subject.getInventory().getItemInOffHand());
    }

    private void removeVisual(Player subject) {
        UUID subjectId = subject.getUniqueId();
        Mannequin visual = visuals.remove(subjectId);
        if (visual != null && visual.isValid()) {
            visual.remove();
        }
        restoreVisibility(subjectId, subject);
    }

    private void restoreVisibility(UUID subjectId, Player subject) {
        Boolean invisibleBefore = previousInvisibility.remove(subjectId);
        if (subject != null && invisibleBefore != null) {
            subject.setInvisible(invisibleBefore);
        }
    }

    private void removeOrphans() {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Mannequin mannequin : world.getEntitiesByClass(Mannequin.class)) {
                if (mannequin.getScoreboardTags().contains(VISUAL_TAG)) {
                    mannequin.remove();
                }
            }
        }
    }
}
