package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.GatheringSkillConfig;
import cz.nekara.rpg.configuration.NativeGatheringConfig;
import cz.nekara.rpg.configuration.SkillsConfig;
import cz.nekara.rpg.modules.skills.GatheringMaterialPolicy.GatheringTool;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillPresentation;
import cz.nekara.rpg.skills.combat.RandomChanceRoller;
import cz.nekara.rpg.skills.luck.LuckChanceResolver;
import cz.nekara.rpg.skills.experience.ChunkActivityTracker;
import cz.nekara.rpg.skills.experience.ExperienceAwardRequest;
import cz.nekara.rpg.skills.experience.ExperienceContext;
import cz.nekara.rpg.skills.experience.ExperienceFingerprint;
import cz.nekara.rpg.skills.experience.ExperienceGrantGuard;
import cz.nekara.rpg.skills.experience.RecentActionGuard;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.MechanicId;
import cz.nekara.rpg.skills.perks.PerkMechanicResolver;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.rewards.DropMultiplierResolver;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.stats.StatId;
import cz.nekara.rpg.skills.stats.StatSnapshot;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

final class NativeGatheringListener implements Listener {
    private static final Duration FINGERPRINT_TTL = Duration.ofSeconds(5);
    private static final Duration PLAYER_INPUT_TTL = Duration.ofSeconds(30);
    private static final int TRACKED_FINGERPRINTS = 32_768;
    private static final int TRACKED_CHUNKS = 16_384;

    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final List<Definition> definitions;
    private final Map<SkillId, ChunkActivityTracker> chunkActivity = new EnumMap<>(SkillId.class);
    private final PlacedBlockTracker placedBlocks;
    private final ExperienceGrantGuard rewardGuard = new ExperienceGrantGuard(
        FINGERPRINT_TTL, TRACKED_FINGERPRINTS);
    private final RecentActionGuard physicalInputs = new RecentActionGuard(PLAYER_INPUT_TTL);
    private final RecentActionGuard validatedBreaks = new RecentActionGuard(FINGERPRINT_TTL);
    private final PerkStatResolver perkStats;
    private final PerkMechanicResolver perkMechanics;
    private final DropMultiplierResolver dropMultiplier = new DropMultiplierResolver();
    private final NamespacedKey speedModifierKey;
    private final NamespacedKey proficiencyMobilityKey;
    private final Set<UUID> automatedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> proficiencyWarnings = new ConcurrentHashMap<>();
    private final Set<String> pendingSuspiciousRestores = ConcurrentHashMap.newKeySet();
    private boolean enabled;

    NativeGatheringListener(
        NekaraRPGPlugin plugin,
        SkillsModule module,
        SkillsConfig config,
        DefaultPerkTree perkTree
    ) {
        this.plugin = plugin;
        this.module = module;
        this.placedBlocks = new PlacedBlockTracker(plugin);
        this.perkStats = new PerkStatResolver(perkTree.catalog());
        this.perkMechanics = new PerkMechanicResolver(perkTree.catalog());
        this.speedModifierKey = new NamespacedKey(plugin, "skills_gathering_speed");
        this.proficiencyMobilityKey = new NamespacedKey(plugin, "skills_tool_proficiency_mobility");
        this.definitions = List.of(
            new Definition(SkillId.MINING, config.mining(), GatheringTool.PICKAXE, true,
                StatId.MINING_SPEED, null),
            new Definition(SkillId.WOODCUTTING, config.woodcutting(), GatheringTool.AXE, true,
                StatId.WOODCUTTING_SPEED, MechanicId.RARE_LEAF_DROPS),
            new Definition(SkillId.DIGGING, config.digging(), GatheringTool.SHOVEL, false,
                StatId.DIGGING_SPEED, null)
        );
        for (Definition definition : definitions) {
            chunkActivity.put(definition.skill(), new ChunkActivityTracker(
                Duration.ofSeconds(definition.config().chunkWindowSeconds()), TRACKED_CHUNKS));
        }
    }

    void enable() {
        if (enabled) {
            return;
        }
        placedBlocks.enable();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
        plugin.getServer().getOnlinePlayers().forEach(module::preloadProfile);
    }

    void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        HandlerList.unregisterAll(this);
        placedBlocks.disable();
        chunkActivity.values().forEach(ChunkActivityTracker::clear);
        physicalInputs.clear();
        validatedBreaks.clear();
        automatedPlayers.clear();
        pendingSuspiciousRestores.clear();
        Bukkit.getOnlinePlayers().forEach(player -> {
            removeSpeedModifier(player);
            removeProficiencyMobility(player);
        });
    }

    PlacedBlockTracker placedBlocks() {
        return placedBlocks;
    }

    void beginAutomatedBreaks(UUID playerId) {
        automatedPlayers.add(playerId);
    }

    void endAutomatedBreaks(UUID playerId) {
        automatedPlayers.remove(playerId);
    }

    @EventHandler
    public void preloadProfile(PlayerJoinEvent event) {
        module.preloadProfile(event.getPlayer());
    }

    @EventHandler
    public void forgetProfile(PlayerQuitEvent event) {
        String playerKey = event.getPlayer().getUniqueId().toString();
        physicalInputs.forget(playerKey);
        validatedBreaks.forget(playerKey);
        removeSpeedModifier(event.getPlayer());
        removeProficiencyMobility(event.getPlayer());
        proficiencyWarnings.remove(event.getPlayer().getUniqueId());
        module.forgetProfile(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void recordPhysicalInput(BlockDamageEvent event) {
        if (!enabled || automatedPlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlock();
        definition(block.getType(), player.getInventory().getItemInMainHand()).ifPresent(definition -> {
            if (plugin.configuration().get().worlds().isEnabled(block.getWorld().getName())) {
                physicalInputs.record(player.getUniqueId().toString(),
                    actionKey(definition.skill(), sourceKey(block, block.getType())));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void instantlyBreakLeaves(BlockDamageEvent event) {
        if (!enabled || !GatheringMaterialPolicy.isLeaves(event.getBlock().getType())
            || !GatheringMaterialPolicy.suitableTool(GatheringTool.AXE, event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }
        module.runtimeState(event.getPlayer().getUniqueId(), SkillId.WOODCUTTING).ifPresent(state -> {
            if (state.has(MechanicId.INSTANT_LEAF_BREAK)) {
                event.setInstaBreak(true);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void restoreSuspiciousBlockAfterBrushing(PlayerInteractEvent event) {
        if (!enabled || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
            || event.getMaterial() != Material.BRUSH || !isSuspiciousBlock(event.getClickedBlock().getType())) {
            return;
        }
        Player player = event.getPlayer();
        if (unsupportedMode(player) || !plugin.configuration().get().worlds().isEnabled(player.getWorld().getName())
            || !module.runtimeState(player.getUniqueId(), SkillId.DIGGING)
                .map(state -> state.has(MechanicId.SUSPICIOUS_BLOCK_RESTORATION)).orElse(false)
            || !roll(0.20)) {
            return;
        }
        Block block = event.getClickedBlock();
        Material original = block.getType();
        String key = sourceKey(block, original);
        if (!pendingSuspiciousRestores.add(key)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingSuspiciousRestores.remove(key);
            if (enabled && block.getType() != original && isUnderlyingSuspiciousBlock(block.getType(), original)) {
                block.setType(original, false);
            }
        }, 110L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void awardCompletedBreak(BlockBreakEvent event) {
        if (!enabled || automatedPlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Block block = event.getBlock();
        Player player = event.getPlayer();
        Material material = block.getType();
        Optional<Definition> found = definition(material, player.getInventory().getItemInMainHand());
        if (found.isEmpty()
            || !plugin.configuration().get().worlds().isEnabled(block.getWorld().getName())) {
            return;
        }
        Definition definition = found.get();
        String sourceKey = sourceKey(block, material);
        String actionKey = actionKey(definition.skill(), sourceKey);
        String playerKey = player.getUniqueId().toString();
        if (!physicalInputs.consume(playerKey, actionKey)) {
            return;
        }
        validatedBreaks.record(playerKey, actionKey);
        long baseExperience = definition.config().experience(material);
        if (!definition.config().experienceEnabled() || baseExperience < 1) {
            return;
        }

        GatheringBreak fact = new GatheringBreak(
            player.getUniqueId(), definition.skill(), block.getLocation(), material,
            baseExperience, placedBlocks.isPlayerPlaced(block),
            player.getGameMode() == GameMode.CREATIVE,
            player.getGameMode() == GameMode.SPECTATOR,
            chunkKey(block), sourceKey);
        plugin.getServer().getScheduler().runTask(plugin, () -> finishBreak(fact));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void applyFinalDropPerks(BlockDropItemEvent event) {
        if (!enabled || automatedPlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        Material material = event.getBlockState().getType();
        Block block = event.getBlockState().getBlock();
        Optional<Definition> found = definition(material, player.getInventory().getItemInMainHand());
        if (found.isEmpty() || placedBlocks.isPlayerPlaced(block)
            || unsupportedMode(player)
            || !plugin.configuration().get().worlds().isEnabled(block.getWorld().getName())) {
            return;
        }
        Definition definition = found.get();
        String sourceKey = sourceKey(block, material);
        String actionKey = actionKey(definition.skill(), sourceKey);
        if (!validatedBreaks.consume(player.getUniqueId().toString(), actionKey)
            || !rewardGuard.tryAcquire(fingerprint(player.getUniqueId(), definition.skill(),
                "gathering_drop", sourceKey))) {
            return;
        }
        Optional<SkillProfile> cached = module.cachedProfile(player.getUniqueId());
        if (cached.isEmpty()) {
            module.preloadProfile(player);
            return;
        }

        SkillProfile profile = cached.get();
        StatSnapshot stats;
        try {
            stats = perkStats.resolve(profile, definition.skill(),
                module.newGamePlusStatMultiplier(profile, definition.skill()));
        } catch (RuntimeException exception) {
            module.invalidateProfile(player.getUniqueId(), exception);
            return;
        }

        List<BonusDrop> bonusDrops = new ArrayList<>();
        int level = module.skillLevel(profile, definition.skill());
        boolean eligibleDoubleDrop = definition.skill() != SkillId.WOODCUTTING
            || GatheringMaterialPolicy.isLog(material);
        if (eligibleDoubleDrop && definition.config().finalDropMultiplierEnabled()
            && definition.config().experience(material) > 0 && !event.getItems().isEmpty()) {
            int multiplier = dropMultiplier.resolve(
                stats,
                module.innateGatheringDoubleDropChance(profile, definition.skill()),
                new RandomChanceRoller(ThreadLocalRandom.current()));
            bonusDrops.addAll(createBonusDrops(event.getItems(), multiplier - 1));
        }
        if (definition.skill() == SkillId.WOODCUTTING && GatheringMaterialPolicy.isLog(material)) {
            awardVanillaExperience(player, stats.value(StatId.WOODCUTTING_LOG_EXPERIENCE));
            if (roll(module.woodcuttingNewGamePlusBonusDropChance(profile))) {
                bonusDrops.addAll(createBonusDrops(event.getItems(), 1));
            }
        }
        if (definition.skill() == SkillId.DIGGING) {
            awardVanillaExperience(player, stats.value(StatId.DIGGING_BLOCK_EXPERIENCE));
            if (roll(module.diggingNewGamePlusBonusDropChance(profile))) {
                bonusDrops.addAll(createBonusDrops(event.getItems(), 1));
            }
        }
        if (definition.skill() == SkillId.MINING) {
            awardVanillaExperience(player, stats.value(StatId.MINING_BLOCK_EXPERIENCE));
        }
        if (definition.skill() == SkillId.WOODCUTTING && GatheringMaterialPolicy.isLeaves(material)
            && perkMechanics.has(profile, SkillId.WOODCUTTING, MechanicId.GOLDEN_LEAVES)) {
            awardVanillaExperience(player, stats.value(StatId.WOODCUTTING_LEAF_EXPERIENCE));
            if (roll(stats.value(StatId.GOLDEN_LEAF_APPLE_CHANCE))) {
                bonusDrops.add(new BonusDrop(block.getLocation().add(0.5, 0.5, 0.5),
                    new ItemStack(Material.GOLDEN_APPLE, 1)));
            }
        }
        try {
            rareDrop(definition, profile, level, stats, material, block.getLocation())
                .ifPresent(bonusDrops::add);
        } catch (RuntimeException exception) {
            module.invalidateProfile(player.getUniqueId(), exception);
            return;
        }
        if (bonusDrops.isEmpty()) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!enabled) {
                return;
            }
            for (BonusDrop bonus : bonusDrops) {
                bonus.location().getWorld().dropItemNaturally(bonus.location(), bonus.item());
            }
        });
    }

    @EventHandler
    public void refreshHeldSlot(PlayerItemHeldEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void refreshSwappedHands(PlayerSwapHandItemsEvent event) {
        scheduleRefresh(event.getPlayer());
    }

    @EventHandler
    public void refreshInventory(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            scheduleRefresh(player);
        }
    }

    void refreshPlayer(Player player) {
        if (!enabled || !player.isOnline()) {
            return;
        }
        removeSpeedModifier(player);
        removeProficiencyMobility(player);
        Optional<Definition> held = definitionForTool(player.getInventory().getItemInMainHand());
        Optional<SkillProfile> profile = module.cachedProfile(player.getUniqueId());
        if (profile.isEmpty()) {
            module.preloadProfile(player);
            return;
        }
        double modifier = 0.0;
        try {
            if (held.isPresent()) {
                double multiplier = perkStats.resolve(profile.get(), held.get().skill(),
                    module.newGamePlusStatMultiplier(profile.get(), held.get().skill()))
                    .value(held.get().speedStat());
                modifier += multiplier - 1.0;
            }
        } catch (RuntimeException exception) {
            module.invalidateProfile(player.getUniqueId(), exception);
            return;
        }
        EquipmentProficiencyPolicy.heldTool(player.getInventory().getItemInMainHand()).ifPresent(requirement -> {
            int currentLevel = module.skillLevel(profile.get(), requirement.skill());
            if (currentLevel < requirement.requiredLevel()) {
                warn(player, requirement, "Tento nástroj ještě neumíš používat");
            }
        });
        Optional<EquipmentProficiencyPolicy.Requirement> requirement =
            EquipmentProficiencyPolicy.heldTool(player.getInventory().getItemInMainHand());
        if (requirement.isPresent()
            && module.skillLevel(profile.get(), requirement.get().skill()) < requirement.get().requiredLevel()) {
            modifier += EquipmentProficiencyPolicy.toolBreakSpeedModifier();
            addProficiencyMobility(player, EquipmentProficiencyPolicy.toolMovementPenalty());
        } else {
            proficiencyWarnings.remove(player.getUniqueId());
        }
        AttributeInstance speed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (speed != null && modifier != 0.0) {
            speed.addTransientModifier(new AttributeModifier(
                speedModifierKey, modifier, AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    private void finishBreak(GatheringBreak fact) {
        if (!enabled || fact.location().getBlock().getType() == fact.material()
            || fact.creative() || fact.spectator() || fact.playerPlaced()) {
            return;
        }
        Definition definition = definition(fact.skill());
        int recentAwards = chunkActivity.get(fact.skill()).reserveAward(fact.chunk());
        ExperienceContext context = new ExperienceContext(
            fact.skill(), false, fact.creative(), fact.spectator(), false,
            fact.playerPlaced(), false, recentAwards);
        ExperienceAwardRequest request = new ExperienceAwardRequest(
            fact.playerId().toString(), fact.skill(), fact.baseExperience(), context,
            fingerprint(fact.playerId(), fact.skill(), "block_break", fact.sourceKey()));
        module.awardExperience(fact.playerId(), request,
            result -> module.showExperienceFeedback(fact.playerId(), fact.skill(),
                ExperienceSourcePresentation.gathering(fact.material()), result));
    }

    private Optional<BonusDrop> rareDrop(
        Definition definition,
        SkillProfile profile,
        int level,
        StatSnapshot stats,
        Material source,
        Location location
    ) {
        if (!(definition.config() instanceof NativeGatheringConfig gathering)
            || !gathering.rareDropsEnabled()
            || gathering.rareDropWeights().isEmpty()) {
            return Optional.empty();
        }
        if (definition.skill() == SkillId.WOODCUTTING
            && (!GatheringMaterialPolicy.isLeaves(source)
                || definition.rareMechanic() == null
                || !perkMechanics.has(profile, definition.skill(), definition.rareMechanic()))) {
            return Optional.empty();
        }
        double chance = stats.value(StatId.RARE_DROP_CHANCE);
        if (definition.skill() == SkillId.DIGGING) {
            chance += plugin.configuration().get().skills().levelRewards()
                .diggingRareDropChance(level);
        }
        var luck = plugin.configuration().get().skills().luck();
        chance = LuckChanceResolver.rareLootChance(chance, module.globalLuck(profile),
            luck.maximumPoints(), luck.rareLootChanceBonusPerPoint());
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return Optional.empty();
        }
        Map<Material, Integer> weights = new EnumMap<>(Material.class);
        weights.putAll(gathering.rareDropWeightsFor(source));
        if (definition.skill() == SkillId.DIGGING
            && isArchaeologySource(source)
            && perkMechanics.has(profile, SkillId.DIGGING, MechanicId.ARCHAEOLOGY_FINDS)) {
            weights.merge(Material.BRICK, 3, Integer::sum);
            weights.merge(Material.ECHO_SHARD, 1, Integer::sum);
        }
        Material selected = selectWeighted(weights);
        return Optional.of(new BonusDrop(location.clone().add(0.5, 0.5, 0.5),
            new ItemStack(selected, 1)));
    }

    private Optional<Definition> definition(Material material, ItemStack tool) {
        return definitions.stream()
            .filter(definition -> !definition.toolRequired()
                || GatheringMaterialPolicy.suitableTool(definition.tool(), tool))
            .filter(definition -> definition.config().experience(material) > 0
                || definition.skill() == SkillId.WOODCUTTING
                    && GatheringMaterialPolicy.isLeaves(material))
            .findFirst();
    }

    private Optional<Definition> definitionForTool(ItemStack tool) {
        return definitions.stream()
            .filter(definition -> GatheringMaterialPolicy.suitableTool(definition.tool(), tool))
            .findFirst();
    }

    private Definition definition(SkillId skill) {
        return definitions.stream().filter(value -> value.skill() == skill).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown gathering skill: " + skill));
    }

    private void scheduleRefresh(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> refreshPlayer(player));
    }

    private void removeSpeedModifier(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
        if (speed != null) {
            speed.removeModifier(speedModifierKey);
        }
    }

    private void addProficiencyMobility(Player player, double penalty) {
        AttributeInstance movement = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement != null && penalty != 0.0) {
            movement.addTransientModifier(new AttributeModifier(
                proficiencyMobilityKey, penalty, AttributeModifier.Operation.ADD_SCALAR));
        }
    }

    private void removeProficiencyMobility(Player player) {
        AttributeInstance movement = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movement != null) {
            movement.removeModifier(proficiencyMobilityKey);
        }
    }

    private void warn(Player player, EquipmentProficiencyPolicy.Requirement requirement, String itemType) {
        String key = requirement.skill().id() + ":" + requirement.requiredLevel();
        if (key.equals(proficiencyWarnings.put(player.getUniqueId(), key))) {
            return;
        }
        player.sendActionBar(net.kyori.adventure.text.Component.text(itemType + ": vyžaduje "
            + SkillPresentation.czechName(requirement.skill()) + " level " + requirement.requiredLevel() + ".",
            net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    private static List<BonusDrop> createBonusDrops(List<Item> drops, int additionalCopies) {
        if (additionalCopies < 1) {
            return List.of();
        }
        List<BonusDrop> bonus = new ArrayList<>();
        for (Item dropped : drops) {
            ItemStack original = dropped.getItemStack();
            long remaining = (long) original.getAmount() * additionalCopies;
            int maximum = Math.max(1, original.getMaxStackSize());
            while (remaining > 0) {
                ItemStack copy = original.clone();
                int amount = (int) Math.min(maximum, remaining);
                copy.setAmount(amount);
                bonus.add(new BonusDrop(dropped.getLocation().clone(), copy));
                remaining -= amount;
            }
        }
        return List.copyOf(bonus);
    }

    private void awardVanillaExperience(Player player, double amount) {
        int experience = (int) Math.round(amount);
        if (experience < 1) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (enabled && player.isOnline()) {
                player.giveExp(experience);
            }
        });
    }

    private static boolean roll(double chance) {
        return chance > 0.0 && ThreadLocalRandom.current().nextDouble() < chance;
    }

    private static boolean isSuspiciousBlock(Material material) {
        return material == Material.SUSPICIOUS_SAND || material == Material.SUSPICIOUS_GRAVEL;
    }

    private static boolean isArchaeologySource(Material material) {
        return material == Material.SAND || material == Material.RED_SAND || material == Material.GRAVEL;
    }

    private static boolean isUnderlyingSuspiciousBlock(Material material, Material original) {
        return original == Material.SUSPICIOUS_SAND && material == Material.SAND
            || original == Material.SUSPICIOUS_GRAVEL && material == Material.GRAVEL;
    }

    private static Material selectWeighted(Map<Material, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = ThreadLocalRandom.current().nextInt(total);
        for (Map.Entry<Material, Integer> entry : weights.entrySet()) {
            roll -= entry.getValue();
            if (roll < 0) {
                return entry.getKey();
            }
        }
        throw new IllegalStateException("Weighted material table is empty");
    }

    private static boolean unsupportedMode(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
            || player.getGameMode() == GameMode.SPECTATOR;
    }

    private static ChunkActivityTracker.ChunkKey chunkKey(Block block) {
        return new ChunkActivityTracker.ChunkKey(
            block.getWorld().getUID(), block.getChunk().getX(), block.getChunk().getZ());
    }

    private static ExperienceFingerprint fingerprint(
        UUID playerId,
        SkillId skill,
        String type,
        String sourceKey
    ) {
        return new ExperienceFingerprint(playerId.toString(), skill, type, sourceKey);
    }

    private static String actionKey(SkillId skill, String sourceKey) {
        return skill.id() + ":" + sourceKey;
    }

    private static String sourceKey(Block block, Material material) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":"
            + block.getZ() + ":" + material.getKey().asString();
    }

    private record Definition(
        SkillId skill,
        GatheringSkillConfig config,
        GatheringTool tool,
        boolean toolRequired,
        StatId speedStat,
        MechanicId rareMechanic
    ) {
    }

    private record GatheringBreak(
        UUID playerId,
        SkillId skill,
        Location location,
        Material material,
        long baseExperience,
        boolean playerPlaced,
        boolean creative,
        boolean spectator,
        ChunkActivityTracker.ChunkKey chunk,
        String sourceKey
    ) {
        private GatheringBreak {
            location = location.clone();
        }
    }

    private record BonusDrop(Location location, ItemStack item) {
    }
}
