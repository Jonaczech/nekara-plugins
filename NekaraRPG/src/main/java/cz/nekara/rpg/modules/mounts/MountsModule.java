package cz.nekara.rpg.modules.mounts;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.MountConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.menu.GuiItems;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.skills.milestones.PowerMilestoneId;
import cz.nekara.rpg.mount.MountCooldown;
import cz.nekara.rpg.mount.MountOwnerId;
import cz.nekara.rpg.mount.MountRecord;
import cz.nekara.rpg.mount.MountRepository;
import cz.nekara.rpg.mount.SqliteMountRepository;
import cz.nekara.rpg.mount.YamlMountRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MountsModule implements NekaraModule, Listener {
    public static final String ID = "mounts";

    private static final int MENU_STATUS_SLOT = 4;
    private static final int MENU_RENAME_SLOT = 10;
    private static final int MENU_COLOR_SLOT = 12;
    private static final int MENU_EQUIPMENT_SLOT = 14;
    private static final int MENU_STORAGE_SLOT = 15;
    private static final int MENU_WHISTLE_SLOT = 16;
    private static final int MENU_BACK_SLOT = 18;
    private static final int MENU_CALL_SLOT = 21;
    private static final int MENU_DISMISS_SLOT = 23;
    private static final int EQUIPMENT_SADDLE_SLOT = 11;
    private static final int EQUIPMENT_CHEST_SLOT = 13;
    private static final int EQUIPMENT_ARMOR_SLOT = 15;
    private static final int EQUIPMENT_BACK_SLOT = 22;
    private static final int COLOR_BACK_SLOT = 22;
    private static final int CONFIRM_YES_SLOT = 11;
    private static final int CONFIRM_BACK_SLOT = 15;
    private static final Map<Integer, Horse.Color> COLOR_SLOTS = Map.of(
            10, Horse.Color.WHITE,
            11, Horse.Color.CREAMY,
            12, Horse.Color.CHESTNUT,
            13, Horse.Color.BROWN,
            14, Horse.Color.BLACK,
            15, Horse.Color.GRAY,
            16, Horse.Color.DARK_BROWN
    );

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final NamespacedKey mountIdKey;
    private final NamespacedKey ownerIdKey;
    private final NamespacedKey whistleKey;
    private final NamespacedKey whistleMountIdKey;
    private final NamespacedKey whistleOwnerIdKey;
    private final Map<UUID, Horse> activeMounts = new HashMap<>();
    private final Map<UUID, Location> callAnchors = new HashMap<>();
    private final Map<UUID, Location> wanderTargets = new HashMap<>();
    private final Map<UUID, Instant> nextWanderAt = new HashMap<>();
    private final Map<UUID, Instant> activeRecallAvailableAt = new HashMap<>();
    private final Map<UUID, Location> approachTargets = new HashMap<>();
    private final Map<UUID, Double> approachDistances = new HashMap<>();
    private final Map<UUID, Instant> approachProgressAt = new HashMap<>();
    private final Set<UUID> arrivedAtCall = new java.util.HashSet<>();
    private final Map<UUID, NamePrompt> namePrompts = new HashMap<>();
    private final Set<UUID> pendingWhistleReturns = new java.util.HashSet<>();

    private MountRepository repository;
    private BukkitTask autosaveTask;
    private BukkitTask guidanceTask;
    private String storageFailure;
    private boolean enabled;

    public MountsModule(NekaraRPGPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        mountIdKey = new NamespacedKey(plugin, "mount-id");
        ownerIdKey = new NamespacedKey(plugin, "mount-owner-id");
        whistleKey = new NamespacedKey(plugin, "mount-whistle");
        whistleMountIdKey = new NamespacedKey(plugin, "whistle-mount-id");
        whistleOwnerIdKey = new NamespacedKey(plugin, "whistle-owner-id");
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
        storageFailure = null;
        try {
            repository = openRepository();
        } catch (IOException | RuntimeException exception) {
            repository = null;
            storageFailure = exception.getMessage();
            plugin.getLogger().severe("NekaraMounts storage is unavailable; mount operations are locked: "
                    + exception.getMessage());
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
        if (repository != null) {
            reconcileLoadedMounts();
        }
        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::autosaveActiveMounts,
                config().autosavePeriodTicks(), config().autosavePeriodTicks());
        guidanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::guideActiveMounts, 10L, 10L);
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        cancelTask(autosaveTask);
        cancelTask(guidanceTask);
        autosaveTask = null;
        guidanceTask = null;
        closeModuleInventories();
        HandlerList.unregisterAll(this);
        for (Horse horse : new ArrayList<>(activeMounts.values())) {
            storeAndRemove(horse, "module shutdown");
        }
        activeMounts.clear();
        callAnchors.clear();
        wanderTargets.clear();
        nextWanderAt.clear();
        activeRecallAvailableAt.clear();
        approachTargets.clear();
        approachDistances.clear();
        approachProgressAt.clear();
        arrivedAtCall.clear();
        namePrompts.clear();
        closeRepository();
        repository = null;
        enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public int activeCount() {
        return (int) activeMounts.values().stream().filter(Entity::isValid).count();
    }

    public int registeredCount() {
        if (repository == null) {
            return 0;
        }
        try {
            return repository.findAll().size();
        } catch (RuntimeException exception) {
            failStorage(exception);
            return 0;
        }
    }

    public String storageStatus() {
        if (storageFailure != null) {
            return "uzamčeno";
        }
        return repository instanceof SqliteMountRepository ? "SQLite" : "nedostupné";
    }

    public Optional<MountOverview> overview(Player player) {
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        MountRecord record = found.get();
        Horse active = resolveActive(record);
        Instant now = Instant.now();
        String state = record.isDead() && MountCooldown.isActive(record.reviveAt(), now)
                ? "Odpočívá po pádu"
                : active != null ? "Nablízku"
                : record.activeEntityUuid() != null ? "Vzdálený"
                : "V bezpečí";
        long cooldown = record.isDead() && MountCooldown.isActive(record.reviveAt(), now)
                ? MountCooldown.remainingSeconds(record.reviveAt(), now)
                : MountCooldown.remainingSeconds(record.summonAvailableAt(), now);
        return Optional.of(new MountOverview(record.customName(), state, record.health(), record.maxHealth(),
                occupiedStorageSlots(record), record.hasChest(), cooldown));
    }

    private MountRepository openRepository() throws IOException {
        File databaseFile = new File(plugin.getDataFolder(), config().databaseFile());
        SqliteMountRepository sqlite = new SqliteMountRepository(databaseFile);
        try {
            File legacyFile = new File(plugin.getDataFolder(), config().storageFile());
            if (!sqlite.isLegacyMigrationComplete() && sqlite.isEmpty() && legacyFile.isFile()) {
                YamlMountRepository legacy = new YamlMountRepository(legacyFile);
                File backup = new File(legacyFile.getParentFile(), legacyFile.getName() + ".pre-sqlite.bak");
                if (!backup.exists()) {
                    Files.copy(legacyFile.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                }
                sqlite.importAll(legacy.findAll(), legacy.combatWindows());
                plugin.getLogger().info("Migrated " + legacy.findAll().size()
                        + " NekaraMounts records from YAML to SQLite; legacy data and backup were retained.");
            } else if (!sqlite.isLegacyMigrationComplete()) {
                sqlite.markLegacyMigrationComplete();
            }
            return sqlite;
        } catch (IOException | RuntimeException exception) {
            sqlite.close();
            throw exception;
        }
    }

    private void closeRepository() {
        if (repository instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception exception) {
                plugin.getLogger().severe("Could not close NekaraMounts storage: " + exception.getMessage());
            }
        }
    }

    public void openMenu(Player player) {
        if (!hasMountMilestone(player) || !prepareOperation(player, true)) {
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            openColorMenu(player, true);
            return;
        }
        MountRecord record = found.get();
        MountMenuHolder holder = new MountMenuHolder(record.ownerId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("NekaraMounts — " + record.customName(), NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(MENU_RENAME_SLOT, item(Material.NAME_TAG,
                Component.text("Přejmenovat koně", NamedTextColor.GOLD),
                Component.text(record.customName(), NamedTextColor.GRAY)));
        inventory.setItem(MENU_COLOR_SLOT, colorItem(record.color(), "Změnit barvu"));
        inventory.setItem(MENU_EQUIPMENT_SLOT, item(Material.SADDLE,
                Component.text("Sedlo a brnění", NamedTextColor.YELLOW),
                Component.text("Sedlo, truhla a koňské brnění", NamedTextColor.GRAY)));
        inventory.setItem(MENU_STORAGE_SLOT, item(Material.CHEST,
                Component.text("Brašny", NamedTextColor.GOLD),
                Component.text(record.hasChest()
                        ? "Uloženo " + occupiedStorageSlots(record) + "/" + MountRecord.STORAGE_SIZE
                        : "Nejdřív koni nasaď truhlu", NamedTextColor.GRAY),
                Component.text(record.hasChest()
                        ? "Otevře velký inventář tvého koně"
                        : "Truhlu vložíš do správy výbavy", NamedTextColor.DARK_GRAY)));
        boolean hasWhistle = hasAnyWhistle(player);
        inventory.setItem(MENU_WHISTLE_SLOT, item(config().whistleMaterial(),
                Component.text(hasWhistle ? "Odebrat píšťalku" : "Obnovit píšťalku", NamedTextColor.AQUA),
                Component.text(hasWhistle
                        ? "Odloží ji, dokud si ji znovu nevyžádáš"
                        : "Přivolá píšťalku spojenou s tvým koněm", NamedTextColor.GRAY)));
        inventory.setItem(MENU_CALL_SLOT, item(Material.LIME_DYE,
                Component.text("Přivolat", NamedTextColor.GREEN)));
        inventory.setItem(MENU_DISMISS_SLOT, item(Material.GRAY_DYE,
                Component.text("Odvolat", NamedTextColor.GRAY)));
        inventory.setItem(MENU_BACK_SLOT, backItem("Zpět do NekaraRPG"));
        player.openInventory(inventory);
    }

    public boolean grant(Player target) {
        if (!prepareOperation(target, true)) {
            return false;
        }
        if (findOwned(target).isPresent()) {
            messages.send(target, "mount-already-owned");
            return false;
        }
        openColorMenu(target, true);
        messages.send(target, "mount-grant-started");
        return true;
    }

    public void call(Player player) {
        if (!hasMountMilestone(player) || !prepareOperation(player, true)) {
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord record = found.get();
        Instant now = Instant.now();
        Location callLocation = player.getLocation().clone();

        if (record.activeEntityUuid() != null) {
            Instant activeRecallAt = activeRecallAvailableAt.get(record.mountId());
            if (MountCooldown.isActive(activeRecallAt, now)) {
                actionBar(player, "mount-recall-cooldown", Map.of("time", MountCooldown.format(
                        MountCooldown.remainingSeconds(activeRecallAt, now))));
                return;
            }
            Horse active = resolveActive(record);
            if (active == null) {
                messages.send(player, "mount-active-unloaded");
                return;
            }
            if (!active.getWorld().equals(player.getWorld())) {
                messages.send(player, "mount-other-world");
                return;
            }
            if (!active.getPassengers().isEmpty()) {
                messages.send(player, "mount-being-ridden");
                return;
            }
            MountRecord updated = capture(record, active, active.getUniqueId(), now);
            try {
                repository.update(updated);
                directToCall(updated.mountId(), active, callLocation);
                actionBar(player, "mount-called", Map.of("name", updated.customName()));
            } catch (IOException exception) {
                failStorage(exception);
                messages.send(player, "mount-storage-error");
            }
            return;
        }

        if (MountCooldown.isActive(record.summonAvailableAt(), now)) {
            actionBar(player, "mount-summon-cooldown", Map.of("time", MountCooldown.format(
                    MountCooldown.remainingSeconds(record.summonAvailableAt(), now))));
            return;
        }
        if (record.isDead() && MountCooldown.isActive(record.reviveAt(), now)) {
            actionBar(player, "mount-dead", Map.of("time", MountCooldown.format(
                    MountCooldown.remainingSeconds(record.reviveAt(), now))));
            return;
        }
        if (record.isDead()) {
            record = record.revived(now);
        }
        Instant nextCall = now.plusSeconds(config().summonCooldownSeconds());

        Optional<Location> spawnLocation = findSafeSpawn(player);
        if (spawnLocation.isEmpty()) {
            messages.send(player, "mount-no-space");
            return;
        }
        MountRecord restored = record;
        Horse horse = player.getWorld().spawn(spawnLocation.get(), Horse.class,
                spawned -> apply(restored, player, spawned));
        MountRecord activeRecord = capture(record, horse, horse.getUniqueId(), now)
                .withSummonAvailableAt(nextCall, now);
        try {
            repository.update(activeRecord);
            activeMounts.put(record.mountId(), horse);
            directToCall(record.mountId(), horse, callLocation);
            actionBar(player, "mount-called", Map.of("name", activeRecord.customName()));
        } catch (IOException exception) {
            horse.remove();
            failStorage(exception);
            messages.send(player, "mount-storage-error");
        }
    }

    public void dismiss(Player player) {
        if (!hasMountMilestone(player) || !prepareOperation(player, false)) {
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord record = found.get();
        if (record.activeEntityUuid() == null) {
            messages.send(player, "mount-not-active");
            return;
        }
        Horse horse = resolveActive(record);
        if (horse == null) {
            messages.send(player, "mount-active-unloaded");
            return;
        }
        if (storeAndRemove(horse, "player dismissal")) {
            actionBar(player, "mount-dismissed", Map.of("name", record.customName()));
        } else {
            messages.send(player, "mount-storage-error");
        }
    }

    public void sendStatus(Player player) {
        if (!hasMountMilestone(player)) {
            return;
        }
        if (!enabled) {
            messages.send(player, "module-disabled", Map.of("module", ID));
            return;
        }
        if (repository == null || storageFailure != null) {
            messages.send(player, "mount-storage-error");
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord record = found.get();
        Instant now = Instant.now();
        String state;
        if (record.isDead() && MountCooldown.isActive(record.reviveAt(), now)) {
            state = "mrtvý, návrat za " + MountCooldown.format(
                    MountCooldown.remainingSeconds(record.reviveAt(), now));
        } else if (record.activeEntityUuid() != null) {
            state = resolveActive(record) == null ? "aktivní v nenačteném chunku" : "aktivní";
        } else {
            state = "odvolaný";
        }
        messages.send(player, "mount-status", Map.of(
                "name", record.customName(),
                "state", state,
                "health", String.format(Locale.ROOT, "%.1f/%.1f", record.health(), record.maxHealth()),
                "speed", String.format(Locale.ROOT, "%.3f", record.movementSpeed()),
                "jump", String.format(Locale.ROOT, "%.3f", record.jumpStrength()),
                "color", record.color().name().toLowerCase(Locale.ROOT),
                "style", record.style().name().toLowerCase(Locale.ROOT)
        ));
    }

    public void restoreWhistle(Player player) {
        if (!hasMountMilestone(player) || !prepareOperation(player, false)) {
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord record = found.get();
        removeAllWhistles(player);
        if (giveWhistle(player, record)) {
            messages.send(player, "mount-whistle-restored");
        } else {
            messages.send(player, "mount-whistle-no-space");
        }
    }

    public void removeWhistle(Player player) {
        if (!hasMountMilestone(player) || !prepareOperation(player, false)) {
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        int removed = removeAllWhistles(player);
        messages.send(player, removed > 0 ? "mount-whistle-removed" : "mount-whistle-not-found");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWhistle(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getItem() == null
                || (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK)
                || !isWhistle(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (plugin.authModule().isEnabled() && !plugin.authModule().isAuthenticated(player)) {
            return;
        }
        if (!isOwnedWhistle(player, event.getItem())) {
            actionBar(player, "mount-whistle-foreign", Map.of());
            return;
        }
        call(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWhistleDrop(PlayerDropItemEvent event) {
        if (!isWhistle(event.getItemDrop().getItemStack())) {
            return;
        }
        event.setCancelled(true);
        actionBar(event.getPlayer(), "mount-whistle-bound", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWhistleInventoryMove(InventoryMoveItemEvent event) {
        if (isWhistle(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWhistleHopperPickup(InventoryPickupItemEvent event) {
        if (isWhistle(event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWhistleDispense(BlockDispenseEvent event) {
        if (isWhistle(event.getItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWhistlePickup(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (!isWhistle(item)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || !isOwnedWhistle(player, item)) {
            event.setCancelled(true);
            return;
        }
        MountRecord record = findOwned(player).orElse(null);
        if (record != null && hasBoundWhistle(player, record)) {
            event.setCancelled(true);
            event.getItem().remove();
            actionBar(player, "mount-whistle-duplicate-removed", Map.of());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        boolean removed = event.getDrops().removeIf(this::isWhistle);
        if (removed && findOwned(event.getPlayer()).isPresent()) {
            pendingWhistleReturns.add(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!pendingWhistleReturns.remove(event.getPlayer().getUniqueId())) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> returnWhistleAfterDeath(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCombat(EntityDamageByEntityEvent event) {
        if (!enabled || repository == null || storageFailure != null
                || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        persistCombat(attacker, victim, Instant.now().plusSeconds(config().combatTagSeconds()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMountDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !isManaged(horse)) {
            return;
        }
        Player attacker = resolvePlayerDamager(event.getDamager());
        if (attacker != null && !isOwner(attacker, horse)) {
            event.setCancelled(true);
            actionBar(attacker, "mount-foreign", Map.of());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Horse horse) || !isManaged(horse)) {
            return;
        }
        Player player = event.getPlayer();
        if (!isOwner(player, horse)) {
            event.setCancelled(true);
            actionBar(player, "mount-foreign", Map.of());
            return;
        }
        if (player.getInventory().getItem(event.getHand()).getType() == Material.NAME_TAG) {
            event.setCancelled(true);
            actionBar(player, "mount-use-gui-name", Map.of());
            return;
        }
        if (player.isSneaking()) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> openEquipmentMenu(player));
            return;
        }
        scheduleSave(horse);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Horse horse) || !(event.getEntered() instanceof Player player)
                || !isManaged(horse)) {
            return;
        }
        if (!isOwner(player, horse)) {
            event.setCancelled(true);
            actionBar(player, "mount-foreign", Map.of());
            return;
        }
        clearGuidance(readMountId(horse));
        horse.setAware(true);
        horse.getPathfinder().stopPathfinding();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof Horse horse) || !(event.getExited() instanceof Player player)
                || !isManaged(horse) || !isOwner(player, horse)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (horse.isValid() && horse.getPassengers().isEmpty()) {
                UUID mountId = readMountId(horse);
                if (mountId != null) {
                    callAnchors.put(mountId, horse.getLocation().clone());
                    arrivedAtCall.add(mountId);
                    scheduleNextWander(mountId, Instant.now());
                    horse.getPathfinder().stopPathfinding();
                    horse.setAware(true);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        Horse horse = managedHorse(event.getInventory());
        if (horse == null || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        if (!isOwner(player, horse)) {
            actionBar(player, "mount-foreign", Map.of());
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> openEquipmentMenu(player));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (shouldBlockWhistleClick(event)) {
            event.setCancelled(true);
            actionBar(player, "mount-whistle-bound", Map.of());
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof MountMenuHolder mountMenu) {
            event.setCancelled(true);
            if (!ownsHolder(player, mountMenu.ownerId)) {
                player.closeInventory();
                return;
            }
            handleMenuClick(player, event.getRawSlot());
            return;
        }
        if (holder instanceof ColorMenuHolder colorMenu) {
            event.setCancelled(true);
            handleColorClick(player, colorMenu, event.getRawSlot());
            return;
        }
        if (holder instanceof EquipmentMenuHolder equipmentMenu) {
            if (!ownsHolder(player, equipmentMenu.ownerId)) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
            int topSize = event.getView().getTopInventory().getSize();
            if (event.getRawSlot() >= topSize) {
                if (tryEquipFromPlayerInventory(player, event) || event.isShiftClick()
                        || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                        || event.getHotbarButton() >= 0) {
                    event.setCancelled(true);
                }
                return;
            }
            event.setCancelled(true);
            handleEquipmentClick(player, event.getRawSlot());
            return;
        }
        if (holder instanceof StorageMenuHolder storageMenu) {
            if (!ownsHolder(player, storageMenu.ownerId)) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
            handleStorageClick(player, event);
            return;
        }
        if (holder instanceof ConfirmationMenuHolder confirmation) {
            event.setCancelled(true);
            handleConfirmationClick(player, confirmation, event.getRawSlot());
            return;
        }
        NamePrompt prompt = namePrompts.get(player.getUniqueId());
        if (prompt != null && event.getView() instanceof AnvilView anvilView) {
            event.setCancelled(true);
            if (event.getRawSlot() == 2) {
                submitName(player, prompt, anvilView);
            }
            return;
        }
        Horse horse = managedHorse(event.getView().getTopInventory());
        if (horse != null) {
            event.setCancelled(true);
            actionBar(player, "mount-use-gui-equipment", Map.of());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (isWhistle(event.getOldCursor())
                && (event.getView().getTopInventory().getType() != InventoryType.CRAFTING
                || event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize()))) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                actionBar(player, "mount-whistle-bound", Map.of());
            }
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof MountMenuHolder || holder instanceof ColorMenuHolder
                || (holder instanceof EquipmentMenuHolder
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize()))
                || holder instanceof ConfirmationMenuHolder
                || (holder instanceof StorageMenuHolder
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize()))
                || managedHorse(event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)
                || !namePrompts.containsKey(player.getUniqueId())) {
            return;
        }
        event.getView().setRepairCost(0);
        event.getView().setMaximumRepairCost(0);
        event.getView().setBypassCost(true);
        event.setResult(item(Material.LIME_DYE,
                Component.text("Potvrdit jméno", NamedTextColor.GREEN)));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !(event.getView() instanceof AnvilView)) {
            return;
        }
        NamePrompt closedPrompt = namePrompts.remove(player.getUniqueId());
        if (closedPrompt != null) {
            event.getInventory().clear();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline() || !enabled) {
                    return;
                }
                if (closedPrompt.mode == NamePromptMode.CREATE) {
                    openColorMenu(player, true);
                } else {
                    openMenu(player);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Horse horse) || !isManaged(horse)) {
            return;
        }
        // Managed equipment must never become loot, even if persistence is already locked.
        event.getDrops().clear();
        event.setDroppedExp(0);
        if (repository == null || storageFailure != null) {
            return;
        }
        UUID mountId = readMountId(horse);
        Optional<MountRecord> found = findByMountId(mountId);
        if (found.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        MountRecord current = capture(found.get(), horse, null, now)
                .killed(now, now.plusSeconds(config().deathCooldownSeconds()));
        try {
            repository.update(current);
            activeMounts.remove(current.mountId());
            clearGuidance(current.mountId());
        } catch (IOException exception) {
            failStorage(exception);
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        if (!enabled || repository == null || storageFailure != null) {
            return;
        }
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Horse horse && isManaged(horse)) {
                reconcile(horse);
            }
        }
    }

    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        if (!enabled) {
            return;
        }
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Horse horse && isManaged(horse)) {
                storeAndRemove(horse, "chunk unload");
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        namePrompts.remove(event.getPlayer().getUniqueId());
        if (!enabled || repository == null || storageFailure != null || !config().recallOnQuit()) {
            return;
        }
        String ownerId = MountOwnerId.fromPlayerName(event.getPlayer().getName());
        if (isInCombat(ownerId, Instant.now())) {
            return;
        }
        findByOwnerId(ownerId).map(this::resolveActive)
                .ifPresent(horse -> storeAndRemove(horse, "owner disconnect"));
    }

    private void handleMenuClick(Player player, int slot) {
        switch (slot) {
            case MENU_RENAME_SLOT -> findOwned(player).ifPresent(record ->
                    openNamePrompt(player, new NamePrompt(NamePromptMode.RENAME, record.color()),
                            record.customName()));
            case MENU_COLOR_SLOT -> openColorMenu(player, false);
            case MENU_EQUIPMENT_SLOT -> openEquipmentMenu(player);
            case MENU_STORAGE_SLOT -> openStorageOrEquipment(player);
            case MENU_WHISTLE_SLOT -> {
                if (hasAnyWhistle(player)) {
                    openConfirmation(player, ConfirmationAction.REMOVE_WHISTLE);
                } else {
                    toggleWhistle(player);
                    openMenu(player);
                }
            }
            case MENU_CALL_SLOT -> call(player);
            case MENU_DISMISS_SLOT -> openConfirmation(player, ConfirmationAction.DISMISS);
            case MENU_BACK_SLOT -> plugin.openMainMenu(player);
            default -> {
            }
        }
    }

    private void openColorMenu(Player player, boolean creation) {
        ColorMenuHolder holder = new ColorMenuHolder(creation);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text(creation ? "Vyber barvu svého koně" : "Změna barvy koně",
                        NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        fill(inventory);
        for (Map.Entry<Integer, Horse.Color> entry : COLOR_SLOTS.entrySet()) {
            inventory.setItem(entry.getKey(), colorItem(entry.getValue(), colorDisplay(entry.getValue())));
        }
        inventory.setItem(COLOR_BACK_SLOT, backItem(creation ? "Zpět" : "Zpět ke koni"));
        player.openInventory(inventory);
    }

    private void handleColorClick(Player player, ColorMenuHolder holder, int slot) {
        Horse.Color color = COLOR_SLOTS.get(slot);
        if (color == null) {
            if (slot == COLOR_BACK_SLOT) {
                if (holder.creation) {
                    plugin.openMainMenu(player);
                } else {
                    openMenu(player);
                }
            }
            return;
        }
        if (holder.creation) {
            openNamePrompt(player, new NamePrompt(NamePromptMode.CREATE, color), "");
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord updated = found.get().withIdentity(found.get().customName(), color, Instant.now());
        if (updateRecord(updated)) {
            Horse active = resolveActive(updated);
            if (active != null) {
                active.setColor(color);
            }
            openMenu(player);
        }
    }

    private void openNamePrompt(Player player, NamePrompt prompt, String currentName) {
        namePrompts.put(player.getUniqueId(), prompt);
        InventoryView opened = player.openAnvil(null, true);
        if (!(opened instanceof AnvilView anvilView)) {
            namePrompts.remove(player.getUniqueId());
            messages.send(player, "mount-gui-error");
            return;
        }
        AnvilInventory inventory = anvilView.getTopInventory();
        inventory.setFirstItem(item(Material.NAME_TAG,
                Component.text(currentName == null || currentName.isBlank() ? "Jméno koně" : currentName)));
        anvilView.setRepairCost(0);
        anvilView.setMaximumRepairCost(0);
        anvilView.setBypassCost(true);
    }

    private void submitName(Player player, NamePrompt prompt, AnvilView anvilView) {
        String name = sanitizeName(anvilView.getRenameText());
        if (!isValidName(name)) {
            actionBar(player, "mount-name-invalid", Map.of(
                    "min", config().minimumNameLength(), "max", config().maximumNameLength()));
            return;
        }
        namePrompts.remove(player.getUniqueId());
        anvilView.getTopInventory().clear();
        player.closeInventory();
        if (prompt.mode == NamePromptMode.CREATE) {
            createVirtualMount(player, prompt.color, name);
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord updated = found.get().withIdentity(name, found.get().color(), Instant.now());
        if (updateRecord(updated)) {
            Horse active = resolveActive(updated);
            if (active != null) {
                applyBoldName(active, name);
            }
            actionBar(player, "mount-name-updated", Map.of("name", name));
            openMenu(player);
        }
    }

    private void createVirtualMount(Player player, Horse.Color color, String name) {
        if (!prepareOperation(player, true)) {
            return;
        }
        String ownerId = MountOwnerId.fromPlayerName(player.getName());
        if (findByOwnerId(ownerId).isPresent()) {
            messages.send(player, "mount-already-owned");
            return;
        }
        Instant now = Instant.now();
        MountRecord record = new MountRecord(
                ownerId, player.getName(), player.getUniqueId(), UUID.randomUUID(), null,
                name, config().defaultMaxHealth(), config().defaultMaxHealth(),
                config().defaultMovementSpeed(), config().defaultJumpStrength(), color,
                Horse.Style.NONE, new ItemStack(Material.SADDLE), null, null, List.of(),
                0, 0, 300, List.of(),
                null, null, null, now);
        try {
            if (!repository.create(record)) {
                messages.send(player, "mount-already-owned");
                return;
            }
            removeAllWhistles(player);
            if (!giveWhistle(player, record)) {
                messages.send(player, "mount-whistle-no-space");
            }
            actionBar(player, "mount-created", Map.of("name", name));
            openMenu(player);
        } catch (IOException exception) {
            failStorage(exception);
            messages.send(player, "mount-storage-error");
        }
    }

    private void openEquipmentMenu(Player player) {
        if (!prepareOperation(player, false)) {
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord record = found.get();
        EquipmentMenuHolder holder = new EquipmentMenuHolder(record.ownerId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("Výbava — " + record.customName(), NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(EQUIPMENT_SADDLE_SLOT, equipmentDisplay(record.saddle(), EquipmentKind.SADDLE));
        inventory.setItem(EQUIPMENT_CHEST_SLOT, equipmentDisplay(record.chest(), EquipmentKind.CHEST));
        inventory.setItem(EQUIPMENT_ARMOR_SLOT, equipmentDisplay(record.armor(), EquipmentKind.ARMOR));
        inventory.setItem(EQUIPMENT_BACK_SLOT, backItem("Zpět ke koni"));
        player.openInventory(inventory);
    }

    private void handleEquipmentClick(Player player, int slot) {
        if (slot == EQUIPMENT_BACK_SLOT) {
            openMenu(player);
            return;
        }
        if (slot != EQUIPMENT_SADDLE_SLOT && slot != EQUIPMENT_CHEST_SLOT
                && slot != EQUIPMENT_ARMOR_SLOT) {
            return;
        }
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            player.closeInventory();
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord record = found.get();
        ItemStack cursor = emptyToNull(player.getItemOnCursor());
        if (cursor != null && cursor.getAmount() != 1) {
            actionBar(player, "mount-equipment-single-item", Map.of());
            return;
        }
        EquipmentKind kind = slot == EQUIPMENT_SADDLE_SLOT ? EquipmentKind.SADDLE
                : slot == EQUIPMENT_CHEST_SLOT ? EquipmentKind.CHEST : EquipmentKind.ARMOR;
        if (cursor != null && !isValidEquipment(cursor, kind)) {
            actionBar(player, kind == EquipmentKind.SADDLE ? "mount-equipment-saddle-only"
                    : kind == EquipmentKind.CHEST ? "mount-equipment-chest-only"
                    : "mount-equipment-armor-only", Map.of());
            return;
        }
        ItemStack previous = kind == EquipmentKind.SADDLE ? record.saddle()
                : kind == EquipmentKind.CHEST ? record.chest() : record.armor();
        if (sameItem(previous, cursor)) {
            return;
        }
        if (kind == EquipmentKind.CHEST && cursor == null && hasStoredItems(record)) {
            actionBar(player, "mount-storage-not-empty", Map.of());
            return;
        }
        ItemStack newSaddle = kind == EquipmentKind.SADDLE ? cursor : record.saddle();
        ItemStack newArmor = kind == EquipmentKind.ARMOR ? cursor : record.armor();
        ItemStack newChest = kind == EquipmentKind.CHEST ? cursor : record.chest();
        MountRecord updated = record.withEquipment(newSaddle, newArmor, newChest, Instant.now());
        if (!updateRecord(updated)) {
            return;
        }
        player.setItemOnCursor(previous == null ? new ItemStack(Material.AIR) : previous);
        Horse active = resolveActive(updated);
        if (active != null) {
            active.getInventory().setSaddle(updated.saddle());
            active.getInventory().setArmor(updated.armor());
        }
        player.getOpenInventory().setItem(slot,
                equipmentDisplay(kind == EquipmentKind.SADDLE ? updated.saddle()
                        : kind == EquipmentKind.CHEST ? updated.chest() : updated.armor(), kind));
    }

    private boolean tryEquipFromPlayerInventory(Player player, InventoryClickEvent event) {
        ItemStack source = emptyToNull(event.getCurrentItem());
        EquipmentKind kind = equipmentKind(source);
        if (kind == null || !isEmpty(event.getCursor()) || event.isShiftClick()
                || event.getHotbarButton() >= 0
                || (event.getAction() != InventoryAction.PICKUP_ALL
                && event.getAction() != InventoryAction.PICKUP_HALF)) {
            return false;
        }
        event.setCancelled(true);
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            player.closeInventory();
            messages.send(player, "mount-not-owned");
            return true;
        }
        MountRecord record = found.get();
        ItemStack previous = equipment(record, kind);
        ItemStack equipped = source.clone();
        equipped.setAmount(1);
        if (sameItem(previous, equipped)) {
            return true;
        }
        MountRecord updated = record.withEquipment(
                kind == EquipmentKind.SADDLE ? equipped : record.saddle(),
                kind == EquipmentKind.ARMOR ? equipped : record.armor(),
                kind == EquipmentKind.CHEST ? equipped : record.chest(),
                Instant.now());
        if (!updateRecord(updated)) {
            return true;
        }

        ItemStack remaining = source.clone();
        remaining.setAmount(source.getAmount() - 1);
        event.setCurrentItem(remaining.getAmount() == 0 ? new ItemStack(Material.AIR) : remaining);
        player.setItemOnCursor(previous == null ? new ItemStack(Material.AIR) : previous);
        applyEquipmentToActive(updated);
        player.getOpenInventory().setItem(equipmentSlot(kind), equipmentDisplay(equipped, kind));
        return true;
    }

    private EquipmentKind equipmentKind(ItemStack item) {
        if (item == null) return null;
        for (EquipmentKind kind : EquipmentKind.values()) {
            if (isValidEquipment(item, kind)) return kind;
        }
        return null;
    }

    private ItemStack equipment(MountRecord record, EquipmentKind kind) {
        return switch (kind) {
            case SADDLE -> record.saddle();
            case CHEST -> record.chest();
            case ARMOR -> record.armor();
        };
    }

    private int equipmentSlot(EquipmentKind kind) {
        return switch (kind) {
            case SADDLE -> EQUIPMENT_SADDLE_SLOT;
            case CHEST -> EQUIPMENT_CHEST_SLOT;
            case ARMOR -> EQUIPMENT_ARMOR_SLOT;
        };
    }

    private void applyEquipmentToActive(MountRecord record) {
        Horse active = resolveActive(record);
        if (active != null) {
            active.getInventory().setSaddle(record.saddle());
            active.getInventory().setArmor(record.armor());
        }
    }

    private void openStorageOrEquipment(Player player) {
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            actionBar(player, "mount-not-owned", Map.of());
            return;
        }
        if (!found.get().hasChest()) {
            openEquipmentMenu(player);
            return;
        }
        openStorage(player, found.get());
    }

    private void openStorage(Player player, MountRecord record) {
        StorageMenuHolder holder = new StorageMenuHolder(record.ownerId());
        Inventory inventory = Bukkit.createInventory(holder, MountRecord.STORAGE_SIZE,
                Component.text("Brašny — " + record.customName(), NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        List<ItemStack> storage = record.storage();
        for (int slot = 0; slot < storage.size(); slot++) {
            inventory.setItem(slot, storage.get(slot));
        }
        player.openInventory(inventory);
    }

    private void handleStorageClick(Player player, InventoryClickEvent event) {
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlot() < 0) {
            event.setCancelled(true);
            return;
        }
        if (event.getRawSlot() >= topSize) {
            if (event.isShiftClick() || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                    || event.getHotbarButton() >= 0) {
                event.setCancelled(true);
            }
            return;
        }
        event.setCancelled(true);
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty() || !found.get().hasChest()) {
            player.closeInventory();
            actionBar(player, "mount-storage-closed", Map.of());
            return;
        }
        ItemStack current = emptyToNull(event.getCurrentItem());
        ItemStack cursor = emptyToNull(event.getCursor());
        if (isWhistle(current) || isWhistle(cursor)) {
            actionBar(player, "mount-whistle-bound", Map.of());
            return;
        }

        StorageChange change = event.isRightClick()
                ? rightClickStorage(current, cursor)
                : event.isLeftClick() ? new StorageChange(cursor, current) : null;
        if (change == null || sameItem(current, change.slotItem()) && sameItem(cursor, change.cursorItem())) {
            return;
        }
        List<ItemStack> updatedStorage = new ArrayList<>(found.get().storage());
        updatedStorage.set(event.getRawSlot(), change.slotItem());
        MountRecord updated = found.get().withStorage(updatedStorage, Instant.now());
        if (!updateRecord(updated)) {
            return;
        }
        event.getView().setItem(event.getRawSlot(), change.slotItem());
        player.setItemOnCursor(change.cursorItem() == null
                ? new ItemStack(Material.AIR) : change.cursorItem());
    }

    private StorageChange rightClickStorage(ItemStack current, ItemStack cursor) {
        if (cursor == null && current != null) {
            int takenAmount = (current.getAmount() + 1) / 2;
            ItemStack taken = current.clone();
            taken.setAmount(takenAmount);
            ItemStack remaining = current.clone();
            remaining.setAmount(current.getAmount() - takenAmount);
            return new StorageChange(remaining.getAmount() == 0 ? null : remaining, taken);
        }
        if (cursor != null && current == null) {
            ItemStack placed = cursor.clone();
            placed.setAmount(1);
            ItemStack remaining = cursor.clone();
            remaining.setAmount(cursor.getAmount() - 1);
            return new StorageChange(placed, remaining.getAmount() == 0 ? null : remaining);
        }
        if (cursor != null && current != null && current.isSimilar(cursor)
                && current.getAmount() < current.getMaxStackSize()) {
            ItemStack increased = current.clone();
            increased.setAmount(current.getAmount() + 1);
            ItemStack remaining = cursor.clone();
            remaining.setAmount(cursor.getAmount() - 1);
            return new StorageChange(increased, remaining.getAmount() == 0 ? null : remaining);
        }
        return null;
    }

    private void openConfirmation(Player player, ConfirmationAction action) {
        ConfirmationMenuHolder holder = new ConfirmationMenuHolder(action);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text(action == ConfirmationAction.DISMISS
                        ? "Odvolat koně?" : "Odebrat píšťalku?", NamedTextColor.DARK_RED));
        holder.inventory = inventory;
        fill(inventory);
        MountOverview overview = overview(player).orElseThrow();
        inventory.setItem(MENU_STATUS_SLOT, GuiItems.info("Stav společníka",
                Component.text(overview.state(), NamedTextColor.GRAY),
                Component.text("Zdraví " + oneDecimal(overview.health()) + "/"
                        + oneDecimal(overview.maxHealth()), NamedTextColor.DARK_GRAY),
                Component.text(overview.hasChest()
                        ? "Brašny " + overview.occupiedStorageSlots() + "/54" : "Bez brašen",
                        NamedTextColor.DARK_GRAY),
                Component.text(overview.cooldownSeconds() > 0
                        ? "Připraven za " + MountCooldown.format(overview.cooldownSeconds())
                        : "Píšťalka je připravená", NamedTextColor.DARK_GRAY)));
        inventory.setItem(CONFIRM_YES_SLOT, item(Material.LIME_DYE,
                Component.text("Ano", NamedTextColor.GREEN),
                Component.text(action == ConfirmationAction.DISMISS
                        ? "Kůň se vrátí do bezpečí" : "Píšťalku můžeš později obnovit",
                        NamedTextColor.GRAY)));
        inventory.setItem(CONFIRM_BACK_SLOT, backItem("Ještě ne"));
        player.openInventory(inventory);
    }

    private void handleConfirmationClick(Player player, ConfirmationMenuHolder holder, int slot) {
        if (slot == CONFIRM_BACK_SLOT) {
            openMenu(player);
            return;
        }
        if (slot != CONFIRM_YES_SLOT) {
            return;
        }
        if (holder.action == ConfirmationAction.DISMISS) {
            player.closeInventory();
            dismiss(player);
        } else {
            toggleWhistle(player);
            openMenu(player);
        }
    }

    private void toggleWhistle(Player player) {
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty()) {
            messages.send(player, "mount-not-owned");
            return;
        }
        MountRecord record = found.get();
        if (hasAnyWhistle(player)) {
            removeAllWhistles(player);
            actionBar(player, "mount-whistle-removed", Map.of());
            return;
        }
        if (giveWhistle(player, record)) {
            actionBar(player, "mount-whistle-restored", Map.of());
        } else {
            messages.send(player, "mount-whistle-no-space");
        }
    }

    private boolean giveWhistle(Player player, MountRecord record) {
        if (hasBoundWhistle(player, record)) {
            return true;
        }
        ItemStack whistle = createWhistle(record);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(whistle);
        return leftovers.isEmpty();
    }

    private ItemStack createWhistle(MountRecord record) {
        ItemStack whistle = new ItemStack(config().whistleMaterial());
        ItemMeta meta = whistle.getItemMeta();
        meta.displayName(Component.text("Píšťalka — " + record.customName(), NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text("Pravým kliknutím zavoláš svého koně.", NamedTextColor.GRAY),
                Component.text("Je-li nablízku, vyrazí k tvému novému místu.", NamedTextColor.DARK_GRAY)));
        meta.setCustomModelData(config().whistleCustomModelData());
        meta.getPersistentDataContainer().set(whistleKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(whistleMountIdKey, PersistentDataType.STRING,
                record.mountId().toString());
        meta.getPersistentDataContainer().set(whistleOwnerIdKey, PersistentDataType.STRING,
                record.ownerId());
        whistle.setItemMeta(meta);
        return whistle;
    }

    private boolean isWhistle(ItemStack item) {
        if (item == null || item.isEmpty() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(whistleKey, PersistentDataType.BYTE);
    }

    private boolean isOwnedWhistle(Player player, ItemStack whistle) {
        ItemMeta meta = whistle.getItemMeta();
        String ownerId = meta.getPersistentDataContainer().get(whistleOwnerIdKey, PersistentDataType.STRING);
        String mountId = meta.getPersistentDataContainer().get(whistleMountIdKey, PersistentDataType.STRING);
        Optional<MountRecord> owned = findOwned(player);
        return owned.isPresent() && owned.get().ownerId().equals(ownerId)
                && owned.get().mountId().toString().equals(mountId);
    }

    private boolean isBoundWhistle(ItemStack item, MountRecord record) {
        if (!isWhistle(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        String ownerId = meta.getPersistentDataContainer().get(whistleOwnerIdKey, PersistentDataType.STRING);
        String mountId = meta.getPersistentDataContainer().get(whistleMountIdKey, PersistentDataType.STRING);
        return record.ownerId().equals(ownerId) && record.mountId().toString().equals(mountId);
    }

    private boolean hasBoundWhistle(Player player, MountRecord record) {
        return containsBoundWhistle(player.getInventory(), record)
                || containsBoundWhistle(player.getEnderChest(), record);
    }

    private boolean hasAnyWhistle(Player player) {
        return containsWhistle(player.getInventory()) || containsWhistle(player.getEnderChest());
    }

    private boolean containsWhistle(Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (isWhistle(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsBoundWhistle(Inventory inventory, MountRecord record) {
        for (ItemStack item : inventory.getContents()) {
            if (isBoundWhistle(item, record)) {
                return true;
            }
        }
        return false;
    }

    private int removeAllWhistles(Player player) {
        return removeAllWhistles(player.getInventory()) + removeAllWhistles(player.getEnderChest());
    }

    private int removeAllWhistles(Inventory inventory) {
        int removed = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isWhistle(item)) {
                removed += item.getAmount();
                inventory.setItem(slot, null);
            }
        }
        return removed;
    }

    private void returnWhistleAfterDeath(Player player) {
        Optional<MountRecord> found = findOwned(player);
        if (found.isEmpty() || hasBoundWhistle(player, found.get())) {
            return;
        }
        if (giveWhistle(player, found.get())) {
            messages.send(player, "mount-whistle-returned");
        } else {
            messages.send(player, "mount-whistle-no-space");
        }
    }

    private boolean shouldBlockWhistleClick(InventoryClickEvent event) {
        ItemStack hotbarItem = event.getHotbarButton() < 0 || !(event.getWhoClicked() instanceof Player player)
                ? null : player.getInventory().getItem(event.getHotbarButton());
        boolean currentWhistle = isWhistle(event.getCurrentItem());
        boolean cursorWhistle = isWhistle(event.getCursor());
        boolean whistleInvolved = currentWhistle || cursorWhistle || isWhistle(hotbarItem);
        if (!whistleInvolved) {
            return false;
        }
        if (event.getRawSlot() < 0 || event.getAction() == InventoryAction.CLONE_STACK) {
            return true;
        }
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) {
            return true;
        }
        return (currentWhistle && !isEmpty(event.getCursor()) && !cursorWhistle)
                || (cursorWhistle && !isEmpty(event.getCurrentItem()) && !currentWhistle);
    }

    private boolean isEmpty(ItemStack item) {
        return item == null || item.isEmpty();
    }

    private boolean prepareOperation(Player player, boolean requireAllowedWorld) {
        if (!enabled) {
            messages.send(player, "module-disabled", Map.of("module", ID));
            return false;
        }
        if (repository == null || storageFailure != null) {
            messages.send(player, "mount-storage-error");
            return false;
        }
        if (requireAllowedWorld && !config().isWorldAllowed(player.getWorld().getName())) {
            messages.send(player, "mount-world-blocked");
            return false;
        }
        String ownerId = MountOwnerId.fromPlayerName(player.getName());
        Instant now = Instant.now();
        Optional<Instant> combatUntil;
        try {
            combatUntil = repository.combatUntil(ownerId);
        } catch (RuntimeException exception) {
            failStorage(exception);
            messages.send(player, "mount-storage-error");
            return false;
        }
        if (combatUntil.isPresent() && MountCooldown.isActive(combatUntil.get(), now)) {
            messages.send(player, "mount-combat-blocked", Map.of("time", MountCooldown.format(
                    MountCooldown.remainingSeconds(combatUntil.get(), now))));
            return false;
        }
        return true;
    }

    private boolean hasMountMilestone(Player player) {
        if (plugin.skillsModule().hasPowerMilestone(player.getUniqueId(), PowerMilestoneId.MOUNT)) {
            return true;
        }
        messages.send(player, "mount-milestone-locked", Map.of(
            "level", PowerMilestoneId.MOUNT.requiredPowerLevel()));
        return false;
    }

    private Optional<MountRecord> findOwned(Player player) {
        return findByOwnerId(MountOwnerId.fromPlayerName(player.getName()));
    }

    private Optional<MountRecord> findByOwnerId(String ownerId) {
        if (repository == null) {
            return Optional.empty();
        }
        try {
            return repository.findByOwnerId(ownerId);
        } catch (RuntimeException exception) {
            failStorage(exception);
            return Optional.empty();
        }
    }

    private Optional<MountRecord> findByMountId(UUID mountId) {
        if (repository == null || mountId == null) {
            return Optional.empty();
        }
        try {
            return repository.findByMountId(mountId);
        } catch (RuntimeException exception) {
            failStorage(exception);
            return Optional.empty();
        }
    }

    private MountConfig config() {
        return plugin.configuration().get().mounts();
    }

    private Optional<Location> findSafeSpawn(Player player) {
        Location origin = player.getLocation().getBlock().getLocation();
        int minimum = Math.min(config().minimumSpawnDistance(), config().maximumSpawnDistance());
        int maximum = Math.max(config().minimumSpawnDistance(), config().maximumSpawnDistance());
        for (int distance = maximum; distance >= minimum; distance--) {
            for (int x = -distance; x <= distance; x++) {
                for (int z = -distance; z <= distance; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) != distance) {
                        continue;
                    }
                    for (int y : new int[]{0, 1, -1, 2, -2}) {
                        Location candidate = origin.clone().add(x + 0.5, y, z + 0.5);
                        if (isSafe(candidate)) {
                            candidate.setYaw(player.getLocation().getYaw());
                            return Optional.of(candidate);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private boolean isSafe(Location location) {
        Material floor = location.clone().add(0, -1, 0).getBlock().getType();
        return floor.isSolid() && location.getBlock().isPassable()
                && location.clone().add(0, 1, 0).getBlock().isPassable()
                && location.clone().add(0, 2, 0).getBlock().isPassable();
    }

    private MountRecord capture(MountRecord base, Horse horse, UUID activeEntityUuid, Instant now) {
        AttributeInstance maxHealth = requireAttribute(horse, Attribute.MAX_HEALTH);
        AttributeInstance movementSpeed = requireAttribute(horse, Attribute.MOVEMENT_SPEED);
        double health = horse.isDead() ? 0.0 : Math.min(horse.getHealth(), maxHealth.getBaseValue());
        return new MountRecord(base.ownerId(), base.ownerName(), base.lastKnownOwnerUuid(), base.mountId(),
                activeEntityUuid, base.customName(), health, maxHealth.getBaseValue(),
                movementSpeed.getBaseValue(), horse.getJumpStrength(), horse.getColor(), horse.getStyle(),
                horse.getInventory().getSaddle(), horse.getInventory().getArmor(), base.chest(), base.storage(),
                horse.getFireTicks(),
                horse.getFreezeTicks(), horse.getRemainingAir(), List.copyOf(horse.getActivePotionEffects()),
                base.summonAvailableAt(), base.diedAt(), base.reviveAt(), now);
    }

    private void apply(MountRecord record, Player owner, Horse horse) {
        requireAttribute(horse, Attribute.MAX_HEALTH).setBaseValue(record.maxHealth());
        requireAttribute(horse, Attribute.MOVEMENT_SPEED).setBaseValue(record.movementSpeed());
        horse.setJumpStrength(record.jumpStrength());
        horse.setColor(record.color());
        horse.setStyle(record.style());
        applyBoldName(horse, record.customName());
        horse.setAdult();
        horse.setTamed(true);
        horse.setOwner(owner);
        horse.setDomestication(horse.getMaxDomestication());
        horse.setPersistent(true);
        horse.setAware(true);
        horse.getInventory().setSaddle(record.saddle() == null
                ? new ItemStack(Material.SADDLE) : record.saddle());
        horse.getInventory().setArmor(record.armor());
        horse.setHealth(Math.max(0.01, Math.min(record.health(), record.maxHealth())));
        horse.setFireTicks(Math.max(0, record.fireTicks()));
        horse.setFreezeTicks(Math.max(0, Math.min(record.freezeTicks(), horse.getMaxFreezeTicks())));
        horse.setRemainingAir(Math.max(0, Math.min(record.remainingAir(), horse.getMaximumAir())));
        for (var effect : horse.getActivePotionEffects()) {
            horse.removePotionEffect(effect.getType());
        }
        horse.addPotionEffects(record.potionEffects());
        tag(horse, record);
    }

    private void applyBoldName(Horse horse, String name) {
        horse.customName(Component.text(name, NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        horse.setCustomNameVisible(true);
    }

    private AttributeInstance requireAttribute(Horse horse, Attribute attribute) {
        AttributeInstance instance = horse.getAttribute(attribute);
        if (instance == null) {
            throw new IllegalStateException("Horse is missing required attribute " + attribute.getKey() + ".");
        }
        return instance;
    }

    private void tag(Horse horse, MountRecord record) {
        horse.getPersistentDataContainer().set(mountIdKey, PersistentDataType.STRING,
                record.mountId().toString());
        horse.getPersistentDataContainer().set(ownerIdKey, PersistentDataType.STRING, record.ownerId());
    }

    private boolean isManaged(Horse horse) {
        return readMountId(horse) != null
                && horse.getPersistentDataContainer().has(ownerIdKey, PersistentDataType.STRING);
    }

    private UUID readMountId(Horse horse) {
        String value = horse.getPersistentDataContainer().get(mountIdKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isOwner(Player player, Horse horse) {
        String ownerId = horse.getPersistentDataContainer().get(ownerIdKey, PersistentDataType.STRING);
        return MountOwnerId.fromPlayerName(player.getName()).equals(ownerId);
    }

    private Horse managedHorse(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof Horse horse && isManaged(horse) ? horse : null;
    }

    private Horse resolveActive(MountRecord record) {
        Horse cached = activeMounts.get(record.mountId());
        if (cached != null && cached.isValid() && cached.getUniqueId().equals(record.activeEntityUuid())) {
            return cached;
        }
        if (record.activeEntityUuid() == null) {
            return null;
        }
        for (World world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(record.activeEntityUuid());
            if (entity instanceof Horse horse && isManaged(horse)
                    && record.mountId().equals(readMountId(horse))) {
                activeMounts.put(record.mountId(), horse);
                return horse;
            }
        }
        return null;
    }

    private void reconcileLoadedMounts() {
        for (World world : Bukkit.getWorlds()) {
            for (Horse horse : world.getEntitiesByClass(Horse.class)) {
                if (isManaged(horse)) {
                    reconcile(horse);
                }
            }
        }
    }

    private void reconcile(Horse horse) {
        UUID mountId = readMountId(horse);
        Optional<MountRecord> found = findByMountId(mountId);
        if (found.isEmpty()) {
            horse.remove();
            return;
        }
        MountRecord record = found.get();
        String ownerId = horse.getPersistentDataContainer().get(ownerIdKey, PersistentDataType.STRING);
        if (!record.ownerId().equals(ownerId) || record.activeEntityUuid() == null
                || !record.activeEntityUuid().equals(horse.getUniqueId())) {
            horse.remove();
            return;
        }
        Horse existing = activeMounts.putIfAbsent(mountId, horse);
        if (existing != null && existing.isValid() && !existing.getUniqueId().equals(horse.getUniqueId())) {
            horse.remove();
            return;
        }
        if (existing != null && !existing.isValid()) {
            activeMounts.put(mountId, horse);
        }
        applyBoldName(horse, record.customName());
        if (horse.getInventory().getSaddle() == null) {
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        }
        if (horse.getPassengers().isEmpty()) {
            callAnchors.put(mountId, horse.getLocation().clone());
            arrivedAtCall.add(mountId);
            scheduleNextWander(mountId, Instant.now());
            horse.setAware(true);
        }
        saveActive(horse);
    }

    private boolean storeAndRemove(Horse horse, String reason) {
        if (repository == null || storageFailure != null || !horse.isValid()) {
            return false;
        }
        UUID mountId = readMountId(horse);
        Optional<MountRecord> found = findByMountId(mountId);
        if (found.isEmpty()) {
            return false;
        }
        MountRecord dormant = capture(found.get(), horse, null, Instant.now()).dormant(Instant.now());
        try {
            repository.update(dormant);
            activeMounts.remove(dormant.mountId());
            clearGuidance(dormant.mountId());
            horse.eject();
            horse.remove();
            return true;
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not store mount during " + reason + "; entity was retained.");
            failStorage(exception);
            return false;
        }
    }

    private void autosaveActiveMounts() {
        if (repository == null || storageFailure != null) {
            return;
        }
        for (Horse horse : new ArrayList<>(activeMounts.values())) {
            if (horse.isValid() && !horse.isDead()) {
                saveActive(horse);
            }
        }
    }

    private void saveActive(Horse horse) {
        if (repository == null || storageFailure != null || !horse.isValid() || horse.isDead()) {
            return;
        }
        UUID mountId = readMountId(horse);
        Optional<MountRecord> found = findByMountId(mountId);
        if (found.isEmpty()) {
            return;
        }
        try {
            repository.update(capture(found.get(), horse, horse.getUniqueId(), Instant.now()));
        } catch (IOException exception) {
            failStorage(exception);
        }
    }

    private void scheduleSave(Horse horse) {
        Bukkit.getScheduler().runTask(plugin, () -> saveActive(horse));
    }

    private void directToCall(UUID mountId, Horse horse, Location callLocation) {
        callAnchors.put(mountId, callLocation.clone());
        approachTargets.put(mountId, callLocation.clone());
        approachDistances.put(mountId, horse.getLocation().distanceSquared(callLocation));
        approachProgressAt.put(mountId, Instant.now());
        arrivedAtCall.remove(mountId);
        wanderTargets.remove(mountId);
        nextWanderAt.remove(mountId);
        activeRecallAvailableAt.put(mountId,
                Instant.now().plusSeconds(config().activeRecallCooldownSeconds()));
        horse.setAware(true);
        horse.getPathfinder().moveTo(callLocation, config().pathfindingSpeed());
    }

    private boolean ownsHolder(Player player, String ownerId) {
        return MountOwnerId.fromPlayerName(player.getName()).equals(ownerId);
    }

    private void guideActiveMounts() {
        Instant now = Instant.now();
        double arrivalRadiusSquared = config().waitingRadius() * config().waitingRadius();
        double wanderingRadiusSquared = config().wanderingRadius() * config().wanderingRadius();
        for (Map.Entry<UUID, Location> entry : new ArrayList<>(callAnchors.entrySet())) {
            UUID mountId = entry.getKey();
            Horse horse = activeMounts.get(mountId);
            if (horse == null || !horse.isValid() || horse.isDead()) {
                clearGuidance(mountId);
                continue;
            }
            if (!horse.getPassengers().isEmpty()) {
                horse.setAware(true);
                clearGuidance(mountId);
                continue;
            }
            Location anchor = entry.getValue();
            if (!horse.getWorld().equals(anchor.getWorld())) {
                clearGuidance(mountId);
                continue;
            }

            double distanceFromAnchor = horse.getLocation().distanceSquared(anchor);
            if (!arrivedAtCall.contains(mountId)) {
                if (distanceFromAnchor <= arrivalRadiusSquared) {
                    arrivedAtCall.add(mountId);
                    horse.getPathfinder().stopPathfinding();
                    scheduleNextWander(mountId, now);
                } else {
                    horse.setAware(true);
                    double previousDistance = approachDistances.getOrDefault(mountId, distanceFromAnchor);
                    if (distanceFromAnchor + 0.25 < previousDistance) {
                        approachDistances.put(mountId, distanceFromAnchor);
                        approachProgressAt.put(mountId, now);
                    } else if (!now.isBefore(approachProgressAt.getOrDefault(mountId, now)
                            .plusSeconds(6L))) {
                        approachTargets.put(mountId, findWanderTarget(anchor).orElse(anchor));
                        approachDistances.put(mountId, distanceFromAnchor);
                        approachProgressAt.put(mountId, now);
                    }
                    horse.getPathfinder().moveTo(approachTargets.getOrDefault(mountId, anchor),
                            config().pathfindingSpeed());
                }
                continue;
            }

            horse.setAware(true);
            if (distanceFromAnchor > wanderingRadiusSquared) {
                wanderTargets.remove(mountId);
                horse.getPathfinder().moveTo(anchor, config().pathfindingSpeed());
                continue;
            }

            Location target = wanderTargets.get(mountId);
            if (target != null) {
                if (!horse.getWorld().equals(target.getWorld())
                        || horse.getLocation().distanceSquared(target) <= 1.5
                        || !now.isBefore(nextWanderAt.getOrDefault(mountId, now))) {
                    horse.getPathfinder().stopPathfinding();
                    wanderTargets.remove(mountId);
                    scheduleNextWander(mountId, now);
                }
                continue;
            }
            if (now.isBefore(nextWanderAt.getOrDefault(mountId, now))) {
                continue;
            }
            Optional<Location> nextTarget = findWanderTarget(anchor);
            if (nextTarget.isPresent()) {
                wanderTargets.put(mountId, nextTarget.get());
                nextWanderAt.put(mountId, now.plusSeconds(8L));
                horse.getPathfinder().moveTo(nextTarget.get(), Math.max(0.6, config().pathfindingSpeed() * 0.65));
            } else {
                horse.getPathfinder().stopPathfinding();
                scheduleNextWander(mountId, now);
            }
        }
    }

    private Optional<Location> findWanderTarget(Location anchor) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double radius = random.nextDouble(1.5, config().wanderingRadius());
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            for (int y : new int[]{0, 1, -1, 2, -2}) {
                Location candidate = anchor.clone().add(x, y, z);
                candidate.setX(candidate.getBlockX() + 0.5);
                candidate.setZ(candidate.getBlockZ() + 0.5);
                if (isSafe(candidate)) {
                    return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    private void scheduleNextWander(UUID mountId, Instant now) {
        nextWanderAt.put(mountId, now.plusSeconds(ThreadLocalRandom.current().nextLong(3L, 9L)));
    }

    private void clearWandering(UUID mountId) {
        if (mountId == null) {
            return;
        }
        arrivedAtCall.remove(mountId);
        wanderTargets.remove(mountId);
        nextWanderAt.remove(mountId);
        approachTargets.remove(mountId);
        approachDistances.remove(mountId);
        approachProgressAt.remove(mountId);
    }

    private void clearGuidance(UUID mountId) {
        if (mountId == null) {
            return;
        }
        callAnchors.remove(mountId);
        clearWandering(mountId);
        activeRecallAvailableAt.remove(mountId);
    }

    private boolean updateRecord(MountRecord record) {
        try {
            repository.update(record);
            return true;
        } catch (IOException exception) {
            failStorage(exception);
            Player player = Bukkit.getPlayer(record.lastKnownOwnerUuid());
            if (player != null) {
                messages.send(player, "mount-storage-error");
            }
            return false;
        }
    }

    private void persistCombat(Player attacker, Player victim, Instant until) {
        try {
            Map<String, Instant> windows = new HashMap<>();
            windows.put(MountOwnerId.fromPlayerName(attacker.getName()), until);
            windows.put(MountOwnerId.fromPlayerName(victim.getName()), until);
            repository.setCombatUntil(windows);
        } catch (IOException exception) {
            failStorage(exception);
        }
    }

    private boolean isInCombat(String ownerId, Instant now) {
        try {
            return repository.combatUntil(ownerId)
                    .map(until -> MountCooldown.isActive(until, now)).orElse(false);
        } catch (RuntimeException exception) {
            failStorage(exception);
            return true;
        }
    }

    private Player resolvePlayerDamager(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            return shooter instanceof Player player ? player : null;
        }
        return null;
    }

    private ItemStack equipmentDisplay(ItemStack stored, EquipmentKind kind) {
        if (stored != null) {
            ItemStack display = stored.clone();
            ItemMeta meta = display.getItemMeta();
            List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
            lore.add(Component.text("Klikni prázdným kurzorem pro vyjmutí.", NamedTextColor.GRAY));
            meta.lore(lore);
            display.setItemMeta(meta);
            return display;
        }
        return item(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                Component.text(kind == EquipmentKind.SADDLE ? "Prázdný slot sedla"
                        : kind == EquipmentKind.CHEST ? "Prázdný slot truhly"
                        : "Prázdný slot brnění", NamedTextColor.GRAY),
                Component.text("Klikni na vhodný předmět ve svém inventáři.", NamedTextColor.DARK_GRAY),
                Component.text("Nebo jej vlož do slotu pomocí kurzoru.", NamedTextColor.DARK_GRAY));
    }

    private boolean isValidEquipment(ItemStack item, EquipmentKind kind) {
        return switch (kind) {
            case SADDLE -> item.getType() == Material.SADDLE;
            case CHEST -> item.getType() == Material.CHEST;
            case ARMOR -> item.getType().name().endsWith("_HORSE_ARMOR");
        };
    }

    private ItemStack emptyToNull(ItemStack item) {
        return item == null || item.isEmpty() ? null : item.clone();
    }

    private ItemStack colorItem(Horse.Color color, String label) {
        Material material = switch (color) {
            case WHITE -> Material.WHITE_WOOL;
            case CREAMY -> Material.YELLOW_WOOL;
            case CHESTNUT -> Material.ORANGE_WOOL;
            case BROWN -> Material.BROWN_WOOL;
            case BLACK -> Material.BLACK_WOOL;
            case GRAY -> Material.GRAY_WOOL;
            case DARK_BROWN -> Material.BROWN_CONCRETE;
        };
        return item(material, Component.text(label, NamedTextColor.GOLD),
                Component.text(colorDisplay(color), NamedTextColor.GRAY));
    }

    private String colorDisplay(Horse.Color color) {
        return switch (color) {
            case WHITE -> "Bílá";
            case CREAMY -> "Krémová";
            case CHESTNUT -> "Kaštanová";
            case BROWN -> "Hnědá";
            case BLACK -> "Černá";
            case GRAY -> "Šedá";
            case DARK_BROWN -> "Tmavě hnědá";
        };
    }

    private String oneDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private ItemStack item(Material material, Component name, Component... lore) {
        return GuiItems.item(material, name, lore);
    }

    private ItemStack backItem(String label) {
        return GuiItems.back(label);
    }

    private int occupiedStorageSlots(MountRecord record) {
        return (int) record.storage().stream().filter(java.util.Objects::nonNull).count();
    }

    private boolean hasStoredItems(MountRecord record) {
        return record.storage().stream().anyMatch(java.util.Objects::nonNull);
    }

    private boolean sameItem(ItemStack first, ItemStack second) {
        if (first == null || first.isEmpty()) {
            return second == null || second.isEmpty();
        }
        return second != null && !second.isEmpty() && first.equals(second);
    }

    private void actionBar(Player player, String key, Map<String, ?> placeholders) {
        player.sendActionBar(messages.component(key, placeholders));
    }

    private void fill(Inventory inventory) {
        GuiItems.fill(inventory);
    }

    private String sanitizeName(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().replaceAll("\\s+", " ");
    }

    private boolean isValidName(String name) {
        int length = name.codePointCount(0, name.length());
        return length >= config().minimumNameLength() && length <= config().maximumNameLength()
                && name.codePoints().noneMatch(Character::isISOControl);
    }

    private void closeModuleInventories() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
            if (holder instanceof MountMenuHolder || holder instanceof ColorMenuHolder
                    || holder instanceof EquipmentMenuHolder || holder instanceof StorageMenuHolder
                    || holder instanceof ConfirmationMenuHolder
                    || namePrompts.containsKey(player.getUniqueId())) {
                player.getOpenInventory().getTopInventory().clear();
                player.closeInventory();
            }
        }
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private void failStorage(Exception exception) {
        storageFailure = exception.getMessage() == null
                ? exception.getClass().getSimpleName() : exception.getMessage();
        plugin.getLogger().severe("NekaraMounts storage failed; further operations are locked: "
                + storageFailure);
    }

    private enum NamePromptMode {
        CREATE,
        RENAME
    }

    private enum EquipmentKind {
        SADDLE,
        CHEST,
        ARMOR
    }

    private enum ConfirmationAction {
        DISMISS,
        REMOVE_WHISTLE
    }

    public record MountOverview(
            String name,
            String state,
            double health,
            double maxHealth,
            int occupiedStorageSlots,
            boolean hasChest,
            long cooldownSeconds
    ) {
    }

    private record StorageChange(ItemStack slotItem, ItemStack cursorItem) {
        private StorageChange {
            slotItem = slotItem == null || slotItem.isEmpty() ? null : slotItem.clone();
            cursorItem = cursorItem == null || cursorItem.isEmpty() ? null : cursorItem.clone();
        }
    }

    private record NamePrompt(NamePromptMode mode, Horse.Color color) {
    }

    private static final class MountMenuHolder implements InventoryHolder {
        private final String ownerId;
        private Inventory inventory;

        private MountMenuHolder(String ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class ColorMenuHolder implements InventoryHolder {
        private final boolean creation;
        private Inventory inventory;

        private ColorMenuHolder(boolean creation) {
            this.creation = creation;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class EquipmentMenuHolder implements InventoryHolder {
        private final String ownerId;
        private Inventory inventory;

        private EquipmentMenuHolder(String ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class StorageMenuHolder implements InventoryHolder {
        private final String ownerId;
        private Inventory inventory;

        private StorageMenuHolder(String ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class ConfirmationMenuHolder implements InventoryHolder {
        private final ConfirmationAction action;
        private Inventory inventory;

        private ConfirmationMenuHolder(ConfirmationAction action) {
            this.action = action;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
