package cz.nekara.rpg.modules.sitting;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.SittingConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.sitting.SitResult;
import cz.nekara.rpg.sitting.LyingMovementPolicy;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SittingModule implements Listener {
    private static final String SEAT_TAG = "nekararpg-seat";

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, ArmorStand> seats = new HashMap<>();
    private final Map<UUID, Long> lyingSince = new HashMap<>();
    private LyingVisualService lyingVisuals = new ServerPoseLyingVisualService();
    private BukkitTask cleanupTask;
    private boolean enabled;

    public SittingModule(NekaraRPGPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void enable() {
        if (enabled) {
            return;
        }
        lyingVisuals = createLyingVisualService();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupInvalidSeats, 10L, 10L);
        enabled = true;
    }

    public void disable() {
        if (!enabled) {
            return;
        }
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        HandlerList.unregisterAll(this);
        for (UUID playerId : new ArrayList<>(seats.keySet())) {
            removeSeat(playerId, false);
        }
        for (UUID playerId : new ArrayList<>(lyingSince.keySet())) {
            rise(playerId, false);
        }
        lyingVisuals.close();
        lyingVisuals = new ServerPoseLyingVisualService();
        enabled = false;
    }

    public void reload() {
        lyingVisuals.close();
        lyingVisuals = createLyingVisualService();
        for (UUID playerId : List.copyOf(lyingSince.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                lyingVisuals.show(player, trackedViewers(player));
            }
        }
        cleanupInvalidSeats();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public SitResult sit(Player player) {
        if (!enabled) {
            return SitResult.MODULE_DISABLED;
        }
        UUID playerId = player.getUniqueId();
        if (isSitting(playerId)) {
            return SitResult.ALREADY_SITTING;
        }
        if (isLying(playerId)) {
            return SitResult.INVALID_STATE;
        }
        if (player.isInsideVehicle()) {
            return SitResult.ALREADY_RIDING;
        }
        SittingConfig config = plugin.configuration().get().sitting();
        if (player.isDead() || player.isSleeping() || player.isGliding() || player.isSwimming()
                || (!config.allowCreative() && player.getGameMode() == GameMode.CREATIVE)
                || (!config.allowFlying() && player.isFlying())) {
            return SitResult.INVALID_STATE;
        }
        if (config.requireGround() && !player.isOnGround()) {
            return SitResult.NOT_ON_GROUND;
        }

        Location seatLocation = player.getLocation().clone().add(0.0, config.seatYOffset(), 0.0);
        ArmorStand seat = (ArmorStand) player.getWorld().spawnEntity(seatLocation, EntityType.ARMOR_STAND);
        configureSeat(seat);
        if (!seat.addPassenger(player)) {
            seat.remove();
            return SitResult.FAILED;
        }
        seats.put(playerId, seat);
        return SitResult.SUCCESS;
    }

    public boolean stand(Player player) {
        return removeSeat(player.getUniqueId(), false) | rise(player.getUniqueId(), false);
    }

    public boolean lie(Player player) {
        if (!enabled || isSeated(player) || player.isDead() || player.isSleeping() || !player.isOnGround()
                || player.isGliding() || player.isSwimming() || player.isInsideVehicle()
                || player.isFlying()) {
            return false;
        }
        player.setPose(Pose.SLEEPING, true);
        UUID playerId = player.getUniqueId();
        lyingSince.put(playerId, System.currentTimeMillis());
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player current = Bukkit.getPlayer(playerId);
            if (enabled && current != null && current.isOnline() && isLying(playerId)) {
                current.setPose(Pose.SLEEPING, true);
                lyingVisuals.show(current, trackedViewers(current));
            }
        });
        return true;
    }

    public boolean rise(Player player) {
        return rise(player.getUniqueId(), false);
    }

    public boolean isLying(UUID playerId) {
        return lyingSince.containsKey(playerId);
    }

    public boolean isLying(Player player) {
        return isLying(player.getUniqueId());
    }

    public long lyingSince(UUID playerId) {
        return lyingSince.getOrDefault(playerId, 0L);
    }

    public boolean isSitting(UUID playerId) {
        ArmorStand seat = seats.get(playerId);
        return seat != null && seat.isValid();
    }

    public Collection<Player> seatedPlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSeated(player)) {
                players.add(player);
            }
        }
        return List.copyOf(players);
    }

    public Collection<Player> restingPlayers() {
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isSeated(player) || isLying(player)) {
                players.add(player);
            }
        }
        return List.copyOf(players);
    }

    public int seatedCount() {
        return seatedPlayers().size();
    }

    public boolean isSeated(Player player) {
        if (isSitting(player.getUniqueId())) {
            return true;
        }
        SittingConfig config = plugin.configuration().get().sitting();
        if (!config.detectExternalSeats()) {
            return false;
        }
        Entity vehicle = player.getVehicle();
        while (vehicle != null) {
            if (config.externalSeatEntityTypes().contains(vehicle.getType())) {
                return true;
            }
            vehicle = vehicle.getVehicle();
        }
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeSeat(event.getPlayer().getUniqueId(), false);
        rise(event.getPlayer().getUniqueId(), false);
        lyingVisuals.forgetViewer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        removeSeat(event.getPlayer().getUniqueId(), false);
        rise(event.getPlayer().getUniqueId(), false);
        lyingVisuals.forgetViewer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        removeSeat(event.getPlayer().getUniqueId(), false);
        rise(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        removeSeat(event.getPlayer().getUniqueId(), false);
        rise(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (plugin.configuration().get().sitting().standOnDamage()) {
            removeSeat(player.getUniqueId(), true);
        }
        if (plugin.configuration().get().campfire().lying().wakeOnDamage()) {
            rise(player.getUniqueId(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!isLying(event.getPlayer()) || event.getTo() == null) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (LyingMovementPolicy.changesPosition(
                from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ())) {
            event.setTo(from);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            rise(event.getPlayer().getUniqueId(), false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrackEntity(PlayerTrackEntityEvent event) {
        if (!(event.getEntity() instanceof Player subject) || !isLying(subject)) {
            return;
        }
        UUID subjectId = subject.getUniqueId();
        UUID viewerId = event.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player currentSubject = Bukkit.getPlayer(subjectId);
            Player currentViewer = Bukkit.getPlayer(viewerId);
            if (enabled && currentSubject != null && currentViewer != null && isLying(subjectId)) {
                lyingVisuals.show(currentSubject, currentViewer);
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUntrackEntity(PlayerUntrackEntityEvent event) {
        if (event.getEntity() instanceof Player subject && isLying(subject)) {
            lyingVisuals.hide(subject, event.getPlayer());
        }
    }

    private void configureSeat(ArmorStand seat) {
        seat.setVisible(false);
        seat.setGravity(false);
        seat.setMarker(true);
        seat.setSmall(true);
        seat.setInvulnerable(true);
        seat.setSilent(true);
        seat.setPersistent(false);
        seat.setBasePlate(false);
        seat.addScoreboardTag(SEAT_TAG);
    }

    private void cleanupInvalidSeats() {
        for (Map.Entry<UUID, ArmorStand> entry : new ArrayList<>(seats.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            ArmorStand seat = entry.getValue();
            if (player == null || !player.isOnline() || !seat.isValid()
                    || !seat.getPassengers().contains(player)) {
                removeSeat(entry.getKey(), player != null && player.isOnline());
            }
        }
        for (UUID playerId : new ArrayList<>(lyingSince.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline() || player.isDead()) {
                rise(playerId, false);
            } else if (LyingMovementPolicy.shouldRefreshPose(player.getPose())) {
                player.setPose(Pose.SLEEPING, true);
                lyingVisuals.show(player, trackedViewers(player));
            }
        }
    }

    private boolean removeSeat(UUID playerId, boolean notify) {
        ArmorStand seat = seats.remove(playerId);
        if (seat == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.getVehicle() == seat) {
            player.leaveVehicle();
        }
        if (seat.isValid()) {
            seat.remove();
        }
        if (notify && player != null && player.isOnline()) {
            messages.sendActionBar(player, "sitting-stopped", Map.of());
        }
        return true;
    }

    private boolean rise(UUID playerId, boolean notify) {
        if (lyingSince.remove(playerId) == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            Collection<Player> viewers = trackedViewers(player);
            player.setPose(Pose.STANDING, false);
            lyingVisuals.hide(player, viewers);
            if (notify && player.isOnline()) {
                messages.sendActionBar(player, "campfire-lying-stopped", Map.of());
            }
        }
        return true;
    }

    private Collection<Player> trackedViewers(Player subject) {
        Set<Player> viewers = new LinkedHashSet<>(subject.getTrackedBy());
        if (subject.isOnline()) {
            viewers.add(subject);
        }
        return List.copyOf(viewers);
    }

    private LyingVisualService createLyingVisualService() {
        if (!plugin.configuration().get().campfire().lying().mannequinVisualEnabled()) {
            plugin.getLogger().warning(
                "Native mannequin lying visuals are disabled; using the safe server pose implementation.");
            return new ServerPoseLyingVisualService();
        }
        if (!Mannequin.validPoses().contains(Pose.SLEEPING)) {
            plugin.getLogger().warning(
                "This server does not support the sleeping mannequin pose; using the safe server pose implementation.");
            return new ServerPoseLyingVisualService();
        }
        plugin.getLogger().info("Native mannequin lying visuals enabled.");
        return new MannequinLyingVisualService(plugin);
    }

}
