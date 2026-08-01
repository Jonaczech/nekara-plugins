package cz.nekara.rpg.compatibility;

import cz.nekara.rpg.minigame.FishingMinigameManager;
import cz.nekara.rpg.minigame.FishingMinigameSession;
import cz.nekara.rpg.configuration.MinigameConfig;
import cz.nekara.rpg.configuration.ValhallaFishingConfig;
import cz.nekara.rpg.configuration.ValhallaFishingTier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * Optional, reflection-based bridge for ValhallaMMO's public skill XP event/API.
 * The plugin remains loadable when ValhallaMMO is not installed.
 */
public final class ValhallaExperienceBridge implements Listener {
    private static final String VALHALLA_PLUGIN = "ValhallaMMO";
    private static final String EXPERIENCE_EVENT =
            "me.athlaeos.valhallammo.event.PlayerSkillExperienceGainEvent";
    private static final String FISHING_SKILL =
            "me.athlaeos.valhallammo.skills.skills.implementations.FishingSkill";
    private static final String SKILL_BASE =
            "me.athlaeos.valhallammo.skills.skills.Skill";

    private final JavaPlugin plugin;
    private final FishingMinigameManager manager;
    private boolean registered;
    private Class<?> experienceEventClass;
    private Method getPlayer;
    private Method getAmount;
    private Method getLeveledSkill;
    private Method getReason;
    private Method setCancelled;
    private Method getPreparedFishingDrops;
    private Class<?> fishingSkillClass;
    private Class<?> fishingProfileClass;
    private Object fishingSkill;
    private Method skillRegistryGetSkill;
    private Method skillGetProfileType;
    private Method skillGetMaxLevel;
    private Method profileRegistryGetSkillProfile;
    private Method profileRegistryGetMergedProfile;
    private Method profileRegistryGetPersistentProfile;
    private Method profileGetLevel;

    public ValhallaExperienceBridge(JavaPlugin plugin, FishingMinigameManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void register() {
        Plugin valhalla = Bukkit.getPluginManager().getPlugin(VALHALLA_PLUGIN);
        if (valhalla == null || !valhalla.isEnabled()) {
            return;
        }
        try {
            ClassLoader loader = valhalla.getClass().getClassLoader();
            experienceEventClass = Class.forName(EXPERIENCE_EVENT, true, loader);
            getPlayer = experienceEventClass.getMethod("getPlayer");
            getAmount = experienceEventClass.getMethod("getAmount");
            getLeveledSkill = experienceEventClass.getMethod("getLeveledSkill");
            getReason = experienceEventClass.getMethod("getReason");
            setCancelled = experienceEventClass.getMethod("setCancelled", boolean.class);
            Class<?> lootListener = Class.forName(
                    "me.athlaeos.valhallammo.listeners.LootListener", true, loader);
            getPreparedFishingDrops = lootListener.getMethod("getPreparedFishingDrops");
            prepareFishingDifficultyAccess(loader);

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) experienceEventClass;
            EventExecutor executor = (listener, event) -> handle(event);
            Bukkit.getPluginManager().registerEvent(
                    eventType, this, EventPriority.MONITOR, executor, plugin, false);
            registered = true;
            plugin.getLogger().info("ValhallaMMO fishing XP bridge enabled; XP is deferred until catch delivery.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            plugin.getLogger().warning("ValhallaMMO was detected, but its public fishing XP API could not be loaded; "
                    + "ValhallaMMO XP will keep its normal timing. Details: " + exception.getMessage());
        }
    }

    private void prepareFishingDifficultyAccess(ClassLoader loader) {
        try {
            fishingSkillClass = Class.forName(FISHING_SKILL, true, loader);
            Class<?> skillRegistry = Class.forName(
                    "me.athlaeos.valhallammo.skills.skills.SkillRegistry", true, loader);
            skillRegistryGetSkill = skillRegistry.getMethod("getSkill", Class.class);
            fishingSkill = skillRegistryGetSkill.invoke(null, fishingSkillClass);
            if (fishingSkill == null) {
                throw new ReflectiveOperationException("FishingSkill is not registered");
            }
            skillGetProfileType = fishingSkill.getClass().getMethod("getProfileType");
            skillGetMaxLevel = fishingSkill.getClass().getMethod("getMaxLevel");
            fishingProfileClass = (Class<?>) skillGetProfileType.invoke(fishingSkill);

            Class<?> profileRegistry = Class.forName(
                    "me.athlaeos.valhallammo.playerstats.profiles.ProfileRegistry", true, loader);
            profileRegistryGetSkillProfile = profileRegistry.getMethod(
                    "getSkillProfile", Player.class, Class.class);
            profileRegistryGetMergedProfile = profileRegistry.getMethod(
                    "getMergedProfile", Player.class, Class.class);
            profileRegistryGetPersistentProfile = profileRegistry.getMethod(
                    "getPersistentProfile", Player.class, Class.class);
            Class<?> profileClass = Class.forName(
                    "me.athlaeos.valhallammo.playerstats.profiles.Profile", true, loader);
            profileGetLevel = profileClass.getMethod("getLevel");
            plugin.getLogger().info("ValhallaMMO fishing difficulty scaling enabled; fishing level affects minigame difficulty.");
        } catch (ReflectiveOperationException | ClassCastException exception) {
            plugin.getLogger().warning("ValhallaMMO fishing difficulty scaling is unavailable; using base minigame difficulty. "
                    + "Details: " + exception.getMessage());
            fishingSkill = null;
        }
    }

    public MinigameConfig applyFishingDifficulty(Player player, MinigameConfig base,
                                                  ValhallaFishingConfig configuration) {
        if (!configuration.enabled() || fishingSkill == null) {
            return base;
        }
        try {
            int level = Math.max(
                    readProfileLevel(profileRegistryGetMergedProfile.invoke(null, player, fishingProfileClass)),
                    Math.max(
                            readProfileLevel(profileRegistryGetSkillProfile.invoke(null, player, fishingProfileClass)),
                            readProfileLevel(profileRegistryGetPersistentProfile.invoke(null, player, fishingProfileClass))));
            if (level <= 0) {
                return base;
            }
            int maxLevel = Math.max(1, ((Number) skillGetMaxLevel.invoke(fishingSkill)).intValue());
            ValhallaFishingTier tier = configuration.tierForLevel(level);
            int requiredMin = tier.requiredHitsMin();
            int requiredMax = tier.requiredHitsMax();
            int maxMisses = tier.maxMisses();
            if (level >= maxLevel) {
                requiredMin = configuration.maxLevelRequiredHits();
                requiredMax = configuration.maxLevelRequiredHits();
                maxMisses = configuration.maxLevelMaxMisses();
            }

            manager.debug("Applied ValhallaMMO fishing difficulty for " + player.getName()
                    + ": level=" + level + "/" + maxLevel
                    + ", tier=" + tier.name()
                    + ", requiredHits=" + requiredMin + "-" + requiredMax
                    + ", maxMisses=" + maxMisses + ".");
            return base.withDifficulty(requiredMin, requiredMax, maxMisses);
        } catch (ReflectiveOperationException | ClassCastException | ArithmeticException exception) {
            manager.debug("Could not read ValhallaMMO fishing level; using base minigame difficulty. "
                    + exception.getMessage());
            return base;
        }
    }

    private int readProfileLevel(Object profile) throws ReflectiveOperationException {
        if (profile == null) {
            return 0;
        }
        Object value = profileGetLevel.invoke(profile);
        return value instanceof Number number ? Math.max(0, number.intValue()) : 0;
    }

    /**
     * Captures ValhallaMMO's prepared extra fishing drops before its MONITOR
     * listener skips the cancelled PlayerFishEvent.
     */
    public void capturePreparedLoot(Player player, FishingMinigameSession session) {
        if (!registered || getPreparedFishingDrops == null) {
            return;
        }
        try {
            Object prepared = getPreparedFishingDrops.invoke(null);
            if (!(prepared instanceof Map<?, ?> dropsByPlayer)) {
                return;
            }
            Object value = dropsByPlayer.remove(player.getUniqueId());
            if (!(value instanceof List<?> drops)) {
                return;
            }
            int captured = 0;
            for (Object drop : drops) {
                if (drop instanceof ItemStack item) {
                    session.deferValhallaExtraDrop(item);
                    captured++;
                }
            }
            if (captured > 0) {
                manager.debug("Deferred " + captured + " ValhallaMMO extra fishing drop(s) for "
                        + player.getName() + ".");
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {
            plugin.getLogger().warning("Could not capture ValhallaMMO extra fishing drops for "
                    + player.getName() + ". Details: " + exception.getMessage());
        }
    }

    private void handle(Event event) {
        if (!(event instanceof Cancellable cancellable) || !registered || cancellable.isCancelled()) {
            return;
        }
        try {
            Player player = (Player) getPlayer.invoke(event);
            FishingMinigameSession session = manager.session(player.getUniqueId());
            if (session == null || !session.awaitingValhallaExperience()
                    || session.valhallaReplayInProgress()) {
                return;
            }
            Object skill = getLeveledSkill.invoke(event);
            if (skill == null || !FISHING_SKILL.equals(skill.getClass().getName())) {
                return;
            }
            double amount = ((Number) getAmount.invoke(event)).doubleValue();
            Object reason = getReason.invoke(event);
            session.deferValhallaExperience(amount, skill, reason);
            setCancelled.invoke(event, true);
            manager.debug("Deferred ValhallaMMO fishing XP for " + player.getName()
                    + "; amount=" + amount + ".");
        } catch (ReflectiveOperationException | ClassCastException exception) {
            plugin.getLogger().warning("Could not defer ValhallaMMO fishing XP: " + exception.getMessage());
        }
    }

    public void deliver(Player player, FishingMinigameSession session) {
        double amount = session.deferredValhallaExperience();
        Object skill = session.valhallaSkill();
        Object reason = session.valhallaExperienceReason();
        if (amount <= 0.0 || skill == null || reason == null) {
            return;
        }
        try {
            session.valhallaReplayInProgress(true);
            MethodHandle baseAddExperience = baseAddExperienceHandle(skill, reason.getClass());
            double baseAmount = amount / globalExperienceMultiplier(player, reason, skill);
            baseAddExperience.invokeWithArguments(player, baseAmount, false, reason);
            session.clearDeferredValhallaExperience();
            manager.debug("Delivered ValhallaMMO fishing XP to " + player.getName()
                    + "; amount=" + amount + ".");
        } catch (Throwable exception) {
            plugin.getLogger().warning("Could not deliver deferred ValhallaMMO fishing XP to "
                    + player.getName() + "; the catch was delivered, but profession XP was skipped. Details: "
                    + exception.getMessage());
        } finally {
            session.valhallaReplayInProgress(false);
        }
    }

    private MethodHandle baseAddExperienceHandle(Object skill, Class<?> reasonClass) throws Throwable {
        Class<?> skillClass = Class.forName(SKILL_BASE, true, skill.getClass().getClassLoader());
        Method method = skillClass.getMethod(
                "addEXP", Player.class, double.class, boolean.class, reasonClass);
        return MethodHandles.privateLookupIn(skill.getClass(), MethodHandles.lookup())
                .unreflectSpecial(method, skill.getClass())
                .bindTo(skill);
    }

    private double globalExperienceMultiplier(Player player, Object reason, Object skill) {
        try {
            Method isExperienceScaling = skill.getClass().getMethod("isExperienceScaling");
            String reasonName = reason.toString();
            if (!(boolean) isExperienceScaling.invoke(skill)
                    || !("SKILL_ACTION".equals(reasonName) || "EXP_SHARE".equals(reasonName))) {
                return 1.0;
            }
            ClassLoader loader = skill.getClass().getClassLoader();
            Class<?> stats = Class.forName(
                    "me.athlaeos.valhallammo.playerstats.AccumulativeStatManager", true, loader);
            Method getCachedStats = stats.getMethod(
                    "getCachedStats", String.class, org.bukkit.entity.Entity.class, long.class, boolean.class);
            double bonus = ((Number) getCachedStats.invoke(null, "GLOBAL_EXP_GAIN", player, 10000L, true))
                    .doubleValue();
            double multiplier = 1.0 + bonus;
            return Double.isFinite(multiplier) && multiplier > 0.0 ? multiplier : 1.0;
        } catch (ReflectiveOperationException | ClassCastException exception) {
            manager.debug("Could not read ValhallaMMO global XP multiplier; replaying unchanged amount.");
            return 1.0;
        }
    }
}
