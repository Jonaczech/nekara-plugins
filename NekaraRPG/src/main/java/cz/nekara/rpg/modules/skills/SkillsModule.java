package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.SkillsConfig;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.admin.SkillAdminActor;
import cz.nekara.rpg.skills.admin.SkillAdminInspection;
import cz.nekara.rpg.skills.admin.SkillAdminOperation;
import cz.nekara.rpg.skills.admin.SkillAdminResult;
import cz.nekara.rpg.skills.admin.SkillAdministrationService;
import cz.nekara.rpg.skills.experience.ExperienceAwardRequest;
import cz.nekara.rpg.skills.experience.ExperienceAwardResult;
import cz.nekara.rpg.skills.experience.ExperienceGrantGuard;
import cz.nekara.rpg.skills.experience.GlobalExperienceEvent;
import cz.nekara.rpg.skills.experience.ExperiencePolicy;
import cz.nekara.rpg.skills.experience.SkillExperienceService;
import cz.nekara.rpg.skills.export.SkillExportResult;
import cz.nekara.rpg.skills.export.SkillExportService;
import cz.nekara.rpg.skills.newgameplus.NewGamePlusResult;
import cz.nekara.rpg.skills.newgameplus.NewGamePlusService;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.PerkDefinition;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.perks.PerkPurchasePolicy;
import cz.nekara.rpg.skills.perks.PerkPurchaseService;
import cz.nekara.rpg.skills.perks.PerkTreeViewport;
import cz.nekara.rpg.skills.perks.PerkMechanicResolver;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillStorageException;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.telemetry.SkillRuntimeMetrics;
import cz.nekara.rpg.skills.telemetry.SkillRuntimeMetricsSnapshot;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SkillsModule implements NekaraModule {
    public static final String ID = "skills";
    private static final int MAX_QUEUED_EXPERIENCE_AWARDS = 8_192;
    private static final int EXPERIENCE_DRAIN_BATCH = 256;

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final DefaultPerkTree perkTree;
    private final SkillsMenu menu;
    private final SkillExperienceFeedback experienceFeedback;
    private final Set<UUID> pendingPurchases = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pendingAdminMutations = ConcurrentHashMap.newKeySet();
    private final Map<UUID, SkillProfile> profileCache = new ConcurrentHashMap<>();
    private final Map<UUID, Double> allExperienceBoosts = new ConcurrentHashMap<>();
    private final Map<ExperienceBoostKey, Double> skillExperienceBoosts = new ConcurrentHashMap<>();
    private final Map<RuntimeStateKey, SkillRuntimeState> runtimeStateCache = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<PendingExperienceAward> experienceQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger queuedExperienceAwards = new AtomicInteger();
    private final AtomicBoolean experienceDrainScheduled = new AtomicBoolean();
    private final AtomicBoolean exportInProgress = new AtomicBoolean();
    private final AtomicLong nextQueueWarningAt = new AtomicLong();
    private final PerkStatResolver perkStats;
    private final PerkMechanicResolver perkMechanics;
    private volatile SkillProfileRepository repository;
    private volatile SkillProgressResolver progressResolver;
    private volatile PerkPurchaseService purchaseService;
    private volatile SkillExperienceService experienceService;
    private volatile GlobalExperienceEvent globalExperienceEvent;
    private volatile NewGamePlusService newGamePlusService;
    private volatile SkillsConfig activeConfig;
    private volatile SkillAdministrationService administrationService;
    private volatile SkillExportService exportService;
    private volatile SkillRuntimeMetrics runtimeMetrics =
        new SkillRuntimeMetrics(System.currentTimeMillis());
    private volatile NativeGatheringListener nativeGatheringListener;
    private volatile GatheringAbilityListener gatheringAbilityListener;
    private volatile GatheringUtilityPerkListener gatheringUtilityPerkListener;
    private volatile NativeActivityListener nativeActivityListener;
    private volatile CombatPerkListener combatPerkListener;
    private volatile ProductionPerkListener productionPerkListener;
    private volatile String storageFailure;
    private volatile long generation;
    private boolean enabled;

    public SkillsModule(NekaraRPGPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.perkTree = DefaultPerkTree.create();
        this.perkStats = new PerkStatResolver(perkTree.catalog());
        this.perkMechanics = new PerkMechanicResolver(perkTree.catalog());
        this.menu = new SkillsMenu(plugin, this, perkTree);
        this.experienceFeedback = new SkillExperienceFeedback(plugin, messages);
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
        generation++;
        runtimeMetrics = new SkillRuntimeMetrics(System.currentTimeMillis());
        SkillsConfig config = plugin.configuration().get().skills();
        activeConfig = config;
        SkillProgressionCurve progressionCurve = new SkillProgressionCurve(
            SkillProgressionCurve.DEFAULT_MAX_LEVEL,
            config.baseExperience(),
            config.linearGrowth(),
            config.quadraticGrowth()
        );
        progressResolver = new SkillProgressResolver(progressionCurve);
        storageFailure = null;
        try {
            SqliteSkillProfileRepository sqliteRepository = new SqliteSkillProfileRepository(
                new File(plugin.getDataFolder(), config.databaseFile()));
            repository = sqliteRepository;
            purchaseService = new PerkPurchaseService(
                repository,
                progressResolver,
                perkTree.catalog(),
                new PerkPurchasePolicy(),
                3
            );
            experienceService = new SkillExperienceService(
                repository,
                progressionCurve,
                new ExperiencePolicy(
                    Math.min(config.mining().chunkSoftLimit(),
                        Math.min(config.woodcutting().chunkSoftLimit(), config.digging().chunkSoftLimit())),
                    Math.min(config.mining().chunkHardLimit(),
                        Math.min(config.woodcutting().chunkHardLimit(), config.digging().chunkHardLimit())),
                    Math.min(config.mining().farmFloorMultiplier(),
                        Math.min(config.woodcutting().farmFloorMultiplier(),
                            config.digging().farmFloorMultiplier())),
                    Set.of()
                ),
                new ExperienceGrantGuard(Duration.ofSeconds(5), 16_384),
                3
            );
            newGamePlusService = new NewGamePlusService(repository, progressionCurve, perkTree.catalog(), config.newGamePlus());
            globalExperienceEvent = new GlobalExperienceEvent(new File(plugin.getDataFolder(), "skills/experience-event.yml"));
            administrationService = new SkillAdministrationService(
                sqliteRepository,
                progressionCurve,
                perkTree.catalog(),
                Clock.systemUTC(),
                3
            );
            exportService = new SkillExportService(
                sqliteRepository,
                new File(plugin.getDataFolder(), "skills/exports").toPath(),
                plugin.getDescription().getVersion(),
                Clock.systemUTC()
            );
        } catch (IOException | RuntimeException exception) {
            repository = null;
            purchaseService = null;
            experienceService = null;
            newGamePlusService = null;
            globalExperienceEvent = null;
            administrationService = null;
            exportService = null;
            storageFailure = exception.getMessage();
            plugin.getLogger().severe("Nekara Skills storage is unavailable; progression is locked: "
                + exception.getMessage());
        }
        enabled = true;
        menu.enable();
        nativeGatheringListener = new NativeGatheringListener(plugin, this, config, perkTree);
        nativeGatheringListener.enable();
        gatheringAbilityListener = new GatheringAbilityListener(
            plugin, this, nativeGatheringListener, config, perkTree, messages);
        gatheringAbilityListener.enable();
        gatheringUtilityPerkListener = new GatheringUtilityPerkListener(plugin, this, perkTree);
        gatheringUtilityPerkListener.enable();
        nativeActivityListener = new NativeActivityListener(
            plugin, this, config.activities(), nativeGatheringListener.placedBlocks());
        nativeActivityListener.enable();
        combatPerkListener = new CombatPerkListener(plugin, this);
        combatPerkListener.enable();
        productionPerkListener = new ProductionPerkListener(plugin, this);
        productionPerkListener.enable();
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        generation++;
        ProductionPerkListener closingProductionPerks = productionPerkListener;
        productionPerkListener = null;
        if (closingProductionPerks != null) {
            closingProductionPerks.disable();
        }
        CombatPerkListener closingCombatPerks = combatPerkListener;
        combatPerkListener = null;
        if (closingCombatPerks != null) {
            closingCombatPerks.disable();
        }
        NativeActivityListener closingActivities = nativeActivityListener;
        nativeActivityListener = null;
        if (closingActivities != null) {
            closingActivities.disable();
        }
        GatheringUtilityPerkListener closingUtilityPerks = gatheringUtilityPerkListener;
        gatheringUtilityPerkListener = null;
        if (closingUtilityPerks != null) {
            closingUtilityPerks.disable();
        }
        GatheringAbilityListener closingAbilities = gatheringAbilityListener;
        gatheringAbilityListener = null;
        if (closingAbilities != null) {
            closingAbilities.disable();
        }
        NativeGatheringListener closingGathering = nativeGatheringListener;
        nativeGatheringListener = null;
        if (closingGathering != null) {
            closingGathering.disable();
        }
        menu.disable();
        SkillProfileRepository closing = repository;
        repository = null;
        progressResolver = null;
        purchaseService = null;
        experienceService = null;
        administrationService = null;
        exportService = null;
        pendingPurchases.clear();
        pendingAdminMutations.clear();
        experienceFeedback.clear();
        allExperienceBoosts.clear();
        skillExperienceBoosts.clear();
        profileCache.clear();
        runtimeStateCache.clear();
        experienceQueue.clear();
        queuedExperienceAwards.set(0);
        if (closing != null) {
            try {
                closing.close();
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("Could not close Nekara Skills storage: " + exception.getMessage());
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void openMenu(Player player) {
        loadProfile(player, (profile, snapshot) -> menu.openOverview(player, profile, snapshot));
    }

    public void openPlayerOverview(Player player) {
        loadProfile(player, (profile, snapshot) -> menu.openPlayerOverview(player, profile, snapshot));
    }

    void openSkillTree(Player player, SkillId skill) {
        loadProfile(player, (profile, snapshot) -> menu.openTree(player, profile, snapshot, skill));
    }

    void openSkillTree(Player player, SkillId skill, PerkTreeViewport viewport) {
        loadProfile(player, (profile, snapshot) -> menu.openTree(player, profile, snapshot, skill, viewport));
    }

    void openPerkConfirmation(Player player, PerkId perkId) {
        PerkDefinition perk = perkTree.catalog().require(perkId);
        loadProfile(player, (profile, snapshot) ->
            menu.openConfirmation(player, profile, snapshot, perk));
    }

    void purchasePerk(Player player, PerkId perkId) {
        PerkPurchaseService activeService = purchaseService;
        if (!enabled || activeService == null || !pendingPurchases.add(player.getUniqueId())) {
            if (activeService == null) {
                menu.showUnavailable(player);
            } else {
                player.sendActionBar(Component.text(
                    "Kronika ještě zapisuje předchozí volbu.",
                    NamedTextColor.YELLOW));
            }
            return;
        }
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var result = activeService.purchase(player.getUniqueId().toString(), perkId);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pendingPurchases.remove(player.getUniqueId());
                    if (enabled && generation == requestGeneration && player.isOnline()) {
                        cacheProfile(player.getUniqueId(), result.profile());
                        menu.openTree(
                            player,
                            result.profile(),
                            result.progress(),
                            perkTree.catalog().require(perkId).skill()
                        );
                        menu.showPurchaseStatus(player, result.status());
                    }
                });
            } catch (SkillStorageException | IllegalStateException exception) {
                pendingPurchases.remove(player.getUniqueId());
                handleStorageFailure(player, requestGeneration, exception);
            }
        });
    }

    private void loadProfile(
        Player player,
        BiConsumer<SkillProfile, SkillProgressSnapshot> callback
    ) {
        if (!enabled || repository == null || progressResolver == null) {
            menu.showUnavailable(player);
            return;
        }
        String playerKey = player.getUniqueId().toString();
        SkillProfileRepository activeRepository = repository;
        SkillProgressResolver activeResolver = progressResolver;
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkillProfile profile = activeRepository.find(playerKey)
                    .orElseGet(() -> SkillProfile.empty(playerKey));
                var snapshot = activeResolver.resolve(profile);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (enabled && generation == requestGeneration && player.isOnline()) {
                        cacheProfile(player.getUniqueId(), profile);
                        callback.accept(profile, snapshot);
                    }
                });
            } catch (SkillStorageException | IllegalStateException exception) {
                handleStorageFailure(player, requestGeneration, exception);
            }
        });
    }

    private void handleStorageFailure(Player player, long requestGeneration, RuntimeException exception) {
        handleStorageFailure(player.getUniqueId(), requestGeneration, exception);
    }

    private void handleStorageFailure(UUID playerId, long requestGeneration, RuntimeException exception) {
        if (generation == requestGeneration) {
            storageFailure = exception.getMessage();
            profileCache.remove(playerId);
            plugin.getLogger().severe(
                "Could not access Nekara Skills profile: " + exception.getMessage());
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (enabled && generation == requestGeneration && player != null && player.isOnline()) {
                menu.showUnavailable(player);
            }
        });
    }

    void preloadProfile(Player player) {
        if (!profileCache.containsKey(player.getUniqueId())) {
            loadProfile(player, (profile, snapshot) -> { });
        }
    }

    Optional<SkillProfile> cachedProfile(UUID playerId) {
        return Optional.ofNullable(profileCache.get(playerId));
    }

    int skillLevel(SkillProfile profile, SkillId skill) {
        SkillProgressResolver resolver = progressResolver;
        if (resolver == null) {
            return 0;
        }
        return resolver.resolve(profile).skill(skill).level();
    }

    Optional<SkillRuntimeState> runtimeState(UUID playerId, SkillId skill) {
        SkillProfile profile = profileCache.get(playerId);
        if (profile == null) {
            return Optional.empty();
        }
        RuntimeStateKey key = new RuntimeStateKey(playerId, skill);
        try {
            return Optional.of(runtimeStateCache.computeIfAbsent(key, ignored -> new SkillRuntimeState(
                perkStats.resolve(profile, skill, newGamePlusStatMultiplier(profile, skill)), perkMechanics.resolve(profile, skill))));
        } catch (RuntimeException exception) {
            invalidateProfile(playerId, exception);
            return Optional.empty();
        }
    }

    void forgetProfile(UUID playerId) {
        profileCache.remove(playerId);
        runtimeStateCache.keySet().removeIf(key -> key.playerId().equals(playerId));
        pendingPurchases.remove(playerId);
    }

    void invalidateProfile(UUID playerId, RuntimeException exception) {
        profileCache.remove(playerId);
        runtimeStateCache.keySet().removeIf(key -> key.playerId().equals(playerId));
        storageFailure = exception.getMessage();
        plugin.getLogger().severe(
            "Could not resolve cached Nekara Skills profile: " + exception.getMessage());
    }

    void awardExperience(
        UUID playerId,
        ExperienceAwardRequest request,
        Consumer<ExperienceAwardResult> callback
    ) {
        SkillExperienceService activeService = experienceService;
        if (!enabled || activeService == null) {
            return;
        }
        SkillRuntimeMetrics activeMetrics = runtimeMetrics;
        int queueDepth = queuedExperienceAwards.incrementAndGet();
        activeMetrics.recordSubmitted(Math.min(queueDepth, MAX_QUEUED_EXPERIENCE_AWARDS));
        if (queueDepth > MAX_QUEUED_EXPERIENCE_AWARDS) {
            queuedExperienceAwards.decrementAndGet();
            activeMetrics.recordQueueRejected();
            long now = System.currentTimeMillis();
            long next = nextQueueWarningAt.get();
            if (now >= next && nextQueueWarningAt.compareAndSet(next, now + 60_000L)) {
                plugin.getLogger().warning(
                    "Nekara Skills experience queue is full; dropping awards safely for 60 seconds.");
            }
            return;
        }
        long requestGeneration = generation;
        ExperienceAwardRequest boostedRequest = applyExperienceBoost(playerId, request);
        experienceQueue.add(new PendingExperienceAward(
            playerId, boostedRequest, callback, activeService, activeMetrics,
            System.nanoTime(), requestGeneration));
        scheduleExperienceDrain();
    }

    public void setExperienceBoost(UUID playerId, SkillId skill, double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 1.0 || multiplier > 100.0) {
            throw new IllegalArgumentException("Násobitel musí být od 1 do 100.");
        }
        if (skill == null) allExperienceBoosts.put(playerId, multiplier);
        else skillExperienceBoosts.put(new ExperienceBoostKey(playerId, skill), multiplier);
    }

    public void clearExperienceBoost(UUID playerId, SkillId skill) {
        if (skill == null) allExperienceBoosts.remove(playerId);
        else skillExperienceBoosts.remove(new ExperienceBoostKey(playerId, skill));
    }

    public double experienceBoost(UUID playerId, SkillId skill) {
        return skillExperienceBoosts.getOrDefault(new ExperienceBoostKey(playerId, skill),
            allExperienceBoosts.getOrDefault(playerId, 1.0));
    }

    public double allExperienceBoost(UUID playerId) {
        return allExperienceBoosts.getOrDefault(playerId, 1.0);
    }

    public Map<SkillId, Double> skillExperienceBoosts(UUID playerId) {
        Map<SkillId, Double> result = new java.util.EnumMap<>(SkillId.class);
        skillExperienceBoosts.forEach((key, value) -> {
            if (key.playerId().equals(playerId)) {
                result.put(key.skill(), value);
            }
        });
        return Map.copyOf(result);
    }

    private ExperienceAwardRequest applyExperienceBoost(UUID playerId, ExperienceAwardRequest request) {
        SkillProfile profile = profileCache.get(playerId);
        GlobalExperienceEvent event = globalExperienceEvent;
        double multiplier = experienceBoost(playerId, request.skill())
            * (event == null ? 1.0 : event.multiplier(request.skill()))
            * (profile == null ? 1.0 : newGamePlusExperienceMultiplier(profile, request.skill()));
        if (multiplier == 1.0) return request;
        long boosted = Math.max(1L, Math.min(100_000_000L,
            Math.round(request.baseExperience() * multiplier)));
        return new ExperienceAwardRequest(request.playerKey(), request.skill(), boosted,
            request.context(), request.fingerprint());
    }

    public void startExperienceEvent(SkillId skill, double multiplier, long durationMillis) {
        if (durationMillis < 60_000L || durationMillis > 604_800_000L) throw new IllegalArgumentException("Délka musí být od 1 minuty do 7 dní.");
        GlobalExperienceEvent event = globalExperienceEvent;
        if (event == null) throw new IllegalStateException("Úložiště XP události není dostupné.");
        event.start(skill, multiplier, Math.addExact(System.currentTimeMillis(), durationMillis));
    }
    public void stopExperienceEvent() { if (globalExperienceEvent != null) globalExperienceEvent.stop(); }
    public GlobalExperienceEvent globalExperienceEvent() { return globalExperienceEvent; }

    void openNewGamePlusConfirmation(Player player, SkillId skill) {
        loadProfile(player, (profile, snapshot) -> {
            if (canUseNewGamePlus(profile, skill)) {
                menu.openNewGamePlusConfirmation(player, profile, snapshot, skill);
            } else {
                menu.openTree(player, profile, snapshot, skill);
                menu.showNewGamePlusStatus(player, skill, new NewGamePlusResult(
                    profile.newGamePlusRank(skill) >= 1
                        ? cz.nekara.rpg.skills.newgameplus.NewGamePlusStatus.MAXIMUM_RANK_REACHED
                        : cz.nekara.rpg.skills.newgameplus.NewGamePlusStatus.NOT_MAX_LEVEL,
                    profile, snapshot, 0));
            }
        });
    }

    void requestNewGamePlus(Player player, SkillId skill) {
        NewGamePlusService activeService = newGamePlusService;
        if (!enabled || activeService == null || !pendingPurchases.add(player.getUniqueId())) {
            return;
        }
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                NewGamePlusResult result = activeService.rebirth(player.getUniqueId().toString(), skill);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pendingPurchases.remove(player.getUniqueId());
                    if (enabled && generation == requestGeneration && player.isOnline()) {
                        cacheProfile(player.getUniqueId(), result.profile());
                        menu.openTree(player, result.profile(), result.progress(), skill);
                        menu.showNewGamePlusStatus(player, skill, result);
                    }
                });
            } catch (RuntimeException exception) {
                pendingPurchases.remove(player.getUniqueId());
                handleStorageFailure(player, requestGeneration, exception);
            }
        });
    }

    boolean canUseNewGamePlus(SkillProfile profile, SkillId skill) {
        SkillsConfig config = activeConfig;
        return config != null && config.newGamePlus().enabled()
            && skillLevel(profile, skill) >= SkillProgressionCurve.DEFAULT_MAX_LEVEL
            && profile.newGamePlusRank(skill) == 0;
    }

    double newGamePlusExperienceMultiplier(SkillProfile profile, SkillId skill) {
        SkillsConfig config = activeConfig;
        if (config == null) return 1.0;
        var rules = config.newGamePlus();
        return profile.newGamePlusRank(skill) == 0 ? 1.0 : rules.experienceMultiplier();
    }

    double newGamePlusStatBonus(SkillProfile profile, SkillId skill) {
        SkillsConfig config = activeConfig;
        return config == null ? 0.0 : config.newGamePlus().perkStatBonusPerRank() * profile.newGamePlusRank(skill);
    }

    private double newGamePlusStatMultiplier(SkillProfile profile, SkillId skill) {
        SkillsConfig config = activeConfig;
        if (config == null) return 1.0;
        return 1.0 + newGamePlusStatBonus(profile, skill);
    }

    void showExperienceFeedback(
        UUID playerId,
        SkillId skill,
        String source,
        ExperienceAwardResult result
    ) {
        if (!enabled) {
            return;
        }
        experienceFeedback.record(playerId, skill, source, result);
    }

    public boolean isExperienceFeedbackVisible(UUID playerId) {
        return enabled && experienceFeedback.isDisplaying(playerId);
    }

    private void cacheProfile(UUID playerId, SkillProfile profile) {
        SkillProfile previous = profileCache.get(playerId);
        SkillProfile selected = profileCache.compute(playerId, (ignored, current) ->
            current == null || profile.revision() >= current.revision() ? profile : current);
        boolean perksChanged = selected == profile
            && (previous == null || !previous.perkRanks().equals(profile.perkRanks()));
        if (!perksChanged) {
            return;
        }
        runtimeStateCache.keySet().removeIf(key -> key.playerId().equals(playerId));
        NativeGatheringListener gathering = nativeGatheringListener;
        Player player = Bukkit.getPlayer(playerId);
        if (gathering != null && player != null && player.isOnline()) {
            gathering.refreshPlayer(player);
        }
        CombatPerkListener combat = combatPerkListener;
        if (combat != null && player != null && player.isOnline()) {
            combat.refreshPlayer(player);
        }
    }

    public AdminDispatchStatus inspectAdmin(
        String playerKey,
        Consumer<SkillAdminInspection> success,
        Consumer<RuntimeException> failure
    ) {
        SkillAdministrationService activeService = administrationService;
        if (!enabled) {
            return AdminDispatchStatus.MODULE_DISABLED;
        }
        if (activeService == null) {
            return AdminDispatchStatus.STORAGE_UNAVAILABLE;
        }
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkillAdminInspection inspection = activeService.inspect(playerKey);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (enabled && generation == requestGeneration) {
                        success.accept(inspection);
                    }
                });
            } catch (RuntimeException exception) {
                handleAdminFailure(requestGeneration, exception, failure);
            }
        });
        return AdminDispatchStatus.STARTED;
    }

    public AdminDispatchStatus executeAdmin(
        SkillAdminActor actor,
        UUID targetId,
        String targetName,
        SkillAdminOperation operation,
        Consumer<SkillAdminResult> success,
        Consumer<RuntimeException> failure
    ) {
        SkillAdministrationService activeService = administrationService;
        if (!enabled) {
            return AdminDispatchStatus.MODULE_DISABLED;
        }
        if (activeService == null) {
            return AdminDispatchStatus.STORAGE_UNAVAILABLE;
        }
        if (!pendingAdminMutations.add(targetId)) {
            return AdminDispatchStatus.BUSY;
        }
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkillAdminResult result = activeService.execute(
                    actor, targetId.toString(), targetName, operation);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pendingAdminMutations.remove(targetId);
                    if (!enabled || generation != requestGeneration) {
                        return;
                    }
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null && target.isOnline()) {
                        cacheProfile(targetId, result.profile());
                    }
                    success.accept(result);
                });
            } catch (RuntimeException exception) {
                pendingAdminMutations.remove(targetId);
                handleAdminFailure(requestGeneration, exception, failure);
            }
        });
        return AdminDispatchStatus.STARTED;
    }

    public List<String> adminPerkIds() {
        return SkillId.gameplaySkills().stream()
            .flatMap(skill -> perkTree.catalog().forSkill(skill).stream())
            .map(perk -> perk.id().value())
            .sorted()
            .toList();
    }

    public int adminPerkMaxRank(PerkId perkId) {
        return perkTree.catalog().require(perkId).maxRank();
    }

    public AdminDispatchStatus exportAdmin(
        Consumer<SkillExportResult> success,
        Consumer<RuntimeException> failure
    ) {
        SkillExportService activeService = exportService;
        if (!enabled) {
            return AdminDispatchStatus.MODULE_DISABLED;
        }
        if (activeService == null) {
            return AdminDispatchStatus.STORAGE_UNAVAILABLE;
        }
        if (!exportInProgress.compareAndSet(false, true)) {
            return AdminDispatchStatus.BUSY;
        }
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkillExportResult result = activeService.export();
                exportInProgress.set(false);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (enabled && generation == requestGeneration) {
                        success.accept(result);
                    }
                });
            } catch (IOException | RuntimeException exception) {
                exportInProgress.set(false);
                plugin.getLogger().severe(
                    "Could not export Nekara Skills data: " + exception.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (enabled && generation == requestGeneration) {
                        failure.accept(exception instanceof RuntimeException runtime
                            ? runtime
                            : new SkillStorageException("Could not export Nekara Skills data", exception));
                    }
                });
            }
        });
        return AdminDispatchStatus.STARTED;
    }

    public SkillRuntimeMetricsSnapshot runtimeMetrics() {
        return runtimeMetrics.snapshot(queuedExperienceAwards.get());
    }

    private void handleAdminFailure(
        long requestGeneration,
        RuntimeException exception,
        Consumer<RuntimeException> failure
    ) {
        if (generation == requestGeneration) {
            storageFailure = exception.getMessage();
            plugin.getLogger().severe(
                "Could not execute Nekara Skills administrative operation: " + exception.getMessage());
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (enabled && generation == requestGeneration) {
                failure.accept(exception);
            }
        });
    }

    public String storageStatus() {
        if (!enabled) {
            return "vypnuto";
        }
        return repository == null ? "uzamčeno" : "připraveno";
    }

    public String storageFailure() {
        return storageFailure;
    }

    public enum AdminDispatchStatus {
        STARTED,
        MODULE_DISABLED,
        STORAGE_UNAVAILABLE,
        BUSY
    }

    private void scheduleExperienceDrain() {
        if (!experienceDrainScheduled.compareAndSet(false, true)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::drainExperienceQueue);
    }

    private void drainExperienceQueue() {
        List<CompletedExperienceAward> completed = new java.util.ArrayList<>();
        try {
            for (int processed = 0; processed < EXPERIENCE_DRAIN_BATCH; processed++) {
                PendingExperienceAward pending = experienceQueue.poll();
                if (pending == null) {
                    break;
                }
                queuedExperienceAwards.updateAndGet(value -> Math.max(0, value - 1));
                try {
                    ExperienceAwardResult result = pending.service().award(pending.request());
                    pending.metrics().recordCompleted(
                        result, System.nanoTime() - pending.enqueuedAtNanos());
                    completed.add(new CompletedExperienceAward(pending, result, null));
                } catch (SkillStorageException | IllegalStateException exception) {
                    pending.metrics().recordFailure(
                        System.nanoTime() - pending.enqueuedAtNanos());
                    completed.add(new CompletedExperienceAward(pending, null, exception));
                }
            }
        } finally {
            experienceDrainScheduled.set(false);
        }
        if (!completed.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> completeExperienceAwards(completed));
        }
        if (!experienceQueue.isEmpty()) {
            scheduleExperienceDrain();
        }
    }

    private void completeExperienceAwards(List<CompletedExperienceAward> completed) {
        for (CompletedExperienceAward entry : completed) {
            PendingExperienceAward pending = entry.pending();
            if (entry.failure() != null) {
                handleStorageFailure(pending.playerId(), pending.generation(), entry.failure());
                continue;
            }
            if (!enabled || generation != pending.generation()) {
                continue;
            }
            Player player = Bukkit.getPlayer(pending.playerId());
            if (player != null && player.isOnline()) {
                entry.result().profile().ifPresent(profile -> cacheProfile(pending.playerId(), profile));
            }
            pending.callback().accept(entry.result());
        }
    }

    private record RuntimeStateKey(UUID playerId, SkillId skill) {
    }

    private record ExperienceBoostKey(UUID playerId, SkillId skill) {
    }

    private record PendingExperienceAward(
        UUID playerId,
        ExperienceAwardRequest request,
        Consumer<ExperienceAwardResult> callback,
        SkillExperienceService service,
        SkillRuntimeMetrics metrics,
        long enqueuedAtNanos,
        long generation
    ) {
    }

    private record CompletedExperienceAward(
        PendingExperienceAward pending,
        ExperienceAwardResult result,
        RuntimeException failure
    ) {
    }
}
