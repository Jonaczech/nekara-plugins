package cz.nekara.rpg.modules.sitting;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.SittingConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.sitting.SitResult;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SittingModule implements NekaraModule, Listener {
    public static final String ID = "sitting";
    private static final String SEAT_TAG = "nekararpg-seat";

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final Map<UUID, ArmorStand> seats = new HashMap<>();
    private BukkitTask cleanupTask;
    private boolean enabled;

    public SittingModule(NekaraRPGPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void enable() {
        if (enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupInvalidSeats, 1L, 1L);
        enabled = true;
    }

    @Override
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
        enabled = false;
    }

    @Override
    public void reload() {
        cleanupInvalidSeats();
    }

    @Override
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
        return removeSeat(player.getUniqueId(), false);
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
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        removeSeat(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        removeSeat(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        removeSeat(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && plugin.configuration().get().sitting().standOnDamage()) {
            removeSeat(player.getUniqueId(), true);
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
}
