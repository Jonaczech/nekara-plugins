package cz.nekara.rpg.minigame;

import cz.nekara.rpg.configuration.FishingConfig;
import cz.nekara.rpg.configuration.HookParticleConfig;
import cz.nekara.rpg.configuration.MinigameConfig;
import cz.nekara.rpg.configuration.OutcomeEffectConfig;
import cz.nekara.rpg.configuration.PluginConfig;
import cz.nekara.rpg.configuration.ValhallaFishingConfig;
import cz.nekara.rpg.compatibility.ValhallaExperienceBridge;
import cz.nekara.rpg.fishing.DeferredCatchCompatibilityStrategy;
import cz.nekara.rpg.fishing.FishingCompatibilityStrategy;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.sounds.SoundService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.FluidCollisionMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Location;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FishingMinigameManager {
    private static final int SUPPRESSED_BITE_WAIT_TICKS = 1_000_000;
    private final JavaPlugin plugin;
    private final MessageService messages;
    private final SoundService sounds;
    private final FishingCompatibilityStrategy compatibilityStrategy = new DeferredCatchCompatibilityStrategy();
    private final MinigameRenderer renderer = new MinigameRenderer();
    private final Map<UUID, FishingMinigameSession> sessions = new HashMap<>();
    private MinigameConfig minigameConfig;
    private FishingConfig fishingConfig;
    private HookParticleConfig hookParticleConfig;
    private OutcomeEffectConfig successEffect;
    private OutcomeEffectConfig failureEffect;
    private ValhallaFishingConfig valhallaFishingConfig;
    private boolean debug;
    private BukkitTask tickTask;
    private long effectTick;
    private ValhallaExperienceBridge valhallaExperienceBridge;

    public FishingMinigameManager(JavaPlugin plugin, MessageService messages, SoundService sounds) {
        this.plugin = plugin;
        this.messages = messages;
        this.sounds = sounds;
    }

    public void applyConfig(PluginConfig config) {
        this.minigameConfig = config.minigame();
        this.hookParticleConfig = config.hookParticles();
        this.successEffect = config.successEffect();
        this.failureEffect = config.failureEffect();
        this.valhallaFishingConfig = config.valhallaFishing();
        this.fishingConfig = config.fishing();
        this.debug = config.debug();
    }

    public void setValhallaExperienceBridge(ValhallaExperienceBridge bridge) {
        this.valhallaExperienceBridge = bridge;
    }

    public void startTicker() {
        if (tickTask != null) {
            return;
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stopTicker() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    public boolean startFromBite(Player player, FishHook hook) {
        if (sessions.containsKey(player.getUniqueId())) {
            return false;
        }
        FishingMinigameSession session = new FishingMinigameSession(
                player.getUniqueId(), hook, EquipmentSlot.HAND, false,
                effectiveMinigameConfig(player));
        session.state(FishingSessionState.WAITING_FOR_BITE);
        sessions.put(player.getUniqueId(), session);
        begin(player, session);
        return true;
    }

    public boolean startTest(Player player) {
        if (sessions.containsKey(player.getUniqueId())) {
            return false;
        }
        FishingMinigameSession session = new FishingMinigameSession(
                player.getUniqueId(), null, EquipmentSlot.HAND, true,
                effectiveMinigameConfig(player));
        sessions.put(player.getUniqueId(), session);
        begin(player, session);
        return true;
    }

    private MinigameConfig effectiveMinigameConfig(Player player) {
        if (valhallaExperienceBridge == null || valhallaFishingConfig == null) {
            return minigameConfig;
        }
        return valhallaExperienceBridge.applyFishingDifficulty(
                player, minigameConfig, valhallaFishingConfig);
    }

    private void begin(Player player, FishingMinigameSession session) {
        sounds.play(player, "bite");
        messages.send(player, "bite");
        if (session.testSession()) {
            startActiveMinigame(player, session);
        }
    }

    private void tick() {
        effectTick++;
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            FishingMinigameSession session = sessions.get(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (session == null) {
                continue;
            }
            if (player == null || !player.isOnline()) {
                removeSession(playerId, false);
                continue;
            }
            if ((session.state() == FishingSessionState.CATCH_COMPLETED
                    || session.state() == FishingSessionState.FAILED)
                    && session.outcomeTicksRemaining() > 0) {
                processOutcomeEffect(player, session);
                continue;
            }
            if (session.state() == FishingSessionState.MINIGAME_ACTIVE) {
                if (!isSessionStillValid(player, session)) {
                    cancel(playerId, false);
                    continue;
                }
                suppressVanillaBites(session.hook(), false);
                spawnHookParticles(player, session);
                TickResult result = session.engine().advanceTick();
                if (result == TickResult.TIMED_OUT) {
                    timeout(playerId);
                } else if (result == TickResult.MOVED) {
                    render(player, session);
                }
            } else if ((session.state() == FishingSessionState.WAITING_FOR_BITE
                    || session.state() == FishingSessionState.WAITING_FOR_REEL)
                    && !isSessionStillValid(player, session)) {
                cancel(playerId, false);
            }
        }
    }

    private void spawnHookParticles(Player player, FishingMinigameSession session) {
        if (hookParticleConfig == null || !hookParticleConfig.enabled()
                || effectTick % hookParticleConfig.intervalTicks() != 0) {
            return;
        }
        FishHook hook = session.hook();
        if (hook == null || !hook.isValid()) {
            return;
        }
        spawnParticleRing(player, hook.getLocation(), hookParticleConfig.particle(),
                hookParticleConfig.count(), hookParticleConfig.radius(),
                hookParticleConfig.yOffset(), effectTick * 0.12);
    }

    /**
     * Stops vanilla from starting another bite while the deferred catch is being played.
     * This uses the public Paper FishHook API; it does not remove or replace the hook.
     */
    public void suppressVanillaBite(FishHook hook) {
        suppressVanillaBites(hook, true);
    }

    private void suppressVanillaBites(FishHook hook, boolean resetState) {
        if (hook == null || !hook.isValid()) {
            return;
        }
        try {
            if (resetState) {
                hook.resetFishingState();
            }
            if (hook.getWaitTime() < SUPPRESSED_BITE_WAIT_TICKS) {
                hook.setWaitTime(SUPPRESSED_BITE_WAIT_TICKS);
            }
        } catch (IllegalArgumentException | IllegalStateException exception) {
            debug("Could not suppress another vanilla bite: " + exception.getMessage());
        }
    }

    private void processOutcomeEffect(Player player, FishingMinigameSession session) {
        OutcomeEffectConfig effect = session.outcomeEffect();
        FishHook hook = session.hook();
        if (effect != null && hook != null && hook.isValid()) {
            spawnParticleRing(player, hook.getLocation(), effect.particle(), effect.count(),
                    effect.radius(), effect.yOffset(), effectTick * 0.24);
        }
        session.decrementOutcomeTicks();
        if (session.outcomeTicksRemaining() == 0) {
            finishOutcome(player, session);
        }
    }

    private void startOutcomeEffect(Player player, FishingMinigameSession session,
                                    OutcomeEffectConfig effect) {
        session.beginOutcomeEffect(effect);
        if (session.outcomeTicksRemaining() == 0) {
            finishOutcome(player, session);
        }
    }

    private void finishOutcome(Player player, FishingMinigameSession session) {
        if (session.state() == FishingSessionState.CATCH_COMPLETED) {
            if (session.testSession()) {
                removeSession(player.getUniqueId(), true);
            } else {
                completeCatch(player, session);
            }
        } else if (session.state() == FishingSessionState.FAILED) {
            removeSession(player.getUniqueId(), true);
        }
    }

    private void spawnParticleRing(Player player, Location origin, Particle particle, int count,
                                   double radius, double yOffset, double phase) {
        Location center = origin.clone().add(0.0, yOffset, 0.0);
        for (int index = 0; index < count; index++) {
            double angle = phase + (Math.PI * 2.0 * index / count);
            Location point = center.clone().add(
                    Math.cos(angle) * radius,
                    0.0,
                    Math.sin(angle) * radius);
            player.spawnParticle(particle, point, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private boolean isSessionStillValid(Player player, FishingMinigameSession session) {
        if (session.testSession()) {
            return player.isOnline();
        }
        return player.isOnline()
                && isAllowedGameMode(player)
                && player.getInventory().getItemInMainHand().getType() == Material.FISHING_ROD
                && session.hook() != null
                && session.hook().isValid();
    }

    private boolean isAllowedGameMode(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR && !fishingConfig.allowSpectator()) {
            return false;
        }
        return player.getGameMode() != GameMode.CREATIVE || fishingConfig.allowCreative();
    }

    public boolean isActive(UUID playerId) {
        FishingMinigameSession session = sessions.get(playerId);
        return session != null && session.state() != FishingSessionState.CATCH_COMPLETED
                && session.state() != FishingSessionState.FAILED
                && session.state() != FishingSessionState.CANCELLED;
    }

    public boolean isMinigameActive(UUID playerId) {
        FishingMinigameSession session = sessions.get(playerId);
        return session != null && session.state() == FishingSessionState.MINIGAME_ACTIVE;
    }

    public boolean handleClick(Player player) {
        FishingMinigameSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        if (session.state() == FishingSessionState.WAITING_FOR_BITE) {
            return false;
        }
        if (session.state() != FishingSessionState.MINIGAME_ACTIVE) {
            return true;
        }
        if (!isSessionStillValid(player, session)) {
            cancel(player.getUniqueId(), true);
            return false;
        }
        if (!session.acceptsInput(System.currentTimeMillis(), minigameConfig.inputDebounceMilliseconds())) {
            return true;
        }
        ClickResult result = session.engine().click();
        switch (result) {
            case HIT -> {
                session.engine().addTimeBonus(minigameConfig.timeBonusTicks());
                pullHookTowardPlayer(player, session);
                sounds.play(player, "hit");
                render(player, session);
            }
            case MISS -> {
                sounds.play(player, "miss");
            }
            case SUCCEEDED -> {
                session.engine().addTimeBonus(minigameConfig.timeBonusTicks());
                pullHookTowardPlayer(player, session);
                succeed(player, session);
            }
            case FAILED -> fail(player, session, "escape", "escape");
            case IGNORED -> {
                // The session was already finalized; the input remains consumed.
            }
        }
        return true;
    }

    private void pullHookTowardPlayer(Player player, FishingMinigameSession session) {
        FishHook hook = session.hook();
        double maximumDistance = minigameConfig.hookPullDistance();
        if (hook == null || !hook.isValid() || maximumDistance <= 0.0
                || !hook.getWorld().equals(player.getWorld())) {
            return;
        }

        Location start = hook.getLocation();
        Location target = player.getLocation().clone().add(0.0, 1.0, 0.0);
        Vector toPlayer = target.toVector().subtract(start.toVector());
        double distance = toPlayer.length();
        if (distance <= 0.75) {
            return;
        }

        Vector direction = toPlayer.clone().normalize();
        double requestedDistance = Math.min(maximumDistance, distance - 0.75);
        RayTraceResult collision = hook.getWorld().rayTraceBlocks(
                start, direction, requestedDistance, FluidCollisionMode.NEVER, true);
        double allowedDistance = requestedDistance;
        if (collision != null) {
            double hitDistance = collision.getHitPosition().distance(start.toVector());
            allowedDistance = Math.min(allowedDistance, Math.max(0.0, hitDistance - 0.2));
        }
        if (allowedDistance <= 0.05) {
            debug("Hook pull stopped by a block for " + player.getName() + ".");
            return;
        }

        Location destination = start.clone().add(direction.multiply(allowedDistance));
        if (hook.teleport(destination)) {
            hook.setVelocity(new Vector());
        }
    }

    private void succeed(Player player, FishingMinigameSession session) {
        sounds.play(player, "minigame-success");
        session.state(FishingSessionState.CATCH_COMPLETED);
        player.sendActionBar(Component.empty());
        startOutcomeEffect(player, session, successEffect);
    }

    private void startActiveMinigame(Player player, FishingMinigameSession session) {
        if (session.state() != FishingSessionState.WAITING_FOR_BITE) {
            return;
        }
        session.state(FishingSessionState.MINIGAME_ACTIVE);
        render(player, session);
    }

    public boolean captureDeferredCatch(PlayerFishEvent event) {
        FishingMinigameSession session = findSession(event);
        if (session == null || session.state() != FishingSessionState.WAITING_FOR_BITE) {
            return false;
        }
        if (event.isCancelled()) {
            debug("CAUGHT_FISH was already cancelled before deferred capture for "
                    + event.getPlayer().getName() + ".");
            cancel(session.playerId(), false);
            return true;
        }
        if (!(event.getCaught() instanceof Item item)) {
            debug("Ignoring CAUGHT_FISH without an Item for " + event.getPlayer().getName() + ".");
            cancel(session.playerId(), false);
            return true;
        }
        session.deferCatch(item, event.getExpToDrop());
        if (valhallaExperienceBridge != null) {
            valhallaExperienceBridge.capturePreparedLoot(event.getPlayer(), session);
        }
        event.setCancelled(true);
        item.remove();
        suppressVanillaBites(session.hook(), true);
        session.state(FishingSessionState.MINIGAME_ACTIVE);
        debug("Deferred original catch for " + event.getPlayer().getName()
                + "; item=" + item.getItemStack().getType()
                + ", exp=" + event.getExpToDrop() + ".");
        render(event.getPlayer(), session);
        return true;
    }

    public void prepareDeferredCatch(PlayerFishEvent event) {
        FishingMinigameSession session = findSession(event);
        if (session != null && session.state() == FishingSessionState.WAITING_FOR_BITE) {
            session.prepareValhallaExperience();
        }
    }

    public void protectUnexpectedCatch(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }
        FishingMinigameSession session = findSession(event);
        if (session != null && session.state() == FishingSessionState.MINIGAME_ACTIVE) {
            event.setCancelled(true);
            if (event.getCaught() instanceof Item item) {
                item.remove();
            }
            if (session.hasDeferredCatch()) {
                debug("Ignored an unexpected second CAUGHT_FISH for " + event.getPlayer().getName()
                        + "; the original deferred catch remains protected.");
                return;
            }
            cancel(session.playerId(), false);
        }
    }

    public void observeFishingEvent(PlayerFishEvent event) {
        FishingMinigameSession session = findSession(event);
        if (session == null) {
            return;
        }
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH
                || event.getState() == PlayerFishEvent.State.REEL_IN) {
            debug("Observed " + event.getState() + " for " + event.getPlayer().getName()
                    + "; state=" + session.state()
                    + ", cancelled=" + event.isCancelled()
                    + ", caught=" + (event.getCaught() == null
                    ? "none" : event.getCaught().getType()));
        }
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH
                && session.state() == FishingSessionState.MINIGAME_ACTIVE
                && !session.hasDeferredCatch()) {
            cancel(session.playerId(), false);
        } else if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH
                && session.state() == FishingSessionState.WAITING_FOR_REEL) {
            if (event.isCancelled()) {
                plugin.getLogger().warning("Standard CAUGHT_FISH was cancelled while completing a NekaraRPG catch for "
                        + event.getPlayer().getName() + "; no replacement loot or XP will be generated.");
                cancel(session.playerId(), false);
            } else if (event.getCaught() instanceof org.bukkit.entity.Item) {
                completeCatch(event.getPlayer(), session);
            } else {
                plugin.getLogger().warning("CAUGHT_FISH did not contain an Item; leaving server behavior untouched.");
                removeSession(session.playerId(), true);
            }
        } else if (event.getState() == PlayerFishEvent.State.REEL_IN
                && session.state() != FishingSessionState.CATCH_COMPLETED
                && !session.hasDeferredCatch()) {
            cancel(session.playerId(), false);
        }
    }

    private void completeCatch(Player player, FishingMinigameSession session) {
        session.state(FishingSessionState.CATCH_COMPLETED);
        sounds.play(player, "catch-success");
        player.sendActionBar(Component.empty());
        ItemStack originalCatch = session.takeDeferredCatch();
        if (originalCatch != null) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(originalCatch);
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            int extraDropCount = 0;
            if (valhallaExperienceBridge != null) {
                var extraDrops = session.takeDeferredValhallaExtraDrops();
                extraDropCount = extraDrops.size();
                for (ItemStack extraDrop : extraDrops) {
                    Map<Integer, ItemStack> extraLeftovers = player.getInventory().addItem(extraDrop);
                    for (ItemStack leftover : extraLeftovers.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                    }
                }
            }
            if (session.vanillaExpToDrop() > 0) {
                player.giveExp(session.vanillaExpToDrop());
            }
            if (valhallaExperienceBridge != null) {
                valhallaExperienceBridge.deliver(player, session);
            }
            debug("Delivered original deferred catch to " + player.getName()
                    + "; item=" + originalCatch.getType()
                    + ", exp=" + session.vanillaExpToDrop()
                    + ", valhallaExtraDrops=" + extraDropCount + ".");
        }
        Bukkit.getScheduler().runTask(plugin, () -> removeCompletedSession(player.getUniqueId(), session));
    }

    private FishingMinigameSession findSession(PlayerFishEvent event) {
        FishingMinigameSession session = sessions.get(event.getPlayer().getUniqueId());
        if (session == null || session.hookId() == null) {
            return null;
        }
        return session.hookId().equals(event.getHook().getUniqueId()) ? session : null;
    }

    private void timeout(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        FishingMinigameSession session = sessions.get(playerId);
        if (player == null || session == null) {
            removeSession(playerId, false);
            return;
        }
        sounds.play(player, "timeout");
        fail(player, session, "escape", "escape");
    }

    private void fail(Player player, FishingMinigameSession session, String messageKey, String soundKey) {
        session.state(FishingSessionState.FAILED);
        sounds.play(player, soundKey);
        messages.send(player, messageKey, placeholders(session));
        session.removeProgressBar();
        player.sendActionBar(Component.empty());
        startOutcomeEffect(player, session, failureEffect);
    }

    public void cancel(UUID playerId, boolean notify) {
        FishingMinigameSession session = sessions.get(playerId);
        if (session == null) {
            return;
        }
        session.state(FishingSessionState.CANCELLED);
        removeSession(playerId, true);
    }

    public boolean cancelByCommand(UUID playerId, CommandSender requester, String targetName) {
        if (!isActive(playerId)) {
            return false;
        }
        cancel(playerId, true);
        if (!(requester instanceof Player player) || !playerId.equals(player.getUniqueId())) {
            messages.send(requester, "player-cancelled", Map.of("player", targetName));
        }
        return true;
    }

    public void cancelAll(boolean notify) {
        for (UUID playerId : new ArrayList<>(sessions.keySet())) {
            cancel(playerId, notify);
        }
    }

    public int activeCount() {
        return sessions.size();
    }

    public FishingMinigameSession session(UUID playerId) {
        return sessions.get(playerId);
    }

    public String modeName() {
        return compatibilityStrategy.modeName();
    }

    public void shutdown() {
        stopTicker();
        cancelAll(false);
    }

    private void removeSession(UUID playerId, boolean clearActionBar) {
        FishingMinigameSession session = sessions.remove(playerId);
        if (session == null) {
            return;
        }
        session.removeProgressBar();
        if (session.hook() != null && session.hook().isValid()) {
            session.hook().remove();
        }
        if (clearActionBar) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendActionBar(Component.empty());
            }
        }
    }

    private void render(Player player, FishingMinigameSession session) {
        updateProgressBar(player, session);
        player.sendActionBar(renderer.render(session.engine()));
    }

    private void updateProgressBar(Player player, FishingMinigameSession session) {
        BossBar bar = session.progressBar();
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_10);
            bar.setVisible(true);
            bar.addPlayer(player);
            session.progressBar(bar);
        }
        double progress = (double) session.engine().hits() / session.engine().requiredHits();
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
    }

    private void removeCompletedSession(UUID playerId, FishingMinigameSession completedSession) {
        if (sessions.remove(playerId, completedSession) && completedSession.hook() != null
                && completedSession.hook().isValid()) {
            completedSession.hook().remove();
        }
        completedSession.removeProgressBar();
    }

    public void debug(String message) {
        if (debug) {
            plugin.getLogger().info("[debug] " + message);
        }
    }

    private Map<String, Object> placeholders(FishingMinigameSession session) {
        MinigameEngine engine = session.engine();
        return Map.of(
                "hits", engine.hits(),
                "required_hits", engine.requiredHits(),
                "misses", engine.misses(),
                "max_misses", engine.config().maxMisses(),
                "time_left", String.format("%.1f", engine.timeLeftTicks() / 20.0)
        );
    }
}
