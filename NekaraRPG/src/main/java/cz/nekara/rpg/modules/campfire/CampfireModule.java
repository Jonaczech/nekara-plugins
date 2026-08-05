package cz.nekara.rpg.modules.campfire;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.campfire.CampfireKey;
import cz.nekara.rpg.campfire.CampfireRestMath;
import cz.nekara.rpg.campfire.CampfireRestSession;
import cz.nekara.rpg.campfire.CampfireSleepRules;
import cz.nekara.rpg.campfire.CampFeature;
import cz.nekara.rpg.campfire.CampFeatureSnapshot;
import cz.nekara.rpg.campfire.LieResult;
import cz.nekara.rpg.campfire.RestedBonusState;
import cz.nekara.rpg.configuration.CampfireConfig;
import cz.nekara.rpg.configuration.CampfireVisualConfig;
import cz.nekara.rpg.configuration.CampingConfig;
import cz.nekara.rpg.configuration.RestedEffectConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.modules.sitting.SittingModule;
import cz.nekara.rpg.skills.milestones.PowerMilestoneId;
import cz.nekara.rpg.sounds.SoundService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class CampfireModule implements NekaraModule, Listener {
    public static final String ID = "campfire";
    private static final String RESTED_HASTE_TAG = "nekararpg-rested-haste";

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final SoundService sounds;
    private final SittingModule sitting;
    private final Map<UUID, CampfireRestSession> restingSessions = new HashMap<>();
    private final Map<UUID, RestedBonusState> restedBonuses = new HashMap<>();
    private final Map<UUID, ManagedHasteEffect> managedHasteEffects = new HashMap<>();
    private final Map<UUID, Long> soloSleepSince = new HashMap<>();
    private Listener mythicSpawnListener;
    private BukkitTask updateTask;
    private boolean enabled;

    public CampfireModule(
            NekaraRPGPlugin plugin,
            MessageService messages,
            SoundService sounds,
            SittingModule sitting
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.sounds = sounds;
        this.sitting = sitting;
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
        clearStaleRestedHasteEffects();
        sitting.enable();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        registerMythicSpawnListener();
        startUpdateTask();
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        stopUpdateTask();
        HandlerList.unregisterAll(this);
        if (mythicSpawnListener != null) {
            HandlerList.unregisterAll(mythicSpawnListener);
            mythicSpawnListener = null;
        }
        restingSessions.clear();
        soloSleepSince.clear();
        clearManagedHasteEffects();
        restedBonuses.clear();
        sitting.disable();
        enabled = false;
    }

    @Override
    public void reload() {
        stopUpdateTask();
        clearManagedHasteEffects();
        sitting.reload();
        startUpdateTask();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public int restingCount() {
        return restingSessions.size();
    }

    public int restedCount() {
        removeExpiredBonuses(System.currentTimeMillis());
        return restedBonuses.size();
    }

    public boolean isRested(UUID playerId) {
        RestedBonusState state = activeRestedBonus(playerId, System.currentTimeMillis());
        return state != null;
    }

    public long restedSecondsRemaining(UUID playerId) {
        long now = System.currentTimeMillis();
        RestedBonusState state = activeRestedBonus(playerId, now);
        return state == null ? 0L : state.remainingSeconds(now);
    }

    /** Returns the native Skills XP multiplier for an active Rested player. */
    public double skillsExperienceMultiplier(UUID playerId) {
        if (!isRested(playerId)) {
            return 1.0;
        }
        var experience = plugin.configuration().get().campfire().restedExperience();
        return experience.enabled() ? experience.multiplier() : 1.0;
    }

    public LieResult lieDown(Player player) {
        if (!enabled) {
            return LieResult.MODULE_DISABLED;
        }
        CampfireConfig config = plugin.configuration().get().campfire();
        if (!config.lying().enabled()) {
            return LieResult.LYING_DISABLED;
        }
        if (sitting.isSeated(player) || sitting.isLying(player)) {
            return LieResult.ALREADY_RESTING;
        }
        return sitting.lie(player) ? LieResult.SUCCESS : LieResult.INVALID_STATE;
    }

    public boolean rise(Player player) {
        return sitting.rise(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        int currentLevel = player.getFoodLevel();
        int requestedLevel = event.getFoodLevel();
        if (requestedLevel >= currentLevel) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (restingSessions.containsKey(playerId)) {
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();
        RestedBonusState state = activeRestedBonus(playerId, now);
        if (state == null || !state.hungerReductionEnabled()) {
            return;
        }
        int rawLoss = currentLevel - requestedLevel;
        CampfireRestMath.HungerLossResult result = CampfireRestMath.scaleHungerLoss(
                rawLoss,
                plugin.configuration().get().campfire().restedHungerLossMultiplier(),
                state.hungerLossCarry()
        );
        state.hungerLossCarry(result.carry());
        if (result.appliedLoss() == 0) {
            event.setCancelled(true);
        } else {
            event.setFoodLevel(Math.max(0, currentLevel - result.appliedLoss()));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getScoreboardTags().contains(RESTED_HASTE_TAG)
                && activeRestedBonus(player.getUniqueId(), System.currentTimeMillis()) == null) {
            clearStaleRestedHaste(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }
        CampfireConfig config = plugin.configuration().get().campfire();
        if (shouldBlockSpawn(event.getLocation(), event.getSpawnReason(), config)) {
            event.setCancelled(true);
        }
    }

    private void startUpdateTask() {
        CampfireConfig config = plugin.configuration().get().campfire();
        updateTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::updateRestingPlayers,
                config.updatePeriodTicks(),
                config.updatePeriodTicks()
        );
    }

    private void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void registerMythicSpawnListener() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("MythicMobs")) {
            return;
        }
        try {
            mythicSpawnListener = new MythicCampSpawnListener(this);
            plugin.getServer().getPluginManager().registerEvents(mythicSpawnListener, plugin);
            plugin.getLogger().info("Campfire spawn protection connected to MythicMobs.");
        } catch (RuntimeException | LinkageError error) {
            mythicSpawnListener = null;
            plugin.getLogger().warning("MythicMobs was found, but its spawn API is incompatible; "
                    + "vanilla spawn protection remains active.");
        }
    }

    boolean shouldBlockMythicSpawn(
            Location location,
            CreatureSpawnEvent.SpawnReason spawnReason,
            String faction
    ) {
        CampfireConfig config = plugin.configuration().get().campfire();
        if (faction == null
                || !config.camping().mythicHostileFaction().equalsIgnoreCase(faction)) {
            return false;
        }
        return shouldBlockSpawn(location, spawnReason, config);
    }

    private boolean shouldBlockSpawn(
            Location location,
            CreatureSpawnEvent.SpawnReason spawnReason,
            CampfireConfig config
    ) {
        CampingConfig camping = config.camping();
        if (!camping.spawnProtectionEnabled()) {
            return false;
        }
        if (camping.spawnProtectionNaturalOnly()
                && spawnReason != CreatureSpawnEvent.SpawnReason.NATURAL) {
            return false;
        }
        return isProtectedCampLocation(location, camping);
    }

    private void updateRestingPlayers() {
        long now = System.currentTimeMillis();
        CampfireConfig config = plugin.configuration().get().campfire();
        removeExpiredBonuses(now);

        Map<CampfireKey, List<Player>> groups = new HashMap<>();
        Map<UUID, CampfireKey> activeCampfires = new HashMap<>();
        for (Player player : sitting.restingPlayers()) {
            if (!player.hasPermission("nekararpg.campfire.use")
                    || !plugin.configuration().get().worlds().isEnabled(player.getWorld().getName())
                    || !plugin.skillsModule().hasPowerMilestone(player.getUniqueId(),
                        PowerMilestoneId.CAMPFIRE_RESTED)) {
                continue;
            }
            Block campfire = findNearestLitCampfire(player.getLocation(), config.radius());
            if (campfire == null) {
                continue;
            }
            CampfireKey key = CampfireKey.from(campfire);
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(player);
            activeCampfires.put(player.getUniqueId(), key);
        }

        removeInactiveSessions(activeCampfires);
        for (Map.Entry<CampfireKey, List<Player>> entry : groups.entrySet()) {
            List<Player> players = entry.getValue();
            Block campfireBlock = resolveCampfire(entry.getKey());
            CampFeatureSnapshot features = campfireBlock == null
                    ? new CampFeatureSnapshot(Set.of())
                    : scanCampFeatures(campfireBlock, config.camping());
            double multiplier = CampfireRestMath.groupMultiplier(
                    players.size(),
                    config.groupMultiplierPerExtraPlayer(),
                    config.maximumGroupMultiplier()
            );
            for (Player player : players) {
                processRestingPlayer(player, entry.getKey(), players.size(), multiplier,
                        features, config, now);
            }
        }
        trySkipNight(now, config);
        updateRestedVisuals(now, config);
    }

    private void trySkipNight(long now, CampfireConfig config) {
        if (Bukkit.getOnlinePlayers().size() != 1) {
            soloSleepSince.clear();
            return;
        }
        Player player = Bukkit.getOnlinePlayers().iterator().next();
        World world = player.getWorld();
        long time = world.getTime();
        UUID playerId = player.getUniqueId();
        boolean eligiblePosture = sitting.isLying(player) && restingSessions.containsKey(playerId);
        boolean normalNight = world.getEnvironment() == World.Environment.NORMAL
                && time >= 12_542L && time < 23_460L;
        if (!eligiblePosture || !normalNight || !config.lying().skipNightWhenAlone()) {
            soloSleepSince.remove(playerId);
            return;
        }
        long eligibleSince = soloSleepSince.computeIfAbsent(playerId, ignored -> now);
        if (!CampfireSleepRules.canSkipNight(
                config.lying().skipNightWhenAlone(),
                Bukkit.getOnlinePlayers().size(),
                eligiblePosture,
                now - eligibleSince,
                config.lying().fallAsleepSeconds() * 1_000L,
                true,
                time)) {
            return;
        }
        world.setTime(0L);
        soloSleepSince.remove(playerId);
        sitting.rise(player);
        messages.sendActionBar(player, "campfire-night-passed", Map.of());
    }

    private void processRestingPlayer(
            Player player,
            CampfireKey campfire,
            int groupSize,
            double groupMultiplier,
            CampFeatureSnapshot features,
            CampfireConfig config,
            long now
    ) {
        UUID playerId = player.getUniqueId();
        CampfireRestSession session = restingSessions.get(playerId);
        if (session == null || !session.campfire().equals(campfire)) {
            if (session != null) {
                restingSessions.remove(playerId);
            }
            session = new CampfireRestSession(
                    playerId,
                    campfire,
                    now,
                    config.healPeriodSeconds(),
                    config.hungerRestorePeriodSeconds()
            );
            restingSessions.put(playerId, session);
            messages.sendActionBar(player, "campfire-rest-start", Map.of());
        }

        applyHealing(player, session, groupMultiplier, config, now);
        applyHungerRestoration(player, session, groupMultiplier, config, now);
        showParticles(
                player,
                config.visuals().restingParticlesEnabled(),
                config.visuals().restingParticle(),
                config.visuals().restingParticleCount(),
                config.visuals().restingParticleRadius(),
                config.visuals().restingParticleYOffset()
        );

        long elapsedSeconds = session.elapsedSeconds(now);
        int restedDurationSeconds = CampfireRestMath.restedDurationSeconds(
                config.restedDurationSeconds(),
                features.count(),
                config.camping().durationPerFeatureSeconds()
        );
        boolean hasteEnabled = features.has(CampFeature.CRAFTING_TABLE);
        boolean hungerReductionEnabled = features.has(CampFeature.SMOKER);
        boolean justGranted = false;
        if (!session.restedGranted() && elapsedSeconds >= config.restedChargeSeconds()) {
            restedBonuses.put(playerId, new RestedBonusState(
                    now + restedDurationSeconds * 1_000L,
                    restedDurationSeconds,
                    hasteEnabled,
                    hungerReductionEnabled
            ));
            session.restedGranted(true);
            justGranted = true;
            messages.sendActionBar(player, "campfire-rested-granted", Map.of(
                    "duration", restedDurationSeconds,
                    "duration_text", formatDuration(restedDurationSeconds)
            ));
            sounds.play(player, "campfire-rested");
        } else if (session.restedGranted()) {
            RestedBonusState state = restedBonuses.computeIfAbsent(
                    playerId,
                    ignored -> new RestedBonusState(
                            now + restedDurationSeconds * 1_000L,
                            restedDurationSeconds,
                            hasteEnabled,
                            hungerReductionEnabled
                    )
            );
            state.refresh(now + restedDurationSeconds * 1_000L,
                    restedDurationSeconds, hasteEnabled, hungerReductionEnabled);
        }
        if (!justGranted) {
            sendRestingActionBar(player, session, groupSize, groupMultiplier, config, now);
        }
    }

    @SuppressWarnings("deprecation")
    private void applyHealing(
            Player player,
            CampfireRestSession session,
            double multiplier,
            CampfireConfig config,
            long now
    ) {
        if (!session.shouldHeal(now)) {
            return;
        }
        session.scheduleNextHeal(now, config.healPeriodSeconds());
        if (config.healAmount() <= 0.0 || player.isDead()) {
            return;
        }
        double maximumHealth = player.getMaxHealth();
        if (player.getHealth() < maximumHealth) {
            player.setHealth(Math.min(maximumHealth, player.getHealth() + config.healAmount() * multiplier));
        }
    }

    private void applyHungerRestoration(
            Player player,
            CampfireRestSession session,
            double multiplier,
            CampfireConfig config,
            long now
    ) {
        if (!session.shouldRestoreHunger(now)) {
            return;
        }
        session.scheduleNextHungerRestore(now, config.hungerRestorePeriodSeconds());
        if (config.hungerRestoreAmount() <= 0 || player.getFoodLevel() >= 20) {
            session.hungerRestoreCarry(0.0);
            return;
        }
        double scaledRestoration = config.hungerRestoreAmount() * multiplier + session.hungerRestoreCarry();
        int appliedRestoration = (int) Math.floor(scaledRestoration);
        session.hungerRestoreCarry(scaledRestoration - appliedRestoration);
        if (appliedRestoration > 0) {
            player.setFoodLevel(Math.min(20, player.getFoodLevel() + appliedRestoration));
        }
    }

    private void sendRestingActionBar(
            Player player,
            CampfireRestSession session,
            int groupSize,
            double multiplier,
            CampfireConfig config,
            long now
    ) {
        Map<String, Object> placeholders = Map.of(
                "elapsed", Math.min(session.elapsedSeconds(now), config.restedChargeSeconds()),
                "required", config.restedChargeSeconds(),
                "group", groupSize,
                "multiplier", String.format(Locale.ROOT, "%.2f", multiplier),
                "remaining", restedSecondsRemaining(player.getUniqueId())
        );
        if (session.restedGranted()) {
            return;
        }
        if (groupSize > 1 && session.elapsedSeconds(now) % 4L == 0L) {
            messages.sendActionBar(player, "campfire-resting-group", placeholders);
            return;
        }
        double progress = (double) session.elapsedSeconds(now) / config.restedChargeSeconds();
        String key = progress < 0.34
                ? "campfire-resting-early"
                : progress < 0.67 ? "campfire-resting-mid" : "campfire-resting-late";
        messages.sendActionBar(player, key, placeholders);
    }

    private void removeInactiveSessions(Map<UUID, CampfireKey> activeCampfires) {
        for (Map.Entry<UUID, CampfireRestSession> entry : new ArrayList<>(restingSessions.entrySet())) {
            CampfireKey active = activeCampfires.get(entry.getKey());
            if (entry.getValue().campfire().equals(active)) {
                continue;
            }
            restingSessions.remove(entry.getKey());
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                messages.sendActionBar(player, "campfire-rest-left", Map.of());
            }
        }
    }

    private Block findNearestLitCampfire(Location origin, double radius) {
        int minimumX = (int) Math.floor(origin.getX() - radius);
        int maximumX = (int) Math.ceil(origin.getX() + radius);
        int minimumY = (int) Math.floor(origin.getY() - radius);
        int maximumY = (int) Math.ceil(origin.getY() + radius);
        int minimumZ = (int) Math.floor(origin.getZ() - radius);
        int maximumZ = (int) Math.ceil(origin.getZ() + radius);
        double maximumDistanceSquared = radius * radius;
        double nearestDistanceSquared = Double.MAX_VALUE;
        Block nearest = null;

        for (int x = minimumX; x <= maximumX; x++) {
            for (int y = minimumY; y <= maximumY; y++) {
                for (int z = minimumZ; z <= maximumZ; z++) {
                    double deltaX = x + 0.5 - origin.getX();
                    double deltaY = y + 0.5 - origin.getY();
                    double deltaZ = z + 0.5 - origin.getZ();
                    double distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                    if (distanceSquared > maximumDistanceSquared || distanceSquared >= nearestDistanceSquared) {
                        continue;
                    }
                    if (!origin.getWorld().isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    Block block = origin.getWorld().getBlockAt(x, y, z);
                    if (!isLitCampfire(block)) {
                        continue;
                    }
                    nearest = block;
                    nearestDistanceSquared = distanceSquared;
                }
            }
        }
        return nearest;
    }

    private Block resolveCampfire(CampfireKey key) {
        org.bukkit.World world = Bukkit.getWorld(key.worldId());
        if (world == null || !world.isChunkLoaded(key.x() >> 4, key.z() >> 4)) {
            return null;
        }
        Block block = world.getBlockAt(key.x(), key.y(), key.z());
        return isLitCampfire(block) ? block : null;
    }

    private CampFeatureSnapshot scanCampFeatures(Block campfire, CampingConfig config) {
        EnumSet<CampFeature> found = EnumSet.noneOf(CampFeature.class);
        double radius = config.featureRadius();
        double maximumDistanceSquared = radius * radius;
        int minimumX = (int) Math.floor(campfire.getX() - radius);
        int maximumX = (int) Math.ceil(campfire.getX() + radius);
        int minimumY = (int) Math.floor(campfire.getY() - radius);
        int maximumY = (int) Math.ceil(campfire.getY() + radius);
        int minimumZ = (int) Math.floor(campfire.getZ() - radius);
        int maximumZ = (int) Math.ceil(campfire.getZ() + radius);

        for (int x = minimumX; x <= maximumX; x++) {
            for (int y = minimumY; y <= maximumY; y++) {
                for (int z = minimumZ; z <= maximumZ; z++) {
                    double deltaX = x - campfire.getX();
                    double deltaY = y - campfire.getY();
                    double deltaZ = z - campfire.getZ();
                    if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ
                            > maximumDistanceSquared) {
                        continue;
                    }
                    if (!campfire.getWorld().isChunkLoaded(x >> 4, z >> 4)) {
                        continue;
                    }
                    Material material = campfire.getWorld().getBlockAt(x, y, z).getType();
                    for (CampFeature feature : config.enabledFeatures()) {
                        if (!found.contains(feature) && feature.matches(material)) {
                            found.add(feature);
                        }
                    }
                    if (found.size() == config.enabledFeatures().size()) {
                        return new CampFeatureSnapshot(found);
                    }
                }
            }
        }
        return new CampFeatureSnapshot(found);
    }

    private boolean isProtectedCampLocation(Location location, CampingConfig config) {
        if (location.getWorld() == null
                || !plugin.configuration().get().worlds().isEnabled(location.getWorld().getName())) {
            return false;
        }
        double radius = config.spawnProtectionRadius();
        double maximumDistanceSquared = radius * radius;
        int minimumChunkX = (int) Math.floor(location.getX() - radius) >> 4;
        int maximumChunkX = (int) Math.floor(location.getX() + radius) >> 4;
        int minimumChunkZ = (int) Math.floor(location.getZ() - radius) >> 4;
        int maximumChunkZ = (int) Math.floor(location.getZ() + radius) >> 4;

        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                if (!location.getWorld().isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }
                Chunk chunk = location.getWorld().getChunkAt(chunkX, chunkZ);
                for (BlockState state : chunk.getTileEntities()) {
                    Block campfire = state.getBlock();
                    if (!isLitCampfire(campfire)
                            || campfire.getLocation().add(0.5, 0.5, 0.5)
                            .distanceSquared(location) > maximumDistanceSquared) {
                        continue;
                    }
                    if (scanCampFeatures(campfire, config).has(CampFeature.BED)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isLitCampfire(Block block) {
        Material type = block.getType();
        return (type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE)
                && block.getBlockData() instanceof Campfire data
                && data.isLit();
    }

    private RestedBonusState activeRestedBonus(UUID playerId, long now) {
        RestedBonusState state = restedBonuses.get(playerId);
        if (state != null && !state.isActive(now)) {
            restedBonuses.remove(playerId);
            removeManagedHasteEffect(playerId);
            return null;
        }
        return state;
    }

    private void removeExpiredBonuses(long now) {
        for (Map.Entry<UUID, RestedBonusState> entry : new ArrayList<>(restedBonuses.entrySet())) {
            if (!entry.getValue().isActive(now)) {
                restedBonuses.remove(entry.getKey());
                removeManagedHasteEffect(entry.getKey());
            }
        }
    }

    private void updateRestedVisuals(long now, CampfireConfig config) {
        CampfireVisualConfig visuals = config.visuals();
        for (Map.Entry<UUID, RestedBonusState> entry : restedBonuses.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }
            RestedBonusState state = entry.getValue();
            long remainingSeconds = state.remainingSeconds(now);
            applyRestedEffect(player, state, now, config);
            if (canShowRestedActionBar(player, now, config)) {
                messages.sendActionBar(player, "campfire-rested-timer", Map.of(
                        "remaining", remainingSeconds,
                        "remaining_text", CampfireRestMath.formatCountdown(remainingSeconds)
                ));
            }
            showParticles(
                    player,
                    visuals.restedParticlesEnabled(),
                    visuals.restedParticle(),
                    visuals.restedParticleCount(),
                    visuals.restedParticleRadius(),
                    visuals.restedParticleYOffset()
            );
        }
    }

    private boolean canShowRestedActionBar(Player player, long now, CampfireConfig config) {
        if (plugin.skillsModule() != null
                && plugin.skillsModule().isExperienceFeedbackVisible(player.getUniqueId())) {
            return false;
        }
        CampfireRestSession session = restingSessions.get(player.getUniqueId());
        boolean campfireCharging = session != null
                && session.elapsedSeconds(now) <= config.restedChargeSeconds();
        boolean fishingMinigameActive = plugin.fishingModule() != null
                && plugin.fishingModule().minigames().isMinigameActive(player.getUniqueId());
        return CampfireRestMath.shouldShowRestedTimer(
                config.visuals().restedActionBarEnabled(),
                campfireCharging,
                fishingMinigameActive
        );
    }

    private void showParticles(
            Player player,
            boolean enabled,
            Particle particle,
            int count,
            double radius,
            double yOffset
    ) {
        if (!enabled) {
            return;
        }
        Location center = player.getLocation().clone().add(0.0, yOffset, 0.0);
        player.getWorld().spawnParticle(particle, center, count, radius, 0.2, radius, 0.01);
    }

    private void applyRestedEffect(
            Player player,
            RestedBonusState state,
            long now,
            CampfireConfig config
    ) {
        RestedEffectConfig effect = config.restedEffect();
        UUID playerId = player.getUniqueId();
        if (!effect.hasteEnabled() || !state.hasteEnabled()) {
            removeManagedHasteEffect(playerId);
            return;
        }

        int durationTicks = toPotionTicks(state.remainingSeconds(now));
        ManagedHasteEffect managed = managedHasteEffects.get(playerId);
        PotionEffect current = player.getPotionEffect(PotionEffectType.HASTE);
        if (managed == null && current != null) {
            return;
        }
        if (managed != null && current != null
                && (current.getAmplifier() != managed.amplifier()
                || current.getDuration() > managed.maximumDurationTicks() + 40)) {
            managedHasteEffects.remove(playerId);
            player.removeScoreboardTag(RESTED_HASTE_TAG);
            return;
        }

        int maximumManagedDuration = Math.max(
                durationTicks,
                current == null ? 0 : current.getDuration()
        );
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.HASTE,
                durationTicks,
                effect.hasteAmplifier(),
                effect.ambient(),
                effect.particles(),
                effect.icon()
        ));
        managedHasteEffects.put(playerId, new ManagedHasteEffect(
                effect.hasteAmplifier(), maximumManagedDuration));
        player.addScoreboardTag(RESTED_HASTE_TAG);
    }

    private int toPotionTicks(long remainingSeconds) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, remainingSeconds * 20L));
    }

    private void removeManagedHasteEffect(UUID playerId) {
        ManagedHasteEffect managed = managedHasteEffects.get(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        managedHasteEffects.remove(playerId);
        PotionEffect current = player.getPotionEffect(PotionEffectType.HASTE);
        if (managed != null && current != null
                && current.getAmplifier() == managed.amplifier()
                && current.getDuration() <= managed.maximumDurationTicks() + 40) {
            player.removePotionEffect(PotionEffectType.HASTE);
        }
        player.removeScoreboardTag(RESTED_HASTE_TAG);
    }

    private void clearManagedHasteEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains(RESTED_HASTE_TAG)) {
                player.removePotionEffect(PotionEffectType.HASTE);
                player.removeScoreboardTag(RESTED_HASTE_TAG);
            }
        }
        managedHasteEffects.clear();
    }

    private void clearStaleRestedHasteEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clearStaleRestedHaste(player);
        }
    }

    private void clearStaleRestedHaste(Player player) {
        if (!player.getScoreboardTags().contains(RESTED_HASTE_TAG)) {
            return;
        }
        player.removePotionEffect(PotionEffectType.HASTE);
        player.removeScoreboardTag(RESTED_HASTE_TAG);
        managedHasteEffects.remove(player.getUniqueId());
    }

    private String formatDuration(int durationSeconds) {
        if (durationSeconds % 60 == 0) {
            return durationSeconds / 60 + " min";
        }
        return durationSeconds + " s";
    }

    private record ManagedHasteEffect(int amplifier, int maximumDurationTicks) {
    }
}
