package cz.nekara.rpg.modules.mining;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.compatibility.ValhallaMiningBridge;
import cz.nekara.rpg.configuration.EchoVeinConfig;
import cz.nekara.rpg.echovein.BlockPosition;
import cz.nekara.rpg.echovein.EchoVeinMath;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.modules.fishing.FishingModule;
import cz.nekara.rpg.sounds.SoundService;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class MiningModule implements NekaraModule, Listener {
    public static final String ID = "mining";
    private static final long PENDING_MAX_AGE_MILLIS = 3_000L;
    private static final Set<Material> HOST_MATERIALS = Collections.unmodifiableSet(EnumSet.of(
            Material.STONE,
            Material.DEEPSLATE,
            Material.NETHERRACK,
            Material.END_STONE));

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final SoundService sounds;
    private final FishingModule fishingModule;
    private final ValhallaMiningBridge valhalla;
    private final Map<UUID, Deque<PendingMiningAction>> activeBreaks = new HashMap<>();
    private final Map<BlockPosition, PendingMiningAction> awaitingDrops = new HashMap<>();
    private final Map<UUID, Deque<BlockPosition>> awaitingByPlayer = new HashMap<>();
    private final Map<UUID, EchoVeinSession> sessions = new HashMap<>();
    private EchoVeinConfig config;
    private BukkitTask ticker;
    private boolean enabled;

    public MiningModule(
            NekaraRPGPlugin plugin,
            MessageService messages,
            SoundService sounds,
            FishingModule fishingModule
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.sounds = sounds;
        this.fishingModule = fishingModule;
        this.valhalla = new ValhallaMiningBridge(plugin, this::prepareExperienceCapture);
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
        config = plugin.configuration().get().echoVein();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        valhalla.register();
        ticker = plugin.getServer().getScheduler().runTaskTimer(
                plugin, this::tick, 0L, config.pulseIntervalTicks());
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        cancelAll(false);
        activeBreaks.clear();
        awaitingDrops.clear();
        awaitingByPlayer.clear();
        valhalla.unregister();
        HandlerList.unregisterAll(this);
        enabled = false;
    }

    @Override
    public void reload() {
        disable();
        enable();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public int activeCount() {
        return sessions.size();
    }

    public boolean isActive(UUID playerId) {
        return sessions.containsKey(playerId);
    }

    public boolean bridgeAvailable() {
        return valhalla.isAvailable();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void beginMiningAction(BlockBreakEvent event) {
        Player player = event.getPlayer();
        EchoVeinSession activeSession = sessions.get(player.getUniqueId());
        EchoVeinSession targetSession = isNaturalTarget(player, event.getBlock(), activeSession)
                ? activeSession : null;
        if (targetSession == null && (!isHostMaterial(event.getBlock().getType()) || !canObserve(player))) {
            return;
        }
        PendingMiningAction action = new PendingMiningAction(
                player.getUniqueId(), BlockPosition.of(event.getBlock()),
                event.getBlock().getLocation(), event.getBlock().getType(), targetSession);
        activeBreaks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()).addLast(action);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void capturePreparedDrops(BlockBreakEvent event) {
        PendingMiningAction action = activeAction(event.getPlayer(), event.getBlock());
        if (action != null) {
            action.addDrops(valhalla.preparedDrops(event.getBlock()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void finishMiningEvent(BlockBreakEvent event) {
        PendingMiningAction action = popActiveAction(event.getPlayer(), event.getBlock());
        if (action == null || event.isCancelled()) {
            return;
        }
        if (action.echoSession() != null
                && !sessions.remove(action.playerId(), action.echoSession())) {
            return;
        }
        awaitingDrops.put(action.position(), action);
        awaitingByPlayer.computeIfAbsent(action.playerId(), ignored -> new ArrayDeque<>())
                .addLast(action.position());
        plugin.getServer().getScheduler().runTaskLater(
                plugin, () -> finalizeMiningAction(action), 3L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void captureNaturalDrops(BlockDropItemEvent event) {
        PendingMiningAction action = awaitingDrops.get(BlockPosition.of(event.getBlockState().getLocation()));
        if (action == null || !action.playerId().equals(event.getPlayer().getUniqueId())) {
            return;
        }
        for (org.bukkit.entity.Item item : event.getItems()) {
            action.addDrop(item.getItemStack());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void chooseVein(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND
                || event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK
                || event.getClickedBlock() == null) {
            return;
        }
        EchoVeinSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || !session.test()
                || !isPickaxe(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }
        if (session.target().equals(BlockPosition.of(event.getClickedBlock()))) {
            completeTestSuccess(event.getPlayer());
        }
    }

    @EventHandler
    public void cancelOnQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), false);
        clearPending(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void cancelOnDeath(PlayerDeathEvent event) {
        completeFailure(event.getEntity(), false);
    }

    @EventHandler(ignoreCancelled = true)
    public void cancelOnWorldChange(PlayerTeleportEvent event) {
        EchoVeinSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session != null && (event.getTo().getWorld() != event.getFrom().getWorld()
                || event.getTo().distanceSquared(session.targetLocation())
                > Math.pow(config.searchRadius() + 4.0, 2))) {
            completeFailure(event.getPlayer(), false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void yieldToFishing(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.FISHING) {
            cancel(event.getPlayer().getUniqueId(), false);
        }
    }

    public boolean startTest(Player player) {
        if (!enabled || sessions.containsKey(player.getUniqueId())
                || fishingModule.minigames().session(player.getUniqueId()) != null) {
            return false;
        }
        Block target = findTarget(player, player.getLocation());
        if (target == null) {
            return false;
        }
        startSession(player, target, true);
        return true;
    }

    public boolean cancelByCommand(UUID playerId, CommandSender requester, String targetName) {
        if (!cancel(playerId, true)) {
            return false;
        }
        if (!(requester instanceof Player player) || !playerId.equals(player.getUniqueId())) {
            messages.send(requester, "player-cancelled", Map.of("player", targetName));
        }
        return true;
    }

    private boolean canObserve(Player player) {
        if (!enabled || !valhalla.isAvailable() || !player.hasPermission("nekararpg.echo-vein.use")
                || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR
                || !plugin.configuration().get().worlds().isEnabled(player.getWorld().getName())
                || sessions.containsKey(player.getUniqueId())) {
            return false;
        }
        return fishingModule.minigames().session(player.getUniqueId()) == null;
    }

    private Runnable prepareExperienceCapture(Player player, double amount) {
        PendingMiningAction action = latestAction(player.getUniqueId());
        if (action != null && System.currentTimeMillis() - action.createdAtMillis() <= PENDING_MAX_AGE_MILLIS) {
            return () -> action.addExperience(amount);
        }
        return null;
    }

    private PendingMiningAction latestAction(UUID playerId) {
        Deque<PendingMiningAction> active = activeBreaks.get(playerId);
        if (active != null && !active.isEmpty()) {
            return active.peekLast();
        }
        Deque<BlockPosition> positions = awaitingByPlayer.get(playerId);
        if (positions == null) {
            return null;
        }
        while (!positions.isEmpty()) {
            PendingMiningAction action = awaitingDrops.get(positions.peekLast());
            if (action != null) {
                return action;
            }
            positions.removeLast();
        }
        return null;
    }

    private PendingMiningAction activeAction(Player player, Block block) {
        Deque<PendingMiningAction> actions = activeBreaks.get(player.getUniqueId());
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        PendingMiningAction action = actions.peekLast();
        return action.position().equals(BlockPosition.of(block)) ? action : null;
    }

    private PendingMiningAction popActiveAction(Player player, Block block) {
        Deque<PendingMiningAction> actions = activeBreaks.get(player.getUniqueId());
        if (actions == null || actions.isEmpty()) {
            return null;
        }
        PendingMiningAction action = actions.peekLast();
        if (!action.position().equals(BlockPosition.of(block))) {
            return null;
        }
        actions.removeLast();
        if (actions.isEmpty()) {
            activeBreaks.remove(player.getUniqueId());
        }
        return action;
    }

    private void finalizeMiningAction(PendingMiningAction action) {
        PendingMiningAction current = awaitingDrops.get(action.position());
        if (current != action) {
            return;
        }
        awaitingDrops.remove(action.position());
        removeAwaiting(action.playerId(), action.position());
        Player player = plugin.getServer().getPlayer(action.playerId());
        if (player == null || !player.isOnline()
                || System.currentTimeMillis() - action.createdAtMillis() > PENDING_MAX_AGE_MILLIS) {
            return;
        }
        if (action.echoSession() != null) {
            completeMinedVein(player, action);
            return;
        }
        if (!canObserve(player) || action.experience() <= 0.0) {
            return;
        }
        debug(String.format(Locale.ROOT,
                "Mining XP observed for %s: block=%s xpEvents=%d totalXp=%.4f",
                player.getName(), action.material(), action.experienceEventCount(), action.experience()));

        if (!EchoVeinMath.winsChance(
                ThreadLocalRandom.current().nextDouble(), config.triggerChance())) {
            return;
        }

        Block target = findTarget(player, action.origin());
        if (target == null) {
            return;
        }
        debug(String.format(Locale.ROOT,
                "Echo Vein triggered for %s: block=%s xpEvents=%d triggeringBlockXp=%.4f target=%s",
                player.getName(), action.material(), action.experienceEventCount(),
                action.experience(), BlockPosition.of(target)));
        startSession(player, target, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void revealOre(BlockDamageEvent event) {
        Player player = event.getPlayer();
        EchoVeinSession session = sessions.get(player.getUniqueId());
        if (!isNaturalTarget(player, event.getBlock(), session) || session.revealRolled()) {
            return;
        }

        Material revealed = null;
        if (EchoVeinMath.winsChance(
                ThreadLocalRandom.current().nextDouble(), config.oreRevealChance())) {
            revealed = chooseRevealedOre(event.getBlock().getType());
        }
        if (revealed != null) {
            event.getBlock().setType(revealed, false);
        }
        EchoVeinSession updated = new EchoVeinSession(
                session.target(),
                revealed == null ? session.targetMaterial() : revealed,
                session.targetLocation(),
                session.expiresAtMillis(),
                session.test(),
                true);
        sessions.replace(player.getUniqueId(), session, updated);
        if (revealed != null) {
            debug("Echo Vein ore revealed for " + player.getName() + ": " + revealed);
        }
    }

    private void startSession(Player player, Block target, boolean test) {
        long now = System.currentTimeMillis();
        EchoVeinSession session = new EchoVeinSession(
                BlockPosition.of(target),
                target.getType(),
                target.getLocation().add(0.5, 0.5, 0.5),
                now + (config.durationTicks() * 50L),
                test,
                false);
        sessions.put(player.getUniqueId(), session);
        if (test) {
            messages.send(player, "echo-vein-test-started");
        }
        pulse(player, session);
        sounds.playAt(player, "echo-vein-pulse", session.targetLocation());
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            EchoVeinSession session = sessions.get(playerId);
            Player player = plugin.getServer().getPlayer(playerId);
            if (session == null) {
                continue;
            }
            if (player == null || !player.isOnline()
                    || session.targetLocation().getWorld() != player.getWorld()) {
                cancel(playerId, false);
                continue;
            }
            if (session.expiresAtMillis() <= now) {
                completeFailure(player, true);
                continue;
            }
            Block target = session.targetLocation().getBlock();
            if (target.getType() != session.targetMaterial()) {
                completeFailure(player, true);
                continue;
            }
            pulse(player, session);
            long remainingMillis = Math.max(0L, session.expiresAtMillis() - now);
            messages.sendActionBar(player, "echo-vein-active", Map.of(
                    "seconds", String.format(Locale.ROOT, "%.1f", remainingMillis / 1_000.0)));
        }
    }

    private void pulse(Player player, EchoVeinSession session) {
        EchoVeinConfig current = config;
        Particle particle = current.particle();
        Location surface = visibleSurface(player, session.targetLocation());
        int nearbyCount = Math.max(2, current.particleCount() / 2);
        int targetCount = Math.min(96, Math.max(
                current.particleCount() + 4, current.particleCount() * 2));
        player.spawnParticle(
                particle,
                surface,
                nearbyCount,
                0.85,
                0.85,
                0.85,
                0.002);
        double targetSpread = Math.max(0.08, current.particleSpread() * 0.45);
        player.spawnParticle(
                particle,
                surface,
                targetCount,
                targetSpread,
                targetSpread,
                targetSpread,
                0.01);
    }

    private Location visibleSurface(Player player, Location blockCenter) {
        Vector towardPlayer = player.getEyeLocation().toVector().subtract(blockCenter.toVector());
        if (towardPlayer.lengthSquared() == 0.0) {
            return blockCenter.clone();
        }
        double x = Math.abs(towardPlayer.getX());
        double y = Math.abs(towardPlayer.getY());
        double z = Math.abs(towardPlayer.getZ());
        if (x >= y && x >= z) {
            return blockCenter.clone().add(Math.copySign(0.52, towardPlayer.getX()), 0.0, 0.0);
        }
        if (y >= z) {
            return blockCenter.clone().add(0.0, Math.copySign(0.52, towardPlayer.getY()), 0.0);
        }
        return blockCenter.clone().add(0.0, 0.0, Math.copySign(0.52, towardPlayer.getZ()));
    }

    private void completeTestSuccess(Player player) {
        EchoVeinSession session = sessions.remove(player.getUniqueId());
        if (session == null || !session.test()) {
            return;
        }
        sounds.play(player, "echo-vein-success");
        messages.send(player, "echo-vein-test-success");
    }

    private void completeMinedVein(Player player, PendingMiningAction action) {
        double bonus = EchoVeinMath.bonusExperience(
                action.experience(), config.experienceBonusMultiplier());
        boolean experienceGranted = valhalla.grantBonusExperience(player, bonus);
        ItemStack reward = config.bonusDropEnabled() ? chooseBonusDrop(action.drops()) : null;
        boolean dropGranted = giveReward(player, reward);

        Block chainedTarget = null;
        if (EchoVeinMath.winsChance(
                ThreadLocalRandom.current().nextDouble(), config.chainChance())) {
            chainedTarget = findAdjacentTarget(player, action.origin());
        }
        boolean chained = chainedTarget != null;
        if (chained) {
            startSession(player, chainedTarget, false);
        } else {
            sounds.play(player, "echo-vein-success");
        }

        debug(String.format(Locale.ROOT,
                "Echo Vein completed for %s: block=%s xpEvents=%d markedBlockXp=%.4f bonusXp=%.4f "
                        + "xpGranted=%s bonusDrop=%s chained=%s",
                player.getName(), action.material(), action.experienceEventCount(), action.experience(), bonus,
                experienceGranted, dropGranted, chained));
    }

    private boolean giveReward(Player player, ItemStack reward) {
        if (reward == null || reward.getType().isAir() || reward.getAmount() <= 0) {
            return false;
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(reward.clone());
        for (ItemStack item : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        return true;
    }

    private void completeFailure(Player player, boolean notify) {
        EchoVeinSession removed = sessions.remove(player.getUniqueId());
        if (removed == null) {
            return;
        }
        if (removed.test()) {
            sounds.play(player, "echo-vein-failure");
            if (notify) {
                messages.send(player, "echo-vein-test-failure");
            }
        }
    }

    private boolean cancel(UUID playerId, boolean notify) {
        EchoVeinSession removed = sessions.remove(playerId);
        if (removed == null) {
            return false;
        }
        Player player = plugin.getServer().getPlayer(playerId);
        if (notify && player != null) {
            messages.send(player, "cancelled");
        }
        return true;
    }

    private void cancelAll(boolean notify) {
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            cancel(playerId, notify);
        }
    }

    private void clearPending(UUID playerId) {
        activeBreaks.remove(playerId);
        Deque<BlockPosition> positions = awaitingByPlayer.remove(playerId);
        if (positions != null) {
            positions.forEach(awaitingDrops::remove);
        }
    }

    private void removeAwaiting(UUID playerId, BlockPosition position) {
        Deque<BlockPosition> positions = awaitingByPlayer.get(playerId);
        if (positions == null) {
            return;
        }
        positions.remove(position);
        if (positions.isEmpty()) {
            awaitingByPlayer.remove(playerId);
        }
    }

    private ItemStack chooseBonusDrop(List<ItemStack> drops) {
        List<ItemStack> eligible = drops.stream()
                .filter(item -> item != null && !item.getType().isAir() && item.getAmount() > 0)
                .map(ItemStack::clone)
                .toList();
        if (eligible.isEmpty()) {
            return null;
        }
        long total = eligible.stream().mapToLong(ItemStack::getAmount).sum();
        if (total <= 0L) {
            return null;
        }
        long ticket = ThreadLocalRandom.current().nextLong(total);
        int index = EchoVeinMath.weightedUnitIndex(
                eligible.stream().map(ItemStack::getAmount).toList(), ticket);
        if (index < 0) {
            return null;
        }
        ItemStack reward = eligible.get(index).clone();
        reward.setAmount(1);
        return reward;
    }

    private Block findTarget(Player player, Location origin) {
        World world = origin.getWorld();
        if (world == null || world != player.getWorld()) {
            return null;
        }
        List<Block> candidates = new ArrayList<>();
        int radius = config.searchRadius();
        BlockPosition originPosition = BlockPosition.of(origin);
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    int blockY = origin.getBlockY() + y;
                    if (blockY < world.getMinHeight() || blockY >= world.getMaxHeight()) {
                        continue;
                    }
                    Block block = world.getBlockAt(
                            origin.getBlockX() + x,
                            blockY,
                            origin.getBlockZ() + z);
                    if (originPosition.equals(BlockPosition.of(block)) || !isCandidate(block)
                            || !isVisible(player, block)) {
                        continue;
                    }
                    candidates.add(block);
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        return candidates.get(0);
    }

    private Block findAdjacentTarget(Player player, Location origin) {
        Block source = origin.getBlock();
        List<Block> candidates = new ArrayList<>();
        for (BlockFace face : new BlockFace[]{
                BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
                BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Block candidate = source.getRelative(face);
            if (isCandidate(candidate) && isVisible(player, candidate)) {
                candidates.add(candidate);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        Collections.shuffle(candidates, ThreadLocalRandom.current());
        return candidates.get(0);
    }

    private boolean isCandidate(Block block) {
        Material type = block.getType();
        return isHostMaterial(type) && type.isSolid() && !block.isLiquid()
                && Tag.MINEABLE_PICKAXE.isTagged(type)
                && !(block.getState() instanceof Container);
    }

    private Material chooseRevealedOre(Material host) {
        int roll = ThreadLocalRandom.current().nextInt(100);
        return switch (host) {
            case STONE -> roll < 40 ? Material.COAL_ORE
                    : roll < 70 ? Material.COPPER_ORE
                    : roll < 92 ? Material.IRON_ORE
                    : roll < 96 ? Material.GOLD_ORE
                    : roll < 98 ? Material.REDSTONE_ORE
                    : roll < 99 ? Material.LAPIS_ORE
                    : Material.DIAMOND_ORE;
            case DEEPSLATE -> roll < 2 ? Material.DEEPSLATE_COAL_ORE
                    : roll < 17 ? Material.DEEPSLATE_COPPER_ORE
                    : roll < 47 ? Material.DEEPSLATE_IRON_ORE
                    : roll < 59 ? Material.DEEPSLATE_GOLD_ORE
                    : roll < 84 ? Material.DEEPSLATE_REDSTONE_ORE
                    : roll < 92 ? Material.DEEPSLATE_LAPIS_ORE
                    : Material.DEEPSLATE_DIAMOND_ORE;
            case NETHERRACK -> roll < 85 ? Material.NETHER_QUARTZ_ORE : Material.NETHER_GOLD_ORE;
            default -> null;
        };
    }

    private boolean isVisible(Player player, Block block) {
        Location eye = player.getEyeLocation();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        Vector direction = center.toVector().subtract(eye.toVector());
        double distance = direction.length();
        if (distance <= 0.0 || distance > config.searchRadius() + 4.0) {
            return false;
        }
        RayTraceResult hit = player.getWorld().rayTraceBlocks(
                eye, direction.normalize(), distance + 0.2, FluidCollisionMode.NEVER, true);
        return hit != null && block.equals(hit.getHitBlock());
    }

    private static boolean isPickaxe(ItemStack item) {
        return item != null && item.getType().name().endsWith("_PICKAXE");
    }

    private static boolean isHostMaterial(Material material) {
        return HOST_MATERIALS.contains(material);
    }

    private static boolean isNaturalTarget(Player player, Block block, EchoVeinSession session) {
        return session != null && !session.test()
                && session.target().equals(BlockPosition.of(block))
                && isPickaxe(player.getInventory().getItemInMainHand());
    }

    private void debug(String message) {
        if (plugin.configuration().get().debug()) {
            plugin.getLogger().info("[Debug] " + message);
        }
    }

    private static final class PendingMiningAction {
        private final UUID playerId;
        private final BlockPosition position;
        private final Location origin;
        private final Material material;
        private final EchoVeinSession echoSession;
        private final long createdAtMillis = System.currentTimeMillis();
        private final List<ItemStack> drops = new ArrayList<>();
        private int experienceEventCount;
        private double experience;

        private PendingMiningAction(
                UUID playerId,
                BlockPosition position,
                Location origin,
                Material material,
                EchoVeinSession echoSession
        ) {
            this.playerId = playerId;
            this.position = position;
            this.origin = origin.clone();
            this.material = material;
            this.echoSession = echoSession;
        }

        private UUID playerId() {
            return playerId;
        }

        private BlockPosition position() {
            return position;
        }

        private Location origin() {
            return origin.clone();
        }

        private EchoVeinSession echoSession() {
            return echoSession;
        }

        private Material material() {
            return material;
        }

        private long createdAtMillis() {
            return createdAtMillis;
        }

        private double experience() {
            return experience;
        }

        private int experienceEventCount() {
            return experienceEventCount;
        }

        private List<ItemStack> drops() {
            return List.copyOf(drops);
        }

        private void addExperience(double amount) {
            if (Double.isFinite(amount) && amount > 0.0) {
                experience += amount;
                experienceEventCount++;
            }
        }

        private void addDrops(List<ItemStack> items) {
            items.forEach(this::addDrop);
        }

        private void addDrop(ItemStack item) {
            if (item != null && !item.getType().isAir() && item.getAmount() > 0) {
                drops.add(item.clone());
            }
        }
    }

    private record EchoVeinSession(
            BlockPosition target,
            Material targetMaterial,
            Location targetLocation,
            long expiresAtMillis,
            boolean test,
            boolean revealRolled
    ) {
    }
}
