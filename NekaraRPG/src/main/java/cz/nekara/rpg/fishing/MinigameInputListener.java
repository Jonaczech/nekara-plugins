package cz.nekara.rpg.fishing;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.minigame.FishingMinigameManager;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

public final class MinigameInputListener implements Listener {
    private final NekaraRPGPlugin plugin;
    private final FishingMinigameManager manager;

    public MinigameInputListener(NekaraRPGPlugin plugin, FishingMinigameManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        UUID playerId = event.getPlayer().getUniqueId();
        if (!manager.isMinigameActive(playerId)) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.FISHING_ROD) {
            if (plugin.configuration().get().fishing().cancelOnItemChange()) {
                manager.cancel(playerId, true);
            }
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        manager.handleClick(event.getPlayer());
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        cancelOnItemChange(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (manager.isActive(event.getPlayer().getUniqueId())) {
            manager.cancel(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        cancelOnItemChange(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof org.bukkit.entity.Player player) {
            cancel(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player && manager.isActive(player.getUniqueId())) {
            event.setCancelled(true);
            manager.cancel(player.getUniqueId(), true);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (plugin.configuration().get().fishing().cancelOnTeleport()) {
            cancel(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (plugin.configuration().get().fishing().cancelOnWorldChange()) {
            cancel(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        cancel(event.getEntity().getUniqueId(), false);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        cancel(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        var fishing = plugin.configuration().get().fishing();
        if ((event.getNewGameMode() == org.bukkit.GameMode.SPECTATOR && !fishing.allowSpectator())
                || (event.getNewGameMode() == org.bukkit.GameMode.CREATIVE && !fishing.allowCreative())) {
            cancel(event.getPlayer().getUniqueId(), true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Player player
                && plugin.configuration().get().fishing().cancelOnDamage()) {
            cancel(player.getUniqueId(), true);
        }
    }

    private void cancelOnItemChange(UUID playerId) {
        if (plugin.configuration().get().fishing().cancelOnItemChange()) {
            cancel(playerId, true);
        }
    }

    private void cancel(UUID playerId, boolean notify) {
        manager.cancel(playerId, notify);
    }
}
