package cz.nekara.rpg.menu;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.ModuleRegistry;
import cz.nekara.rpg.modules.auth.AuthModule;
import cz.nekara.rpg.modules.campfire.CampfireModule;
import cz.nekara.rpg.modules.fishing.FishingModule;
import cz.nekara.rpg.modules.mining.MiningModule;
import cz.nekara.rpg.modules.mounts.MountsModule;
import cz.nekara.rpg.modules.sitting.SittingModule;
import cz.nekara.rpg.sitting.SitResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NekaraRPGMenu implements Listener {
    private static final int AUTH_SLOT = 10;
    private static final int FISHING_SLOT = 11;
    private static final int SITTING_SLOT = 12;
    private static final int CAMPFIRE_SLOT = 14;
    private static final int MINING_SLOT = 15;
    private static final int MOUNTS_SLOT = 16;

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final ModuleRegistry modules;
    private final AuthModule auth;
    private final FishingModule fishing;
    private final SittingModule sitting;
    private final CampfireModule campfire;
    private final MountsModule mounts;

    public NekaraRPGMenu(
            NekaraRPGPlugin plugin,
            MessageService messages,
            ModuleRegistry modules,
            AuthModule auth,
            FishingModule fishing,
            SittingModule sitting,
            CampfireModule campfire,
            MountsModule mounts
    ) {
        this.plugin = plugin;
        this.messages = messages;
        this.modules = modules;
        this.auth = auth;
        this.fishing = fishing;
        this.sitting = sitting;
        this.campfire = campfire;
        this.mounts = mounts;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player player) {
        if (modules.isEnabled(AuthModule.ID) && !auth.isAuthenticated(player)) {
            auth.openMenu(player);
            return;
        }

        MainMenuHolder holder = new MainMenuHolder(player.getUniqueId());
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("NekaraRPG — hlavní menu", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        fill(inventory);

        int visibleModules = 0;
        if (canShow(player, AuthModule.ID, null)) {
            inventory.setItem(AUTH_SLOT, item(Material.PLAYER_HEAD,
                    Component.text("Účet", NamedTextColor.AQUA),
                    Component.text("Přihlášen jako " + player.getName(), NamedTextColor.GRAY),
                    Component.text("Heslo, relace a odhlášení", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (canShow(player, FishingModule.ID, "nekararpg.use")) {
            boolean active = fishing.minigames().isMinigameActive(player.getUniqueId());
            inventory.setItem(FISHING_SLOT, item(Material.FISHING_ROD,
                    Component.text("Rybaření", NamedTextColor.BLUE),
                    Component.text(active ? "Minihra právě probíhá" : "Rybářská minihra je připravená",
                            active ? NamedTextColor.YELLOW : NamedTextColor.GRAY),
                    Component.text("Spouští se přirozeným záběrem", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (canShow(player, SittingModule.ID, "nekararpg.sitting.use")) {
            boolean seated = sitting.isSeated(player);
            inventory.setItem(SITTING_SLOT, item(seated ? Material.LIME_DYE : Material.OAK_STAIRS,
                    Component.text(seated ? "Vstát" : "Sednout si", NamedTextColor.GREEN),
                    Component.text(seated ? "Kliknutím ukončíš sezení" : "Kliknutím se posadíš",
                            NamedTextColor.GRAY)));
            visibleModules++;
        }
        if (canShow(player, CampfireModule.ID, "nekararpg.campfire.use")) {
            long remaining = campfire.restedSecondsRemaining(player.getUniqueId());
            inventory.setItem(CAMPFIRE_SLOT, item(Material.CAMPFIRE,
                    Component.text("Táboření", NamedTextColor.GOLD),
                    Component.text(remaining > 0
                                    ? "Odpočatý ještě " + formatDuration(remaining)
                                    : "Odpočívej vsedě u zapáleného ohně",
                            remaining > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY)));
            visibleModules++;
        }
        if (canShow(player, MiningModule.ID, "nekararpg.echo-vein.use")) {
            inventory.setItem(MINING_SLOT, item(Material.IRON_PICKAXE,
                    Component.text("Těžba", NamedTextColor.GRAY),
                    Component.text("Echo Vein odhaluje skryté žíly", NamedTextColor.WHITE),
                    Component.text("Spouští se při těžbě s ValhallaMMO", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (canShow(player, MountsModule.ID, "nekararpg.mount.use")) {
            inventory.setItem(MOUNTS_SLOT, item(Material.SADDLE,
                    Component.text("Můj kůň", NamedTextColor.GOLD),
                    Component.text("Založení, přivolání, výbava a píšťalka", NamedTextColor.GRAY),
                    Component.text("Kliknutím otevřeš NekaraMounts", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (visibleModules == 0) {
            inventory.setItem(13, item(Material.BARRIER,
                    Component.text("Žádné dostupné moduly", NamedTextColor.RED),
                    Component.text("Aktivní moduly nemáš povolené.", NamedTextColor.GRAY)));
        }
        player.openInventory(inventory);
    }

    public void shutdown() {
        HandlerList.unregisterAll(this);
        closeOpenMenus();
    }

    public void closeOpenMenus() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof MainMenuHolder) {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !(event.getView().getTopInventory().getHolder() instanceof MainMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!holder.ownerId.equals(player.getUniqueId()) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        player.closeInventory();
        Bukkit.getScheduler().runTask(plugin, () -> handle(player, slot));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MainMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handle(Player player, int slot) {
        if (!player.isOnline()) {
            return;
        }
        switch (slot) {
            case AUTH_SLOT -> {
                if (canShow(player, AuthModule.ID, null)) {
                    auth.openMenu(player);
                }
            }
            case FISHING_SLOT -> {
                if (canShow(player, FishingModule.ID, "nekararpg.use")) {
                    messages.send(player, "menu-fishing-info");
                }
            }
            case SITTING_SLOT -> toggleSitting(player);
            case CAMPFIRE_SLOT -> {
                if (canShow(player, CampfireModule.ID, "nekararpg.campfire.use")) {
                    long remaining = campfire.restedSecondsRemaining(player.getUniqueId());
                    messages.send(player, remaining > 0 ? "menu-campfire-rested" : "menu-campfire-info",
                            Map.of("time", formatDuration(remaining)));
                }
            }
            case MINING_SLOT -> {
                if (canShow(player, MiningModule.ID, "nekararpg.echo-vein.use")) {
                    messages.send(player, "menu-mining-info");
                }
            }
            case MOUNTS_SLOT -> {
                if (canShow(player, MountsModule.ID, "nekararpg.mount.use")) {
                    mounts.openMenu(player);
                }
            }
            default -> {
            }
        }
    }

    private void toggleSitting(Player player) {
        if (!canShow(player, SittingModule.ID, "nekararpg.sitting.use")) {
            return;
        }
        if (sitting.isSeated(player)) {
            messages.send(player, sitting.stand(player) ? "sitting-stopped" : "sitting-not-seated");
            return;
        }
        SitResult result = sitting.sit(player);
        messages.send(player, switch (result) {
            case SUCCESS -> "sitting-started";
            case ALREADY_SITTING -> "sitting-already";
            case ALREADY_RIDING -> "sitting-riding";
            case NOT_ON_GROUND -> "sitting-ground-required";
            case INVALID_STATE -> "sitting-invalid-state";
            case MODULE_DISABLED -> "sitting-disabled";
            case FAILED -> "sitting-failed";
        });
    }

    private boolean canShow(Player player, String moduleId, String permission) {
        return modules.isEnabled(moduleId) && (permission == null || player.hasPermission(permission));
    }

    private String formatDuration(long seconds) {
        long safe = Math.max(0L, seconds);
        return safe >= 60L ? (safe / 60L) + "m " + (safe % 60L) + "s" : safe + "s";
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack item(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        if (lore.length > 0) {
            meta.lore(List.of(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    private static final class MainMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private MainMenuHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
