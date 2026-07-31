package cz.nekara.fishing.fishing;

import cz.nekara.fishing.NekaraFishingPlugin;
import cz.nekara.fishing.configuration.PluginConfig;
import cz.nekara.fishing.minigame.FishingMinigameManager;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

public final class FishingListener implements Listener {
    private final NekaraFishingPlugin plugin;
    private final FishingMinigameManager manager;

    public FishingListener(NekaraFishingPlugin plugin, FishingMinigameManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void prepareCatch(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH && !event.isCancelled()) {
            manager.prepareDeferredCatch(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.BITE) {
            if (event.isCancelled()) {
                return;
            }
            Player player = event.getPlayer();
            PluginConfig config = plugin.configuration().get();
            if (!config.minigame().enabled()
                    || !player.hasPermission("nekarafishing.use")
                    || player.hasPermission("nekarafishing.bypass")
                    || !config.worlds().isEnabled(player.getWorld().getName())
                    || !isAllowedGameMode(player, config)
                    || !(event.getHook() instanceof FishHook hook)
                    || hook.getHookedEntity() != null) {
                return;
            }
            if (config.fishing().requireFishingRodInMainHand()
                    && player.getInventory().getItemInMainHand().getType() != org.bukkit.Material.FISHING_ROD) {
                return;
            }
            if (!manager.startFromBite(player, hook)) {
                event.setCancelled(true);
                if (manager.isMinigameActive(player.getUniqueId())) {
                    manager.suppressVanillaBite(hook);
                }
                plugin.getLogger().fine("Ignored an additional fishing bite for " + player.getName()
                        + " while a minigame was already active.");
            }
            return;
        }
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH
                && !manager.captureDeferredCatch(event)) {
            manager.protectUnexpectedCatch(event);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void observeFishingEvent(PlayerFishEvent event) {
        manager.observeFishingEvent(event);
    }

    private boolean isAllowedGameMode(Player player, PluginConfig config) {
        return (player.getGameMode() != org.bukkit.GameMode.CREATIVE || config.fishing().allowCreative())
                && (player.getGameMode() != org.bukkit.GameMode.SPECTATOR || config.fishing().allowSpectator());
    }
}
