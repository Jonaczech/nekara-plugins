package cz.nekara.rpg.modules.dragons;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.DragonConfig;
import cz.nekara.rpg.menu.GuiItems;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.mount.ActiveMountCoordinator;
import cz.nekara.rpg.mount.ActiveMountCoordinator.ActivationResult;
import cz.nekara.rpg.mount.ActiveMountCoordinator.MountKind;
import cz.nekara.rpg.skills.milestones.PowerMilestoneId;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.HappyGhast;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Server-side Happy Ghast prototype for the Power 100 Dragon Bond milestone. */
public final class DragonsModule implements NekaraModule, Listener {
    public static final String ID = "dragons";

    private static final int STATUS_SLOT = 4;
    private static final int CALL_SLOT = 11;
    private static final int DISMISS_SLOT = 15;
    private static final int BACK_SLOT = 18;
    private static final int HORSE_SELECTION_SLOT = 11;
    private static final int DRAGON_SELECTION_SLOT = 15;
    private static final double MANUAL_FLIGHT_SPEED_MULTIPLIER = 5.0;
    private static final double MANUAL_VERTICAL_SPEED_MULTIPLIER = 3.5;
    private static final double SPRINT_SPEED_MULTIPLIER = 1.35;
    private static final double APPROACH_ARRIVAL_RADIUS = 1.5;
    private static final double ENDER_DRAGON_VISUAL_Y_OFFSET = 0.75;
    private static final float ENDER_DRAGON_YAW_OFFSET = 180.0f;
    private static final int FLAP_SOUND_INTERVAL_TICKS = 12;
    private static final int AMBIENT_SOUND_INTERVAL_TICKS = 160;

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final ActiveMountCoordinator coordinator;
    private final NamespacedKey managedKey;
    private final NamespacedKey modelKey;
    private final NamespacedKey ownerKey;
    private final Map<UUID, HappyGhast> activeDragons = new HashMap<>();
    private final Map<UUID, EnderDragon> dragonModels = new HashMap<>();
    private final Map<UUID, Location> waitingLocations = new HashMap<>();
    private final Map<UUID, Location> approachTargets = new HashMap<>();
    private final Map<UUID, Instant> summonAvailableAt = new HashMap<>();
    private final Map<UUID, Instant> recallAvailableAt = new HashMap<>();
    private final Map<UUID, Instant> modelRespawnAvailableAt = new HashMap<>();
    private final Set<UUID> pendingModelOwners = new HashSet<>();
    private final Set<UUID> requestedDismounts = new HashSet<>();
    private BukkitTask holdingTask;
    private int synchronizationTicks;
    private boolean enabled;

    public DragonsModule(
            NekaraRPGPlugin plugin,
            MessageService messages,
            ActiveMountCoordinator coordinator
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.coordinator = coordinator;
        managedKey = new NamespacedKey(plugin, "dragon-mount");
        modelKey = new NamespacedKey(plugin, "dragon-mount-model");
        ownerKey = new NamespacedKey(plugin, "dragon-owner");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public void enable() {
        if (enabled) return;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        removeStaleDragons();
        holdingTask = Bukkit.getScheduler().runTaskTimer(plugin, this::synchronizeDragonModels, 1L, 1L);
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) return;
        closeMenus();
        HandlerList.unregisterAll(this);
        if (holdingTask != null) {
            holdingTask.cancel();
            holdingTask = null;
        }
        for (Map.Entry<UUID, HappyGhast> entry : new ArrayList<>(activeDragons.entrySet())) {
            removeDragon(entry.getKey(), entry.getValue());
        }
        activeDragons.clear();
        dragonModels.clear();
        waitingLocations.clear();
        approachTargets.clear();
        summonAvailableAt.clear();
        recallAvailableAt.clear();
        modelRespawnAvailableAt.clear();
        pendingModelOwners.clear();
        requestedDismounts.clear();
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isUnlocked(Player player) {
        return enabled && plugin.skillsModule().hasPowerMilestone(
                player.getUniqueId(), PowerMilestoneId.DRAGON_BOND);
    }

    /** Returns whether the player is currently riding their own managed dragon carrier. */
    public boolean isRidingDragon(Player player) {
        return player.getVehicle() instanceof HappyGhast dragon && isManaged(dragon) && isOwner(player, dragon);
    }

    public void openMenu(Player player) {
        if (!prepare(player, true)) return;
        DragonMenuHolder holder = new DragonMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("Nekara Dragons — Můj drak", NamedTextColor.DARK_PURPLE));
        holder.inventory = inventory;
        GuiItems.fill(inventory);
        HappyGhast dragon = activeDragon(player.getUniqueId());
        inventory.setItem(STATUS_SLOT, GuiItems.item(Material.DRAGON_HEAD,
                Component.text("Dračí pouto", NamedTextColor.LIGHT_PURPLE)
                        .decorate(TextDecoration.BOLD),
                Component.text(dragon == null ? "Drak odpočívá" : "Drak je ve světě", NamedTextColor.GRAY),
                Component.text("Rychlý let bez inventáře a boje", NamedTextColor.DARK_GRAY)));
        inventory.setItem(CALL_SLOT, GuiItems.item(Material.LIME_DYE,
                Component.text("Přivolat draka", NamedTextColor.GREEN),
                Component.text("Aktivní kůň se bezpečně odvolá", NamedTextColor.GRAY)));
        inventory.setItem(DISMISS_SLOT, GuiItems.item(Material.GRAY_DYE,
                Component.text("Odvolat draka", NamedTextColor.GRAY)));
        inventory.setItem(BACK_SLOT, GuiItems.back("Zpět do NekaraRPG"));
        player.openInventory(inventory);
    }

    public void openWhistleSelection(Player player) {
        if (!prepare(player, false)) return;
        SelectionMenuHolder holder = new SelectionMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("Píšťalka — vyber mounta", NamedTextColor.GOLD));
        holder.inventory = inventory;
        GuiItems.fill(inventory);
        MountKind active = coordinator.activeKind(player.getUniqueId());
        inventory.setItem(HORSE_SELECTION_SLOT, GuiItems.item(Material.SADDLE,
                Component.text("Zavolat koně", NamedTextColor.GOLD),
                Component.text(active == MountKind.HORSE ? "Právě aktivní" : "Pozemní společník",
                        active == MountKind.HORSE ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        inventory.setItem(DRAGON_SELECTION_SLOT, GuiItems.item(Material.DRAGON_HEAD,
                Component.text("Zavolat draka", NamedTextColor.LIGHT_PURPLE),
                Component.text(active == MountKind.DRAGON ? "Právě aktivní" : "Rychlý let",
                        active == MountKind.DRAGON ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
        inventory.setItem(BACK_SLOT, GuiItems.back("Zavřít"));
        player.openInventory(inventory);
    }

    public void call(Player player) {
        if (!prepare(player, true)) return;
        UUID ownerId = player.getUniqueId();
        Instant now = Instant.now();
        HappyGhast active = activeDragon(ownerId);
        if (active != null) {
            if (now.isBefore(recallAvailableAt.getOrDefault(ownerId, Instant.EPOCH))) {
                messages.sendActionBar(player, "dragon-recall-cooldown", Map.of());
                return;
            }
            if (!active.getPassengers().isEmpty()) {
                messages.send(player, "mount-being-ridden");
                return;
            }
            Optional<Location> destination = findSpawn(player);
            if (destination.isEmpty()) {
                messages.send(player, "dragon-no-space");
                return;
            }
            if (isRemote(active, player)) {
                active.teleport(destination.get());
                waitAt(ownerId, active, destination.get());
            } else {
                approach(ownerId, destination.get());
            }
            recallAvailableAt.put(ownerId, now.plusSeconds(config().activeRecallCooldownSeconds()));
            messages.sendActionBar(player, "dragon-called", Map.of());
            return;
        }
        if (now.isBefore(summonAvailableAt.getOrDefault(ownerId, Instant.EPOCH))) {
            messages.sendActionBar(player, "dragon-summon-cooldown", Map.of());
            return;
        }
        Optional<Location> spawn = findSpawn(player);
        if (spawn.isEmpty()) {
            messages.send(player, "dragon-no-space");
            return;
        }
        ActivationResult activation = coordinator.prepareActivation(ownerId, MountKind.DRAGON);
        if (activation == ActivationResult.HAS_PASSENGERS) {
            messages.send(player, "mount-being-ridden");
            return;
        }
        if (activation == ActivationResult.DEACTIVATION_FAILED) {
            messages.send(player, "mount-switch-failed");
            return;
        }

        HappyGhast dragon = player.getWorld().spawn(spawn.get(), HappyGhast.class,
                spawned -> apply(player, spawned));
        activeDragons.put(ownerId, dragon);
        if (!coordinator.claim(ownerId, MountKind.DRAGON, dragon,
                () -> deactivateForSwitch(ownerId, dragon))) {
            activeDragons.remove(ownerId, dragon);
            dragon.remove();
            messages.send(player, "mount-switch-failed");
            return;
        }
        EnderDragon model = spawnModel(ownerId, player.getName(), dragon);
        if (model == null) {
            removeDragon(ownerId, dragon);
            messages.send(player, "dragon-model-failed");
            return;
        }
        waitAt(ownerId, dragon, spawn.get());
        summonAvailableAt.put(ownerId, now.plusSeconds(config().summonCooldownSeconds()));
        recallAvailableAt.put(ownerId, now.plusSeconds(config().activeRecallCooldownSeconds()));
        player.playSound(spawn.get(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.65f, 1.15f);
        messages.sendActionBar(player, "dragon-called", Map.of());
    }

    public void dismiss(Player player) {
        if (!prepare(player, false)) return;
        HappyGhast dragon = activeDragon(player.getUniqueId());
        if (dragon == null) {
            messages.send(player, "dragon-not-active");
            return;
        }
        if (!dragon.getPassengers().isEmpty()) {
            messages.send(player, "mount-being-ridden");
            return;
        }
        removeDragon(player.getUniqueId(), dragon);
        messages.sendActionBar(player, "dragon-dismissed", Map.of());
    }

    private boolean prepare(Player player, boolean requireAllowedWorld) {
        if (!enabled) {
            messages.send(player, "module-disabled", Map.of("module", ID));
            return false;
        }
        if (!plugin.skillsModule().hasPowerMilestone(owner(player), PowerMilestoneId.DRAGON_BOND)) {
            messages.send(player, "dragon-milestone-locked", Map.of(
                    "level", PowerMilestoneId.DRAGON_BOND.requiredPowerLevel()));
            return false;
        }
        if (requireAllowedWorld && !config().isWorldAllowed(player.getWorld().getName())) {
            messages.send(player, "dragon-world-blocked");
            return false;
        }
        return true;
    }

    private UUID owner(Player player) {
        return player.getUniqueId();
    }

    private void apply(Player owner, HappyGhast dragon) {
        dragon.setAdult();
        dragon.setAgeLock(true);
        dragon.setPersistent(true);
        dragon.setRemoveWhenFarAway(false);
        dragon.setAI(false);
        dragon.setInvisible(true);
        dragon.setSilent(true);
        dragon.customName(Component.text("Drak — " + owner.getName(), NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD));
        dragon.setCustomNameVisible(false);
        AttributeInstance health = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(config().maxHealth());
            dragon.setHealth(config().maxHealth());
        }
        AttributeInstance speed = dragon.getAttribute(Attribute.FLYING_SPEED);
        if (speed != null) speed.setBaseValue(config().flyingSpeed());
        dragon.getEquipment().setItem(EquipmentSlot.BODY, new ItemStack(Material.BLACK_HARNESS));
        dragon.getPersistentDataContainer().set(managedKey, PersistentDataType.BYTE, (byte) 1);
        dragon.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                owner.getUniqueId().toString());
    }

    private void applyModel(UUID ownerId, String ownerName, EnderDragon model) {
        model.setAI(true);
        model.setSilent(true);
        model.setInvulnerable(true);
        model.setCollidable(false);
        model.setPersistent(true);
        model.setRemoveWhenFarAway(false);
        model.setPhase(EnderDragon.Phase.HOVER);
        model.customName(Component.text("Drak — " + ownerName, NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD));
        model.setCustomNameVisible(true);
        hideBossBar(model);
        model.getPersistentDataContainer().set(modelKey, PersistentDataType.BYTE, (byte) 1);
        model.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                ownerId.toString());
    }

    private EnderDragon spawnModel(UUID ownerId, String ownerName, HappyGhast carrier) {
        pendingModelOwners.add(ownerId);
        try {
            EnderDragon model = carrier.getWorld().spawn(carrier.getLocation(), EnderDragon.class,
                    spawned -> applyModel(ownerId, ownerName, spawned));
            dragonModels.put(ownerId, model);
            modelRespawnAvailableAt.remove(ownerId);
            synchronizeDragonModel(carrier, model);
            hideCarrierHarness(carrier);
            plugin.getLogger().info("Spawned dragon visual for " + ownerName
                    + " as EnderDragon " + model.getUniqueId() + ".");
            return model;
        } catch (RuntimeException exception) {
            modelRespawnAvailableAt.put(ownerId, Instant.now().plusSeconds(5));
            plugin.getLogger().warning("Could not spawn Ender Dragon visual for " + ownerName
                    + ": " + exception.getMessage());
            return null;
        } finally {
            pendingModelOwners.remove(ownerId);
        }
    }

    private Optional<Location> findSpawn(Player player) {
        Location origin = player.getLocation().getBlock().getLocation();
        int minimumDistance = Math.min(config().minimumSpawnDistance(), config().maximumSpawnDistance());
        int maximumDistance = Math.max(config().minimumSpawnDistance(), config().maximumSpawnDistance());
        int minimumHeight = Math.min(config().minimumSpawnHeight(), config().maximumSpawnHeight());
        int maximumHeight = Math.max(config().minimumSpawnHeight(), config().maximumSpawnHeight());
        for (int distance = minimumDistance; distance <= maximumDistance; distance++) {
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != distance) continue;
                    for (int y = minimumHeight; y <= maximumHeight; y++) {
                        Location candidate = origin.clone().add(x + 0.5, y, z + 0.5);
                        candidate.setYaw(player.getLocation().getYaw());
                        if (isClearForDragon(candidate)) return Optional.of(candidate);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isClearForDragon(Location center) {
        World world = center.getWorld();
        if (world == null || center.getBlockY() + 4 >= world.getMaxHeight()
                || center.getY() > config().maximumAltitude()) return false;
        return DragonFlightCollisionPolicy.canMove(
            flightPosition(center), flightPosition(center), config().maximumAltitude(), voxelAccess(world));
    }

    private HappyGhast activeDragon(UUID ownerId) {
        HappyGhast dragon = activeDragons.get(ownerId);
        if (dragon != null && dragon.isValid() && !dragon.isDead()) return dragon;
        if (dragon != null) {
            activeDragons.remove(ownerId, dragon);
            coordinator.release(ownerId, dragon);
        }
        return null;
    }

    private boolean deactivateForSwitch(UUID ownerId, HappyGhast dragon) {
        if (!dragon.getPassengers().isEmpty()) return false;
        removeDragon(ownerId, dragon);
        return true;
    }

    private void removeDragon(UUID ownerId, HappyGhast dragon) {
        activeDragons.remove(ownerId, dragon);
        waitingLocations.remove(ownerId);
        approachTargets.remove(ownerId);
        EnderDragon model = dragonModels.remove(ownerId);
        modelRespawnAvailableAt.remove(ownerId);
        if (model != null && model.isValid()) {
            hideBossBar(model);
            model.remove();
        }
        coordinator.release(ownerId, dragon);
        if (dragon.isValid()) {
            dragon.eject();
            dragon.remove();
        }
    }

    private boolean isManaged(HappyGhast dragon) {
        return dragon.getPersistentDataContainer().has(managedKey, PersistentDataType.BYTE);
    }

    private boolean isManagedModel(EnderDragon dragon) {
        return dragon.getPersistentDataContainer().has(modelKey, PersistentDataType.BYTE);
    }

    private UUID readOwner(Entity dragon) {
        String value = dragon.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isOwner(Player player, HappyGhast dragon) {
        return player.getUniqueId().equals(readOwner(dragon));
    }

    private DragonConfig config() {
        return plugin.configuration().get().dragons();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof HappyGhast dragon) || !isManaged(dragon)) return;
        if (!(event.getEntered() instanceof Player player) || !isOwner(player, dragon)
                || !dragon.getPassengers().isEmpty()) {
            event.setCancelled(true);
            if (event.getEntered() instanceof Player player) {
                messages.sendActionBar(player, "dragon-foreign", Map.of());
            }
            return;
        }
        waitingLocations.remove(player.getUniqueId());
        approachTargets.remove(player.getUniqueId());
        messages.sendActionBar(player, "dragon-mounted", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventSneakDismount(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof HappyGhast dragon) || !isManaged(dragon)
                || !(event.getExited() instanceof Player player) || !isOwner(player, dragon)) {
            return;
        }
        if (player.isSneaking() && !requestedDismounts.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof HappyGhast dragon) || !isManaged(dragon)
                || !(event.getExited() instanceof Player player) || !isOwner(player, dragon)) {
            return;
        }
        waitAt(player.getUniqueId(), dragon, dragon.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!(player.getVehicle() instanceof HappyGhast dragon) || !isManaged(dragon) || !isOwner(player, dragon)) {
            return;
        }
        event.setCancelled(true);
        UUID ownerId = player.getUniqueId();
        requestedDismounts.add(ownerId);
        boolean leftVehicle = player.leaveVehicle();
        if (!leftVehicle) requestedDismounts.remove(ownerId);
        Bukkit.getScheduler().runTask(plugin, () -> requestedDismounts.remove(ownerId));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof HappyGhast dragon) || !isManaged(dragon)) return;
        Player player = event.getPlayer();
        Material held = player.getInventory().getItem(event.getHand()).getType();
        if (!isOwner(player, dragon) || held == Material.SHEARS || held.name().endsWith("_HARNESS")) {
            event.setCancelled(true);
            if (!isOwner(player, dragon)) messages.sendActionBar(player, "dragon-foreign", Map.of());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamaged(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof EnderDragon model && isManagedModel(model)) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof HappyGhast dragon) || !isManaged(dragon)) return;
        if (event.getDamager() instanceof Player player && !isOwner(player, dragon)) {
            event.setCancelled(true);
            messages.sendActionBar(player, "dragon-foreign", Map.of());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onModelDamaged(EntityDamageEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && isManagedModel(dragon)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onModelExplode(EntityExplodeEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && isManagedModel(dragon)) {
            event.setCancelled(true);
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onModelChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof EnderDragon dragon && isManagedModel(dragon)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onModelChangePhase(EnderDragonChangePhaseEvent event) {
        if (isManagedModel(event.getEntity()) && event.getNewPhase() != EnderDragon.Phase.HOVER) {
            event.setNewPhase(EnderDragon.Phase.HOVER);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof EnderDragon model && isManagedModel(model)) {
            event.getDrops().clear();
            event.setDroppedExp(0);
            UUID ownerId = readOwner(model);
            HappyGhast carrier = ownerId == null ? null : activeDragon(ownerId);
            if (carrier != null) removeDragon(ownerId, carrier);
            return;
        }
        if (!(event.getEntity() instanceof HappyGhast dragon) || !isManaged(dragon)) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        UUID ownerId = readOwner(dragon);
        if (ownerId != null) {
            activeDragons.remove(ownerId, dragon);
            waitingLocations.remove(ownerId);
            approachTargets.remove(ownerId);
            EnderDragon model = dragonModels.remove(ownerId);
            if (model != null && model.isValid()) {
                hideBossBar(model);
                model.remove();
            }
            coordinator.release(ownerId, dragon);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        HappyGhast dragon = activeDragon(event.getPlayer().getUniqueId());
        if (dragon != null) removeDragon(event.getPlayer().getUniqueId(), dragon);
    }

    private void waitAt(UUID ownerId, HappyGhast dragon, Location location) {
        approachTargets.remove(ownerId);
        waitingLocations.put(ownerId, location.clone());
        dragon.setVelocity(new Vector());
    }

    private boolean isRemote(HappyGhast dragon, Player player) {
        if (!dragon.getWorld().equals(player.getWorld())) return true;
        return Math.max(
                Math.abs(dragon.getLocation().getChunk().getX() - player.getLocation().getChunk().getX()),
                Math.abs(dragon.getLocation().getChunk().getZ() - player.getLocation().getChunk().getZ())
        ) >= config().activeTeleportDistanceChunks();
    }

    private void approach(UUID ownerId, Location target) {
        waitingLocations.remove(ownerId);
        approachTargets.put(ownerId, target.clone());
    }

    private void synchronizeDragonModels() {
        boolean refreshHarnessVisibility = ++synchronizationTicks % 20 == 0;
        for (Map.Entry<UUID, HappyGhast> entry : new ArrayList<>(activeDragons.entrySet())) {
            UUID ownerId = entry.getKey();
            HappyGhast carrier = entry.getValue();
            Player rider = ownerRider(ownerId, carrier);
            if (rider != null) {
                controlDragon(rider, carrier);
            } else {
                approachTarget(ownerId, carrier);
            }
            EnderDragon model = activeModel(ownerId);
            if (model == null && !Instant.now().isBefore(
                    modelRespawnAvailableAt.getOrDefault(ownerId, Instant.EPOCH))) {
                Player owner = Bukkit.getPlayer(ownerId);
                if (owner != null) model = spawnModel(ownerId, owner.getName(), carrier);
            }
            if (model != null) {
                synchronizeDragonModel(carrier, model);
            }
            if (refreshHarnessVisibility) hideCarrierHarness(carrier);
        }
        for (Map.Entry<UUID, Location> entry : new ArrayList<>(waitingLocations.entrySet())) {
            UUID ownerId = entry.getKey();
            HappyGhast dragon = activeDragon(ownerId);
            if (dragon == null || !dragon.getPassengers().isEmpty()) {
                waitingLocations.remove(ownerId);
                continue;
            }
            Location target = entry.getValue();
            if (target.getWorld() == null || !target.getWorld().equals(dragon.getWorld())) {
                waitingLocations.remove(ownerId);
                continue;
            }
            if (dragon.getLocation().distanceSquared(target) > 0.04) {
                dragon.teleport(target);
            }
            dragon.setVelocity(new Vector());
        }
    }

    private EnderDragon activeModel(UUID ownerId) {
        EnderDragon model = dragonModels.get(ownerId);
        if (model != null && model.isValid() && !model.isDead()) return model;
        if (model != null) dragonModels.remove(ownerId, model);
        return null;
    }

    private void approachTarget(UUID ownerId, HappyGhast carrier) {
        Location target = approachTargets.get(ownerId);
        if (target == null) return;
        if (!target.getWorld().equals(carrier.getWorld())) {
            approachTargets.remove(ownerId);
            return;
        }
        Vector difference = target.toVector().subtract(carrier.getLocation().toVector());
        double distance = difference.length();
        if (distance <= APPROACH_ARRIVAL_RADIUS) {
            waitAt(ownerId, carrier, target);
            return;
        }
        double speed = config().flyingSpeed() * MANUAL_FLIGHT_SPEED_MULTIPLIER;
        Vector step = difference.normalize().multiply(Math.min(speed, distance));
        Location destination = carrier.getLocation().add(step);
        destination.setY(Math.min(destination.getY(), config().maximumAltitude()));
        destination.setYaw(yawFromMovement(step));
        destination.setPitch(0.0f);
        if (canFlyTo(carrier.getLocation(), destination)) {
            carrier.teleport(destination);
        } else {
            approachTargets.remove(ownerId);
        }
        carrier.setRotation(destination.getYaw(), 0.0f);
        carrier.setVelocity(new Vector());
    }

    private float yawFromMovement(Vector movement) {
        return (float) Math.toDegrees(Math.atan2(-movement.getX(), movement.getZ()));
    }

    private Player ownerRider(UUID ownerId, HappyGhast carrier) {
        for (Entity passenger : carrier.getPassengers()) {
            if (passenger instanceof Player player && player.getUniqueId().equals(ownerId)) return player;
        }
        return null;
    }

    private void controlDragon(Player rider, HappyGhast carrier) {
        Input input = rider.getCurrentInput();
        float yaw = rider.getLocation().getYaw();
        double yawRadians = Math.toRadians(yaw);
        Vector forward = new Vector(-Math.sin(yawRadians), 0.0, Math.cos(yawRadians));
        Vector right = new Vector(-forward.getZ(), 0.0, forward.getX());
        Vector velocity = new Vector();

        if (input.isForward()) velocity.add(forward);
        if (input.isBackward()) velocity.subtract(forward);
        if (input.isRight()) velocity.add(right);
        if (input.isLeft()) velocity.subtract(right);

        double horizontalSpeed = config().flyingSpeed() * MANUAL_FLIGHT_SPEED_MULTIPLIER;
        if (input.isSprint()) horizontalSpeed *= SPRINT_SPEED_MULTIPLIER;
        if (velocity.lengthSquared() > 0.0) velocity.normalize().multiply(horizontalSpeed);

        double verticalSpeed = config().flyingSpeed() * MANUAL_VERTICAL_SPEED_MULTIPLIER;
        if (input.isJump()) velocity.setY(verticalSpeed);
        else if (input.isSneak()) velocity.setY(-verticalSpeed);

        boolean moving = velocity.lengthSquared() > 0.0;
        if (moving) {
            Location destination = carrier.getLocation().add(velocity);
            destination.setYaw(yaw);
            destination.setPitch(0.0f);
            destination.setY(Math.min(destination.getY(), config().maximumAltitude()));
            if (canFlyTo(carrier.getLocation(), destination)) {
                carrier.teleport(destination);
            } else {
                moving = false;
            }
        }
        carrier.setRotation(yaw, 0.0f);
        carrier.setFallDistance(0.0f);
        carrier.setVelocity(new Vector());
        playDragonFlightSound(rider, carrier, moving);
    }

    private void playDragonFlightSound(Player rider, HappyGhast carrier, boolean moving) {
        Location soundLocation = carrier.getLocation();
        if (moving && synchronizationTicks % FLAP_SOUND_INTERVAL_TICKS == 0) {
            rider.playSound(soundLocation, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.55f, 1.1f);
        } else if (!moving && synchronizationTicks % AMBIENT_SOUND_INTERVAL_TICKS == 0) {
            rider.playSound(soundLocation, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.35f, 1.15f);
        }
    }

    private boolean canFlyTo(Location origin, Location destination) {
        World world = destination.getWorld();
        return world != null && world.equals(origin.getWorld())
            && DragonFlightCollisionPolicy.canMove(
                flightPosition(origin), flightPosition(destination), config().maximumAltitude(), voxelAccess(world));
    }

    private static DragonFlightCollisionPolicy.Position flightPosition(Location location) {
        return new DragonFlightCollisionPolicy.Position(location.getX(), location.getY(), location.getZ());
    }

    private static DragonFlightCollisionPolicy.VoxelAccess voxelAccess(World world) {
        return new DragonFlightCollisionPolicy.VoxelAccess() {
            @Override
            public boolean isPassable(int x, int y, int z) {
                return y >= world.getMinHeight() && y < world.getMaxHeight()
                    && world.getBlockAt(x, y, z).isPassable();
            }

            @Override
            public int highestBlockingY(int x, int z) {
                return world.getHighestBlockYAt(x, z);
            }
        };
    }

    private void synchronizeDragonModel(HappyGhast carrier, EnderDragon model) {
        Location target = carrier.getLocation().add(0.0, ENDER_DRAGON_VISUAL_Y_OFFSET, 0.0);
        target.setPitch(0.0f);
        target.setYaw(target.getYaw() + ENDER_DRAGON_YAW_OFFSET);
        Location current = model.getLocation();
        if (!target.getWorld().equals(model.getWorld())
                || current.distanceSquared(target) > 0.01
                || angularDistance(current.getYaw(), target.getYaw()) > 1.0f) {
            model.teleport(target);
        }
        model.setRotation(target.getYaw(), 0.0f);
        model.setVelocity(new Vector());
        if (model.getPhase() != EnderDragon.Phase.HOVER) model.setPhase(EnderDragon.Phase.HOVER);
        hideBossBar(model);
    }

    private float angularDistance(float first, float second) {
        float difference = Math.abs((first - second) % 360.0f);
        return Math.min(difference, 360.0f - difference);
    }

    private void hideCarrierHarness(HappyGhast carrier) {
        ItemStack hiddenHarness = new ItemStack(Material.AIR);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.getWorld().equals(carrier.getWorld())) {
                viewer.sendEquipmentChange(carrier, EquipmentSlot.BODY, hiddenHarness);
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof HappyGhast dragon && isManaged(dragon)) {
                UUID ownerId = readOwner(dragon);
                if (ownerId != null) removeDragon(ownerId, dragon);
            }
            if (entity instanceof EnderDragon dragon && isManagedModel(dragon)) {
                UUID ownerId = readOwner(dragon);
                if (ownerId != null) dragonModels.remove(ownerId, dragon);
                hideBossBar(dragon);
                dragon.remove();
            }
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof HappyGhast dragon && isManaged(dragon)) {
                UUID ownerId = readOwner(dragon);
                HappyGhast tracked = ownerId == null ? null : activeDragons.get(ownerId);
                if (tracked == null || !tracked.getUniqueId().equals(dragon.getUniqueId())) dragon.remove();
            }
            if (entity instanceof EnderDragon dragon && isManagedModel(dragon)) {
                hideBossBar(dragon);
                UUID ownerId = readOwner(dragon);
                EnderDragon tracked = ownerId == null ? null : dragonModels.get(ownerId);
                if ((tracked == null || !tracked.getUniqueId().equals(dragon.getUniqueId()))
                        && (ownerId == null || !pendingModelOwners.contains(ownerId))) {
                    dragon.remove();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof HappyGhast dragon) || !isManaged(dragon)
                || event.getTo().getY() <= config().maximumAltitude()) return;
        Location capped = event.getTo().clone();
        capped.setY(config().maximumAltitude());
        dragon.teleport(capped);
        dragon.setVelocity(dragon.getVelocity().setY(Math.min(0.0, dragon.getVelocity().getY())));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(event.getWhoClicked() instanceof Player player)
                || (!(holder instanceof DragonMenuHolder) && !(holder instanceof SelectionMenuHolder))) return;
        event.setCancelled(true);
        if (!player.getUniqueId().equals(holder instanceof DragonMenuHolder dragon
                ? dragon.ownerId : ((SelectionMenuHolder) holder).ownerId)) return;
        int slot = event.getRawSlot();
        if (holder instanceof DragonMenuHolder) {
            if (slot == CALL_SLOT) call(player);
            else if (slot == DISMISS_SLOT) dismiss(player);
            else if (slot == BACK_SLOT) plugin.openMainMenu(player);
        } else {
            player.closeInventory();
            if (slot == HORSE_SELECTION_SLOT) plugin.mountsModule().call(player);
            else if (slot == DRAGON_SELECTION_SLOT) call(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof DragonMenuHolder || holder instanceof SelectionMenuHolder) event.setCancelled(true);
    }

    private void removeStaleDragons() {
        for (World world : Bukkit.getWorlds()) {
            for (HappyGhast dragon : world.getEntitiesByClass(HappyGhast.class)) {
                if (isManaged(dragon)) dragon.remove();
            }
            for (EnderDragon dragon : world.getEntitiesByClass(EnderDragon.class)) {
                if (isManagedModel(dragon)) {
                    hideBossBar(dragon);
                    dragon.remove();
                }
            }
        }
    }

    private void closeMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof DragonMenuHolder || holder instanceof SelectionMenuHolder) player.closeInventory();
        }
    }

    private static void hideBossBar(EnderDragon dragon) {
        BossBar bossBar = dragon.getBossBar();
        if (bossBar != null) bossBar.setVisible(false);
    }

    private static final class DragonMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private DragonMenuHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class SelectionMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private SelectionMenuHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
