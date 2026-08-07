package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.GatheringAbilityConfig;
import cz.nekara.rpg.configuration.SkillsConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.skills.GatheringMaterialPolicy.GatheringTool;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.abilities.AbilityExecutionContext;
import cz.nekara.rpg.skills.abilities.AbilityExecutionPolicy;
import cz.nekara.rpg.skills.abilities.BoundedGraphSearch;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.MechanicId;
import cz.nekara.rpg.skills.perks.PerkMechanicResolver;
import cz.nekara.rpg.skills.stats.PerkStatResolver;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.block.Action;
import org.bukkit.scheduler.BukkitTask;

final class GatheringAbilityListener implements Listener {
    private static final int[][] ADJACENT_26 = adjacentOffsets();
    private static final double MAXIMUM_TNT_POWER = 6.0;

    private final NekaraRPGPlugin plugin;
    private final SkillsModule module;
    private final NativeGatheringListener gathering;
    private final SkillsConfig config;
    private final MessageService messages;
    private final PerkMechanicResolver mechanics;
    private final PerkStatResolver stats;
    private final Map<CooldownKey, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, TimedAbilityWindow> treeFellerWindows = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Set<UUID> activePlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, EnhancedTnt> enhancedTnt = new HashMap<>();
    private boolean enabled;

    GatheringAbilityListener(
        NekaraRPGPlugin plugin,
        SkillsModule module,
        NativeGatheringListener gathering,
        SkillsConfig config,
        DefaultPerkTree perkTree,
        MessageService messages
    ) {
        this.plugin = plugin;
        this.module = module;
        this.gathering = gathering;
        this.config = config;
        this.messages = messages;
        this.mechanics = new PerkMechanicResolver(perkTree.catalog());
        this.stats = new PerkStatResolver(perkTree.catalog());
    }

    void enable() {
        if (enabled) {
            return;
        }
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
    }

    void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        HandlerList.unregisterAll(this);
        activeTasks.values().forEach(BukkitTask::cancel);
        activeTasks.clear();
        activePlayers.clear();
        enhancedTnt.clear();
        cooldowns.clear();
        treeFellerWindows.clear();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void activateConnectedBreak(BlockBreakEvent event) {
        if (!enabled || activePlayers.contains(event.getPlayer().getUniqueId())) {
            return;
        }
        Player player = event.getPlayer();
        Block source = event.getBlock();
        Material material = source.getType();
        if (GatheringMaterialPolicy.isOre(material)) {
            if (!player.isSneaking()) {
                return;
            }
            tryConnectedAbility(
                player, source, material, SkillId.MINING, MechanicId.VEIN_MINING,
                GatheringTool.PICKAXE, config.veinMining(),
                candidate -> GatheringMaterialPolicy.sameOreFamily(material, candidate.getType()),
                false);
        } else if (GatheringMaterialPolicy.isVeinCluster(material)) {
            if (!player.isSneaking()) {
                return;
            }
            tryConnectedAbility(
                player, source, material, SkillId.MINING, MechanicId.VEIN_CLUSTER_EXTRACTION,
                GatheringTool.PICKAXE, config.veinMining(),
                candidate -> candidate.getType() == material,
                false);
        } else if (GatheringMaterialPolicy.isLog(material)) {
            if (!isTreeFellerActive(player.getUniqueId(), System.currentTimeMillis())) {
                return;
            }
            tryConnectedAbility(
                player, source, material, SkillId.WOODCUTTING, MechanicId.TREE_FELLER,
                GatheringTool.AXE, config.treeFeller(),
                candidate -> GatheringMaterialPolicy.sameWoodFamily(material, candidate.getType()),
                true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void activateTreeFeller(PlayerInteractEvent event) {
        if (!enabled || event.getHand() != EquipmentSlot.HAND || !event.getPlayer().isSneaking()
            || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK
            && (clickedBlock == null || !GatheringMaterialPolicy.isLog(clickedBlock.getType()))) {
            return;
        }
        Player player = event.getPlayer();
        GatheringAbilityConfig ability = config.treeFeller();
        if (!ability.enabled() || ability.durationSeconds() <= 0 || unsupportedMode(player)
            || !plugin.configuration().get().worlds().isEnabled(player.getWorld().getName())
            || !GatheringMaterialPolicy.suitableTool(
                GatheringTool.AXE, player.getInventory().getItemInMainHand())) {
            return;
        }
        var profile = module.cachedProfile(player.getUniqueId());
        if (profile.isEmpty()) {
            return;
        }
        try {
            if (!mechanics.has(profile.get(), SkillId.WOODCUTTING, MechanicId.TREE_FELLER)) {
                return;
            }
        } catch (RuntimeException exception) {
            module.invalidateProfile(player.getUniqueId(), exception);
            return;
        }

        long now = System.currentTimeMillis();
        TimedAbilityWindow existing = treeFellerWindows.get(player.getUniqueId());
        if (existing != null && existing.isActiveAt(now)) {
            event.setCancelled(true);
            messages.sendActionBar(player, "skills-tree-feller-active", Map.of(
                "seconds", Math.max(1L, (existing.activeUntil() - now + 999L) / 1_000L)));
            return;
        }
        long remaining = existing == null ? 0L : existing.cooldownRemainingAt(now);
        if (remaining > 0) {
            event.setCancelled(true);
            sendCooldown(player, remaining);
            return;
        }
        treeFellerWindows.put(player.getUniqueId(),
            TimedAbilityWindow.start(now, ability.durationSeconds(), ability.cooldownSeconds()));
        event.setCancelled(true);
        player.swingMainHand();
        messages.sendActionBar(player, "skills-tree-feller-active", Map.of("seconds", ability.durationSeconds()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void enhancePlayerTnt(ExplosionPrimeEvent event) {
        if (!enabled || !config.drilling().enabled() || !(event.getEntity() instanceof TNTPrimed tnt)
            || !(tnt.getSource() instanceof Player player) || unsupportedMode(player)
            || !plugin.configuration().get().worlds().isEnabled(tnt.getWorld().getName())) {
            return;
        }
        var profile = module.cachedProfile(player.getUniqueId());
        if (profile.isEmpty()) {
            return;
        }
        boolean unlocked;
        double multiplier;
        try {
            unlocked = mechanics.has(profile.get(), SkillId.MINING, MechanicId.DRILLING);
            multiplier = stats.resolve(profile.get(), SkillId.MINING).value(StatId.TNT_POWER);
        } catch (RuntimeException exception) {
            module.invalidateProfile(player.getUniqueId(), exception);
            return;
        }
        if (!unlocked) {
            return;
        }
        long remaining = cooldownRemaining(player.getUniqueId(), MechanicId.DRILLING);
        AbilityExecutionPolicy policy = new AbilityExecutionPolicy(config.drilling().maximumBlocks());
        var decision = policy.evaluate(new AbilityExecutionContext(
            MechanicId.DRILLING, true, false, unsupportedMode(player), false,
            remaining, 1, true));
        if (!decision.allowed()) {
            if (remaining > 0) {
                sendCooldown(player, remaining);
            }
            return;
        }

        float power = (float) Math.min(MAXIMUM_TNT_POWER, event.getRadius() * multiplier);
        if (power <= event.getRadius()) {
            return;
        }
        event.setRadius(power);
        event.setFire(false);
        enhancedTnt.put(tnt.getUniqueId(), new EnhancedTnt(
            player.getUniqueId(), config.drilling().maximumBlocks()));
        setCooldown(player.getUniqueId(), MechanicId.DRILLING, config.drilling(),
            stats.resolve(profile.get(), SkillId.MINING).value(StatId.DRILLING_COOLDOWN_REDUCTION));
        messages.sendActionBar(player, "skills-drilling-ready", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void capEnhancedExplosion(EntityExplodeEvent event) {
        EnhancedTnt enhanced = enhancedTnt.remove(event.getEntity().getUniqueId());
        if (enhanced == null || event.blockList().size() <= enhanced.maximumBlocks()) {
            return;
        }
        List<Block> closest = event.blockList().stream()
            .sorted(Comparator.comparingDouble(block ->
                block.getLocation().distanceSquared(event.getLocation())))
            .limit(enhanced.maximumBlocks())
            .toList();
        event.blockList().retainAll(closest);
    }

    @EventHandler
    public void cleanupPlayer(PlayerQuitEvent event) {
        cancelActive(event.getPlayer().getUniqueId());
        cooldowns.keySet().removeIf(key -> key.playerId().equals(event.getPlayer().getUniqueId()));
        treeFellerWindows.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void cleanupPlayer(PlayerTeleportEvent event) {
        cancelActive(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void cleanupPlayer(PlayerDeathEvent event) {
        cancelActive(event.getPlayer().getUniqueId());
    }

    private void tryConnectedAbility(
        Player player,
        Block source,
        Material sourceMaterial,
        SkillId skill,
        MechanicId mechanic,
        GatheringTool tool,
        GatheringAbilityConfig ability,
        Predicate<Block> accepted,
        boolean requireTreeCrown
    ) {
        if (!ability.enabled() || unsupportedMode(player)
            || gathering.placedBlocks().isPlayerPlaced(source)
            || !plugin.configuration().get().worlds().isEnabled(source.getWorld().getName())) {
            return;
        }
        var profile = module.cachedProfile(player.getUniqueId());
        boolean unlocked = false;
        if (profile.isPresent()) {
            try {
                unlocked = mechanics.has(profile.get(), skill, mechanic);
            } catch (RuntimeException exception) {
                module.invalidateProfile(player.getUniqueId(), exception);
                return;
            }
        }
        long remaining = cooldownRemaining(player.getUniqueId(), mechanic);
        List<Block> connected = BoundedGraphSearch.connected(
            source,
            ability.maximumBlocks() + 1,
            this::loadedNeighbours,
            block -> accepted.test(block) && !gathering.placedBlocks().isPlayerPlaced(block));
        connected = connected.stream().filter(block -> !sameBlock(block, source)).toList();
        if (requireTreeCrown && !hasNearbyLeaves(source.getWorld(), connected, source)) {
            return;
        }
        AbilityExecutionPolicy policy = new AbilityExecutionPolicy(ability.maximumBlocks());
        var decision = policy.evaluate(new AbilityExecutionContext(
            mechanic, unlocked, false, unsupportedMode(player), false, remaining,
            connected.size(), GatheringMaterialPolicy.suitableTool(
                tool, player.getInventory().getItemInMainHand())));
        if (!decision.allowed()) {
            if (unlocked && remaining > 0) {
                sendCooldown(player, remaining);
            }
            return;
        }
        List<BlockTarget> targets = connected.stream()
            .limit(decision.permittedBlockCount())
            .map(BlockTarget::from)
            .toList();
        if (targets.isEmpty()) {
            return;
        }
        if (mechanic != MechanicId.TREE_FELLER) {
            setCooldown(player.getUniqueId(), mechanic, ability);
        }
        int blocksPerTick = ability.blocksPerTick();
        if (mechanic == MechanicId.VEIN_CLUSTER_EXTRACTION) blocksPerTick *= 2;
        runBatchedBreak(player, targets, tool, accepted, blocksPerTick,
            sourceMaterial, source.getLocation());
    }

    private void runBatchedBreak(
        Player player,
        List<BlockTarget> targets,
        GatheringTool tool,
        Predicate<Block> accepted,
        int blocksPerTick,
        Material sourceMaterial,
        Location activationOrigin
    ) {
        UUID playerId = player.getUniqueId();
        cancelActive(playerId);
        activePlayers.add(playerId);
        gathering.beginAutomatedBreaks(playerId);
        int[] cursor = {0};
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled || !player.isOnline() || unsupportedMode(player)
                || !player.getWorld().equals(activationOrigin.getWorld())
                || player.getLocation().distanceSquared(activationOrigin) > 256.0
                || !GatheringMaterialPolicy.suitableTool(
                    tool, player.getInventory().getItemInMainHand())) {
                cancelActive(playerId);
                return;
            }
            int processed = 0;
            while (processed < blocksPerTick && cursor[0] < targets.size()) {
                if (!GatheringMaterialPolicy.suitableTool(
                    tool, player.getInventory().getItemInMainHand())) {
                    cancelActive(playerId);
                    return;
                }
                BlockTarget target = targets.get(cursor[0]++);
                Block block = target.resolve();
                processed++;
                if (block == null || !accepted.test(block)
                    || gathering.placedBlocks().isPlayerPlaced(block)) {
                    continue;
                }
                player.breakBlock(block);
            }
            if (cursor[0] >= targets.size()) {
                cancelActive(playerId);
                messages.sendActionBar(player,
                    GatheringMaterialPolicy.isOre(sourceMaterial)
                        ? "skills-vein-mining-complete" : "skills-tree-feller-complete",
                    Map.of());
            }
        }, 1L, 1L);
        activeTasks.put(playerId, task);
    }

    private List<Block> loadedNeighbours(Block block) {
        List<Block> neighbours = new ArrayList<>(ADJACENT_26.length);
        World world = block.getWorld();
        for (int[] offset : ADJACENT_26) {
            int x = block.getX() + offset[0];
            int y = block.getY() + offset[1];
            int z = block.getZ() + offset[2];
            if (y < world.getMinHeight() || y >= world.getMaxHeight()
                || !world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            neighbours.add(world.getBlockAt(x, y, z));
        }
        return neighbours;
    }

    private boolean hasNearbyLeaves(World world, List<Block> connected, Block source) {
        List<Block> candidates = new ArrayList<>(connected);
        candidates.add(source);
        if (candidates.size() < 3) {
            return false;
        }
        for (Block log : candidates) {
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        int targetX = log.getX() + x;
                        int targetY = log.getY() + y;
                        int targetZ = log.getZ() + z;
                        if (targetY >= world.getMinHeight() && targetY < world.getMaxHeight()
                            && world.isChunkLoaded(targetX >> 4, targetZ >> 4)
                            && GatheringMaterialPolicy.isLeaves(
                                world.getBlockAt(targetX, targetY, targetZ).getType())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private long cooldownRemaining(UUID playerId, MechanicId mechanic) {
        long expiry = cooldowns.getOrDefault(new CooldownKey(playerId, mechanic), 0L);
        return Math.max(0L, expiry - System.currentTimeMillis());
    }

    private void setCooldown(UUID playerId, MechanicId mechanic, GatheringAbilityConfig ability) {
        setCooldown(playerId, mechanic, ability, 0.0);
    }

    private boolean isTreeFellerActive(UUID playerId, long now) {
        TimedAbilityWindow window = treeFellerWindows.get(playerId);
        return window != null && window.isActiveAt(now);
    }

    private void setCooldown(UUID playerId, MechanicId mechanic, GatheringAbilityConfig ability, double reduction) {
        if (ability.cooldownSeconds() > 0) {
            cooldowns.put(new CooldownKey(playerId, mechanic),
                System.currentTimeMillis() + Math.round(ability.cooldownSeconds() * (1.0 - reduction) * 1_000L));
        }
    }

    private void sendCooldown(Player player, long remainingMillis) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1_000L);
        messages.sendActionBar(player, "skills-ability-cooldown", Map.of("seconds", seconds));
    }

    private void cancelActive(UUID playerId) {
        BukkitTask task = activeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
        activePlayers.remove(playerId);
        gathering.endAutomatedBreaks(playerId);
    }

    private static boolean unsupportedMode(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
            || player.getGameMode() == GameMode.SPECTATOR;
    }

    private static boolean sameBlock(Block first, Block second) {
        return first.getWorld().getUID().equals(second.getWorld().getUID())
            && first.getX() == second.getX() && first.getY() == second.getY()
            && first.getZ() == second.getZ();
    }

    private static int[][] adjacentOffsets() {
        List<int[]> offsets = new ArrayList<>(26);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        offsets.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return offsets.toArray(int[][]::new);
    }

    private record CooldownKey(UUID playerId, MechanicId mechanic) {
    }

    private record EnhancedTnt(UUID playerId, int maximumBlocks) {
    }

    private record BlockTarget(UUID worldId, int x, int y, int z) {
        static BlockTarget from(Block block) {
            return new BlockTarget(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        Block resolve() {
            World world = Bukkit.getWorld(worldId);
            if (world == null || y < world.getMinHeight() || y >= world.getMaxHeight()
                || !world.isChunkLoaded(x >> 4, z >> 4)) {
                return null;
            }
            return world.getBlockAt(x, y, z);
        }
    }
}
