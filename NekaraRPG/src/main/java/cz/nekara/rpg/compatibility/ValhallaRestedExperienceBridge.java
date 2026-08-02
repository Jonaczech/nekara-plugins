package cz.nekara.rpg.compatibility;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.RestedValhallaConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Predicate;

/** Optional reflection-based Rested XP integration for ValhallaMMO. */
public final class ValhallaRestedExperienceBridge implements Listener {
    private static final String VALHALLA_PLUGIN = "ValhallaMMO";
    private static final String EXPERIENCE_EVENT =
            "me.athlaeos.valhallammo.event.PlayerSkillExperienceGainEvent";

    private final NekaraRPGPlugin plugin;
    private final Predicate<Player> isEligiblePlayer;
    private boolean registered;
    private Method getPlayer;
    private Method getAmount;
    private Method setAmount;
    private Method getReason;

    public ValhallaRestedExperienceBridge(
            NekaraRPGPlugin plugin,
            Predicate<Player> isEligiblePlayer
    ) {
        this.plugin = plugin;
        this.isEligiblePlayer = isEligiblePlayer;
    }

    public void register() {
        if (registered) {
            return;
        }
        Plugin valhalla = Bukkit.getPluginManager().getPlugin(VALHALLA_PLUGIN);
        if (valhalla == null || !valhalla.isEnabled()) {
            return;
        }
        try {
            ClassLoader loader = valhalla.getClass().getClassLoader();
            Class<?> experienceEventClass = Class.forName(EXPERIENCE_EVENT, true, loader);
            getPlayer = experienceEventClass.getMethod("getPlayer");
            getAmount = experienceEventClass.getMethod("getAmount");
            setAmount = experienceEventClass.getMethod("setAmount", double.class);
            getReason = experienceEventClass.getMethod("getReason");

            @SuppressWarnings("unchecked")
            Class<? extends Event> eventType = (Class<? extends Event>) experienceEventClass;
            EventExecutor executor = (listener, event) -> handle(event);
            Bukkit.getPluginManager().registerEvent(
                    eventType, this, EventPriority.HIGHEST, executor, plugin, true);
            registered = true;
            plugin.getLogger().info("ValhallaMMO Rested XP bridge enabled.");
        } catch (ReflectiveOperationException | LinkageError exception) {
            registered = false;
            plugin.getLogger().warning("ValhallaMMO was detected, but its Rested XP API could not be loaded; "
                    + "skill XP will remain unchanged. Details: " + exception.getMessage());
        }
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        registered = false;
    }

    private void handle(Event event) {
        if (!registered) {
            return;
        }
        try {
            Player player = (Player) getPlayer.invoke(event);
            if (!isEligiblePlayer.test(player)) {
                return;
            }
            Object reason = getReason.invoke(event);
            if (reason == null || !ValhallaRestedExperienceMath.isEligibleReason(reason.toString())) {
                return;
            }
            RestedValhallaConfig config = plugin.configuration().get().campfire().restedValhalla();
            if (!config.enabled()) {
                return;
            }
            double amount = ((Number) getAmount.invoke(event)).doubleValue();
            double scaled = ValhallaRestedExperienceMath.applyMultiplier(
                    amount, config.experienceMultiplier());
            if (Double.compare(amount, scaled) != 0) {
                setAmount.invoke(event, scaled);
            }
        } catch (ReflectiveOperationException | ClassCastException exception) {
            plugin.getLogger().warning("Could not apply the ValhallaMMO Rested XP bonus: "
                    + exception.getMessage());
        }
    }
}
