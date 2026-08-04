package cz.nekara.rpg.modules.sitting;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.LyingConfig;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

final class MannequinLyingVisualService implements LyingVisualService {
    private static final String VISUAL_TAG = "nekararpg-lying-visual";

    private final NekaraRPGPlugin plugin;
    private final LyingVisualTransform transform;
    private final Map<UUID, Mannequin> visuals = new HashMap<>();
    private final Map<UUID, Location> visualLocations = new HashMap<>();
    private final Map<UUID, Boolean> previousInvisibility = new HashMap<>();
    private final Map<UUID, Set<UUID>> equipmentHiddenFor = new HashMap<>();
    private final Map<UUID, Integer> equipmentHashes = new HashMap<>();
    private boolean available = true;

    MannequinLyingVisualService(NekaraRPGPlugin plugin, LyingConfig config) {
        this.plugin = plugin;
        this.transform = new LyingVisualTransform(
            config.mannequinYawOffsetDegrees(),
            config.mannequinForwardOffset(),
            config.mannequinSideOffset(),
            config.mannequinVerticalOffset()
        );
        removeOrphans();
    }

    @Override
    public void show(Player subject, Collection<? extends Player> viewers) {
        if (ensureVisual(subject)) {
            boolean equipmentChanged = refreshEquipmentHash(subject);
            viewers.forEach(viewer -> hideOriginalEquipment(subject, viewer, equipmentChanged));
        }
    }

    @Override
    public void show(Player subject, Player viewer) {
        if (ensureVisual(subject)) {
            refreshEquipmentHash(subject);
            hideOriginalEquipment(subject, viewer, false);
        }
    }

    @Override
    public void hide(Player subject, Collection<? extends Player> viewers) {
        removeVisual(subject);
    }

    @Override
    public void hide(Player subject, Player viewer) {
        Set<UUID> viewers = equipmentHiddenFor.get(subject.getUniqueId());
        if (viewers != null) {
            viewers.remove(viewer.getUniqueId());
        }
    }

    @Override
    public void forgetViewer(UUID viewerId) {
        equipmentHiddenFor.values().forEach(viewers -> viewers.remove(viewerId));
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
            restoreOriginalEquipment(subjectId, subject);
            restoreVisibility(subjectId, subject);
        }
        equipmentHiddenFor.clear();
        visualLocations.clear();
        equipmentHashes.clear();
        previousInvisibility.clear();
    }

    private boolean ensureVisual(Player subject) {
        if (!available || !subject.isOnline()) {
            return false;
        }
        Mannequin current = visuals.get(subject.getUniqueId());
        if (current != null && current.isValid()) {
            copyEquipment(subject, current);
            return true;
        }
        try {
            UUID subjectId = subject.getUniqueId();
            previousInvisibility.putIfAbsent(subjectId, subject.isInvisible());
            Location visualLocation = visualLocations.computeIfAbsent(
                subjectId, ignored -> transform.apply(subject.getLocation()));
            Mannequin visual = subject.getWorld().spawn(
                visualLocation, Mannequin.class);
            visuals.put(subjectId, visual);
            configure(subject, visual);
            subject.setInvisible(true);
            return true;
        } catch (RuntimeException | LinkageError exception) {
            available = false;
            removeVisual(subject);
            plugin.getLogger().log(Level.WARNING,
                "Native mannequin lying visual failed; using the safe server pose fallback.",
                exception);
            return false;
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
        visualLocations.remove(subjectId);
        equipmentHashes.remove(subjectId);
        restoreOriginalEquipment(subjectId, subject);
        restoreVisibility(subjectId, subject);
    }

    private boolean refreshEquipmentHash(Player subject) {
        int currentHash = actualEquipment(subject).hashCode();
        Integer previousHash = equipmentHashes.put(subject.getUniqueId(), currentHash);
        return previousHash != null && previousHash != currentHash;
    }

    private void hideOriginalEquipment(Player subject, Player viewer, boolean force) {
        if (!viewer.isOnline()) {
            return;
        }
        boolean newViewer = equipmentHiddenFor
            .computeIfAbsent(subject.getUniqueId(), ignored -> new HashSet<>())
            .add(viewer.getUniqueId());
        if (newViewer || force) {
            viewer.sendEquipmentChange(subject, emptyEquipment());
        }
    }

    private void restoreOriginalEquipment(UUID subjectId, Player subject) {
        Set<UUID> viewers = equipmentHiddenFor.remove(subjectId);
        if (subject == null || viewers == null) {
            return;
        }
        Map<EquipmentSlot, ItemStack> equipment = actualEquipment(subject);
        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                viewer.sendEquipmentChange(subject, equipment);
            }
        }
    }

    private static Map<EquipmentSlot, ItemStack> emptyEquipment() {
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        ItemStack air = new ItemStack(Material.AIR);
        equipment.put(EquipmentSlot.HAND, air);
        equipment.put(EquipmentSlot.OFF_HAND, air);
        equipment.put(EquipmentSlot.HEAD, air);
        equipment.put(EquipmentSlot.CHEST, air);
        equipment.put(EquipmentSlot.LEGS, air);
        equipment.put(EquipmentSlot.FEET, air);
        return equipment;
    }

    private static Map<EquipmentSlot, ItemStack> actualEquipment(Player subject) {
        EnumMap<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
        equipment.put(EquipmentSlot.HAND, subject.getInventory().getItemInMainHand());
        equipment.put(EquipmentSlot.OFF_HAND, subject.getInventory().getItemInOffHand());
        equipment.put(EquipmentSlot.HEAD, itemOrAir(subject.getInventory().getHelmet()));
        equipment.put(EquipmentSlot.CHEST, itemOrAir(subject.getInventory().getChestplate()));
        equipment.put(EquipmentSlot.LEGS, itemOrAir(subject.getInventory().getLeggings()));
        equipment.put(EquipmentSlot.FEET, itemOrAir(subject.getInventory().getBoots()));
        return equipment;
    }

    private static ItemStack itemOrAir(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item;
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
