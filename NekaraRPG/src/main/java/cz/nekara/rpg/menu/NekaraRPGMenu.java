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
    private static final int SKILLS_SLOT = 22;
    private static final int OVERVIEW_SLOT = 4;
    private static final int DIAGNOSTICS_SLOT = 26;
    private static final int BACK_SLOT = 18;

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

        MainMenuHolder holder = new MainMenuHolder(player.getUniqueId(), Screen.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("NekaraRPG — hlavní menu", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(OVERVIEW_SLOT, item(Material.COMPASS,
                Component.text("Můj přehled", NamedTextColor.AQUA),
                Component.text("To nejdůležitější na jednom místě", NamedTextColor.GRAY),
                Component.text("Odpočinek, činnost a tvůj kůň", NamedTextColor.DARK_GRAY)));

        int visibleModules = 0;
        if (canShow(player, AuthModule.ID, null)) {
            inventory.setItem(AUTH_SLOT, item(Material.PLAYER_HEAD,
                    Component.text("Účet", NamedTextColor.AQUA),
                    Component.text("Přihlášen jako " + player.getName(), NamedTextColor.GRAY),
                    Component.text("Tvé jméno, heslo a bezpečný návrat", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (canShow(player, FishingModule.ID, "nekararpg.use")) {
            boolean active = fishing.minigames().isMinigameActive(player.getUniqueId());
            inventory.setItem(FISHING_SLOT, item(Material.FISHING_ROD,
                    Component.text("Rybaření", NamedTextColor.BLUE),
                    Component.text(active ? "Vlasec je napjatý" : "Voda tiše čeká",
                            active ? NamedTextColor.YELLOW : NamedTextColor.GRAY),
                    Component.text("Záběr odhalí správný okamžik", NamedTextColor.DARK_GRAY)));
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
                    Component.text("Kámen někdy prozradí skrytou žílu", NamedTextColor.WHITE),
                    Component.text("Při těžbě naslouchej jeho ozvěně", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (canShow(player, MountsModule.ID, "nekararpg.mount.use")) {
            inventory.setItem(MOUNTS_SLOT, item(Material.SADDLE,
                    Component.text("Můj kůň", NamedTextColor.GOLD),
                    Component.text("Pouto, výbava, brašny a píšťalka", NamedTextColor.GRAY),
                    Component.text("Postarej se o svého společníka", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("ValhallaMMO")) {
            inventory.setItem(SKILLS_SLOT, item(Material.EXPERIENCE_BOTTLE,
                    Component.text("Dovednosti", NamedTextColor.LIGHT_PURPLE),
                    Component.text("Otevře přehled tvých schopností", NamedTextColor.GRAY),
                    Component.text("Tady uvidíš cestu svého rozvoje", NamedTextColor.DARK_GRAY)));
            visibleModules++;
        }
        if (visibleModules == 0) {
            inventory.setItem(13, item(Material.BARRIER,
                    Component.text("Žádné dostupné moduly", NamedTextColor.RED),
                    Component.text("Aktivní moduly nemáš povolené.", NamedTextColor.GRAY)));
        }
        if (player.hasPermission("nekararpg.command.status")) {
            inventory.setItem(DIAGNOSTICS_SLOT, item(Material.COMPARATOR,
                    Component.text("Správa NekaraRPG", NamedTextColor.RED),
                    Component.text("Stav modulů, úložiště a updateru", NamedTextColor.GRAY)));
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
        MainMenuHolder holder = player.getOpenInventory().getTopInventory().getHolder() instanceof MainMenuHolder open
                ? open : null;
        if (holder == null) {
            return;
        }
        if (holder.screen == Screen.OVERVIEW) {
            if (slot == BACK_SLOT) open(player);
            else if (slot == DIAGNOSTICS_SLOT) openOverview(player);
            return;
        }
        if (holder.screen == Screen.DIAGNOSTICS) {
            if (slot == BACK_SLOT) open(player);
            else if (slot == DIAGNOSTICS_SLOT) openDiagnostics(player);
            return;
        }
        switch (slot) {
            case OVERVIEW_SLOT -> openOverview(player);
            case AUTH_SLOT -> {
                if (canShow(player, AuthModule.ID, null)) {
                    auth.openMenu(player);
                }
            }
            case FISHING_SLOT -> {
                // The tile lore contains the complete player-facing explanation.
            }
            case SITTING_SLOT -> toggleSitting(player);
            case CAMPFIRE_SLOT -> {
                // Status and instructions are intentionally kept in the tile lore.
            }
            case MINING_SLOT -> {
                // Status and instructions are intentionally kept in the tile lore.
            }
            case MOUNTS_SLOT -> {
                if (canShow(player, MountsModule.ID, "nekararpg.mount.use")) {
                    mounts.openMenu(player);
                }
            }
            case SKILLS_SLOT -> {
                if (Bukkit.getPluginManager().isPluginEnabled("ValhallaMMO")) {
                    player.closeInventory();
                    player.performCommand("skills");
                }
            }
            case DIAGNOSTICS_SLOT -> {
                if (player.hasPermission("nekararpg.command.status")) {
                    openDiagnostics(player);
                }
            }
            default -> {
            }
        }
    }

    private void openOverview(Player player) {
        MainMenuHolder holder = new MainMenuHolder(player.getUniqueId(), Screen.OVERVIEW);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("NekaraRPG — můj přehled", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(4, item(Material.PLAYER_HEAD,
                Component.text(player.getName(), NamedTextColor.AQUA),
                Component.text(auth.isEnabled() && auth.isAuthenticated(player)
                        ? "Totožnost je ověřená" : "Účet není ověřený", NamedTextColor.GRAY)));

        long rested = campfire.restedSecondsRemaining(player.getUniqueId());
        inventory.setItem(11, item(Material.CAMPFIRE,
                Component.text("Odpočinek", NamedTextColor.GOLD),
                Component.text(rested > 0 ? "Zbývá " + formatDuration(rested) : "Síly čekají na doplnění",
                        rested > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY)));

        boolean fishingActive = fishing.minigames().isMinigameActive(player.getUniqueId());
        boolean miningActive = plugin.miningModule().isActive(player.getUniqueId());
        inventory.setItem(13, item(fishingActive ? Material.FISHING_ROD
                        : miningActive ? Material.IRON_PICKAXE : Material.CLOCK,
                Component.text("Právě teď", NamedTextColor.YELLOW),
                Component.text(fishingActive ? "Soustředíš se na úlovek"
                        : miningActive ? "Nasloucháš ozvěně kamene" : "Nic tě právě nezdržuje",
                        NamedTextColor.GRAY)));

        MountsModule.MountOverview mount = mounts.overview(player).orElse(null);
        inventory.setItem(15, mount == null
                ? item(Material.LEAD, Component.text("Kůň", NamedTextColor.GOLD),
                Component.text("Zatím nemáš stálého společníka", NamedTextColor.GRAY))
                : item(Material.SADDLE, Component.text(mount.name(), NamedTextColor.GOLD),
                Component.text(mount.state(), NamedTextColor.GRAY),
                Component.text("Zdraví " + oneDecimal(mount.health()) + "/" + oneDecimal(mount.maxHealth()),
                        NamedTextColor.DARK_GRAY),
                Component.text(mount.hasChest()
                        ? "Brašny " + mount.occupiedStorageSlots() + "/54"
                        : "Bez brašen", NamedTextColor.DARK_GRAY),
                Component.text(mount.cooldownSeconds() > 0
                        ? "Připraven za " + formatDuration(mount.cooldownSeconds()) : "Píšťalka je připravená",
                        mount.cooldownSeconds() > 0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN)));
        inventory.setItem(BACK_SLOT, GuiItems.back("Zpět do NekaraRPG"));
        inventory.setItem(DIAGNOSTICS_SLOT, item(Material.SUNFLOWER,
                Component.text("Obnovit", NamedTextColor.YELLOW)));
        player.openInventory(inventory);
    }

    private void openDiagnostics(Player player) {
        if (!player.hasPermission("nekararpg.command.status")) {
            open(player);
            return;
        }
        MainMenuHolder holder = new MainMenuHolder(player.getUniqueId(), Screen.DIAGNOSTICS);
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("NekaraRPG — správa", NamedTextColor.DARK_RED));
        holder.inventory = inventory;
        fill(inventory);
        inventory.setItem(4, item(Material.REDSTONE_TORCH,
                Component.text("NekaraRPG " + plugin.getDescription().getVersion(), NamedTextColor.RED),
                Component.text("Aktivní moduly: " + String.join(", ", modules.enabledModuleIds()),
                        NamedTextColor.GRAY)));
        inventory.setItem(10, item(Material.CHEST,
                Component.text("NekaraMounts", NamedTextColor.GOLD),
                Component.text("Úložiště: " + mounts.storageStatus(), NamedTextColor.GRAY),
                Component.text("Evidováno " + mounts.registeredCount() + " | ve světě " + mounts.activeCount(),
                        NamedTextColor.DARK_GRAY)));
        inventory.setItem(12, item(Material.EXPERIENCE_BOTTLE,
                Component.text("Napojení", NamedTextColor.LIGHT_PURPLE),
                Component.text("ValhallaMMO: " + enabledPlugin("ValhallaMMO"), NamedTextColor.GRAY),
                Component.text("MythicMobs: " + enabledPlugin("MythicMobs"), NamedTextColor.GRAY)));
        long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1_048_576L;
        long maxMemory = Runtime.getRuntime().maxMemory() / 1_048_576L;
        inventory.setItem(14, item(Material.REPEATER,
                Component.text("Server", NamedTextColor.AQUA),
                Component.text("Online hráči: " + Bukkit.getOnlinePlayers().size(), NamedTextColor.GRAY),
                Component.text("Paměť JVM: " + usedMemory + "/" + maxMemory + " MiB", NamedTextColor.DARK_GRAY)));
        var update = plugin.updater().snapshot();
        inventory.setItem(16, item(Material.CLOCK,
                Component.text("Updater", NamedTextColor.YELLOW),
                Component.text(updaterLabel(update.state()), NamedTextColor.GRAY),
                Component.text(update.latestVersion() == null
                        ? "Zatím bez vzdálené verze" : "Nejnovější " + update.latestVersion(),
                        NamedTextColor.DARK_GRAY)));
        inventory.setItem(BACK_SLOT, GuiItems.back("Zpět do NekaraRPG"));
        inventory.setItem(DIAGNOSTICS_SLOT, item(Material.SUNFLOWER,
                Component.text("Obnovit", NamedTextColor.YELLOW)));
        player.openInventory(inventory);
    }

    private String enabledPlugin(String name) {
        return Bukkit.getPluginManager().isPluginEnabled(name) ? "připraveno" : "nenačteno";
    }

    private String updaterLabel(cz.nekara.rpg.updater.UpdaterState state) {
        return switch (state) {
            case DISABLED -> "Vypnutý";
            case IDLE -> "Čeká na kontrolu";
            case CHECKING -> "Právě kontroluje";
            case CURRENT -> "Plugin je aktuální";
            case AVAILABLE -> "Je dostupná nová verze";
            case STAGED -> "Nová verze čeká na restart";
            case FAILED -> "Poslední kontrola selhala";
        };
    }

    private String oneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void toggleSitting(Player player) {
        if (!canShow(player, SittingModule.ID, "nekararpg.sitting.use")) {
            return;
        }
        if (sitting.isSeated(player)) {
            if (!sitting.stand(player)) {
                messages.sendActionBar(player, "sitting-not-seated", Map.of());
            }
            open(player);
            return;
        }
        SitResult result = sitting.sit(player);
        if (result != SitResult.SUCCESS) {
            messages.sendActionBar(player, switch (result) {
            case ALREADY_SITTING -> "sitting-already";
            case ALREADY_RIDING -> "sitting-riding";
            case NOT_ON_GROUND -> "sitting-ground-required";
            case INVALID_STATE -> "sitting-invalid-state";
            case MODULE_DISABLED -> "sitting-disabled";
            case FAILED -> "sitting-failed";
            case SUCCESS -> "sitting-started";
            }, Map.of());
        }
        open(player);
    }

    private boolean canShow(Player player, String moduleId, String permission) {
        return modules.isEnabled(moduleId) && (permission == null || player.hasPermission(permission));
    }

    private String formatDuration(long seconds) {
        long safe = Math.max(0L, seconds);
        return safe >= 60L ? (safe / 60L) + "m " + (safe % 60L) + "s" : safe + "s";
    }

    private void fill(Inventory inventory) {
        GuiItems.fill(inventory);
    }

    private ItemStack item(Material material, Component name, Component... lore) {
        return GuiItems.item(material, name, lore);
    }

    private enum Screen {
        MAIN,
        OVERVIEW,
        DIAGNOSTICS
    }

    private static final class MainMenuHolder implements InventoryHolder {
        private final UUID ownerId;
        private final Screen screen;
        private Inventory inventory;

        private MainMenuHolder(UUID ownerId, Screen screen) {
            this.ownerId = ownerId;
            this.screen = screen;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
