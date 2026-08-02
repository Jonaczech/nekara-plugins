package cz.nekara.rpg.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/** Optional reflection bridge for ValhallaMMO Mining XP and prepared drops. */
public final class ValhallaMiningBridge implements Listener {
    private static final String VALHALLA_PLUGIN = "ValhallaMMO";
    private static final String EXPERIENCE_EVENT =
            "me.athlaeos.valhallammo.event.PlayerSkillExperienceGainEvent";
    private static final String MINING_SKILL =
            "me.athlaeos.valhallammo.skills.skills.implementations.MiningSkill";

    private final JavaPlugin plugin;
    private final BiFunction<Player, Double, Runnable> experienceCaptureFactory;
    private boolean registered;
    private Object miningSkill;
    private Class<?> miningSkillClass;
    private Method getPlayer;
    private Method getAmount;
    private Method getLeveledSkill;
    private Method getReason;
    private Method getProfileType;
    private Method getPersistentProfile;
    private Method getTotalExperience;
    private Method addExperience;
    private Object pluginExperienceReason;
    private Method getPreparedExtraDrops;

    public ValhallaMiningBridge(
            JavaPlugin plugin,
            BiFunction<Player, Double, Runnable> experienceCaptureFactory
    ) {
        this.plugin = plugin;
        this.experienceCaptureFactory = experienceCaptureFactory;
    }

    public void register() {
        if (registered) {
            return;
        }
        Plugin valhalla = Bukkit.getPluginManager().getPlugin(VALHALLA_PLUGIN);
        if (valhalla == null || !valhalla.isEnabled()) {
            plugin.getLogger().warning("Echo Vein requires ValhallaMMO Mining; automatic triggers are unavailable.");
            return;
        }

        try {
            ClassLoader loader = valhalla.getClass().getClassLoader();
            Class<?> experienceEventClass = Class.forName(EXPERIENCE_EVENT, true, loader);
            miningSkillClass = Class.forName(MINING_SKILL, true, loader);
            Class<?> skillRegistry = Class.forName(
                    "me.athlaeos.valhallammo.skills.skills.SkillRegistry", true, loader);
            Method registryGetSkill = skillRegistry.getMethod("getSkill", Class.class);
            miningSkill = registryGetSkill.invoke(null, miningSkillClass);

            getPlayer = experienceEventClass.getMethod("getPlayer");
            getAmount = experienceEventClass.getMethod("getAmount");
            getLeveledSkill = experienceEventClass.getMethod("getLeveledSkill");
            getReason = experienceEventClass.getMethod("getReason");

            getProfileType = miningSkill.getClass().getMethod("getProfileType");
            Class<?> profileType = (Class<?>) getProfileType.invoke(miningSkill);
            Class<?> profileRegistry = Class.forName(
                    "me.athlaeos.valhallammo.playerstats.profiles.ProfileRegistry", true, loader);
            getPersistentProfile = profileRegistry.getMethod("getPersistentProfile", Player.class, Class.class);
            Class<?> profileClass = Class.forName(
                    "me.athlaeos.valhallammo.playerstats.profiles.Profile", true, loader);
            getTotalExperience = profileClass.getMethod("getTotalEXP");

            Class<?> reasonClass = getReason.getReturnType();
            addExperience = miningSkill.getClass().getMethod(
                    "addEXP", Player.class, double.class, boolean.class, reasonClass);
            pluginExperienceReason = enumConstant(reasonClass, "PLUGIN");

            Class<?> lootListener = Class.forName(
                    "me.athlaeos.valhallammo.listeners.LootListener", true, loader);
            getPreparedExtraDrops = lootListener.getMethod("getPreparedExtraDrops", Block.class);
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) experienceEventClass;
            EventExecutor executor = (listener, event) -> handleExperience(event);
            Bukkit.getPluginManager().registerEvent(
                    eventType, this, EventPriority.MONITOR, executor, plugin, true);
            registered = profileType != null;
            plugin.getLogger().info("ValhallaMMO Echo Vein bridge enabled for Mining.");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            unregister();
            plugin.getLogger().warning("ValhallaMMO Mining could not be connected to Echo Vein; "
                    + "automatic triggers are unavailable. Details: " + exception.getMessage());
        }
    }

    private static Object enumConstant(Class<?> enumClass, String name) throws ReflectiveOperationException {
        if (!enumClass.isEnum()) {
            throw new ReflectiveOperationException(enumClass.getName() + " is not an enum");
        }
        for (Object constant : enumClass.getEnumConstants()) {
            if (name.equals(constant.toString())) {
                return constant;
            }
        }
        throw new ReflectiveOperationException("ValhallaMMO XP reason " + name + " is unavailable");
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        registered = false;
    }

    public boolean isAvailable() {
        return registered;
    }

    private double totalExperience(Player player) throws ReflectiveOperationException {
        Class<?> profileType = (Class<?>) getProfileType.invoke(miningSkill);
        Object profile = getPersistentProfile.invoke(null, player, profileType);
        if (profile == null) {
            return 0.0;
        }
        Object value = getTotalExperience.invoke(profile);
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    public boolean grantBonusExperience(Player player, double amount) {
        if (!registered || !Double.isFinite(amount) || amount <= 0.0) {
            return false;
        }
        try {
            addExperience.invoke(miningSkill, player, amount, false, pluginExperienceReason);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not grant Echo Vein Mining XP to "
                    + player.getName() + ": " + exception.getMessage());
            return false;
        }
    }

    public List<ItemStack> preparedDrops(Block block) {
        if (!registered) {
            return List.of();
        }
        try {
            Object value = getPreparedExtraDrops.invoke(null, block);
            if (!(value instanceof List<?> drops)) {
                return List.of();
            }
            List<ItemStack> result = new ArrayList<>();
            for (Object drop : drops) {
                if (drop instanceof ItemStack item && !item.getType().isAir() && item.getAmount() > 0) {
                    result.add(item.clone());
                }
            }
            return List.copyOf(result);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            plugin.getLogger().warning("Could not capture ValhallaMMO Mining drops: " + exception.getMessage());
            return List.of();
        }
    }

    private void handleExperience(Event event) {
        if (!registered) {
            return;
        }
        try {
            Object reason = getReason.invoke(event);
            Object skill = getLeveledSkill.invoke(event);
            if (reason == null || !"SKILL_ACTION".equals(reason.toString())
                    || skill == null || !miningSkillClass.isInstance(skill)) {
                return;
            }
            double amount = ((Number) getAmount.invoke(event)).doubleValue();
            if (Double.isFinite(amount) && amount > 0.0) {
                Player player = (Player) getPlayer.invoke(event);
                double before = totalExperience(player);
                Runnable capture = experienceCaptureFactory.apply(player, amount);
                if (capture != null) {
                    Bukkit.getScheduler().runTask(
                            plugin, () -> confirmExperience(player, amount, before, capture));
                }
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {
            plugin.getLogger().warning("Could not observe ValhallaMMO Mining XP: " + exception.getMessage());
        }
    }

    private void confirmExperience(Player player, double amount, double before, Runnable capture) {
        if (!registered || !player.isOnline()) {
            return;
        }
        try {
            double after = totalExperience(player);
            if (Double.isFinite(after) && after + 0.000_001 >= before + amount) {
                capture.run();
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {
            plugin.getLogger().warning("Could not confirm ValhallaMMO Mining XP: " + exception.getMessage());
        }
    }
}
