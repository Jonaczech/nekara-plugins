package cz.nekara.rpg.modules.auth;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.auth.AccountRepository;
import cz.nekara.rpg.auth.AuthAccount;
import cz.nekara.rpg.auth.LoginThrottle;
import cz.nekara.rpg.auth.PasswordHasher;
import cz.nekara.rpg.auth.YamlAccountRepository;
import cz.nekara.rpg.configuration.AuthConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.NekaraModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class AuthModule implements NekaraModule, Listener {
    public static final String ID = "auth";
    private static final int ACTION_SLOT = 13;
    private static final int LOGOUT_SLOT = 15;

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final Set<UUID> authenticated = new HashSet<>();
    private final Set<UUID> processing = new HashSet<>();
    private final Map<UUID, Prompt> prompts = new HashMap<>();
    private final Map<UUID, BukkitTask> timeoutTasks = new HashMap<>();

    private AccountRepository accounts;
    private PasswordHasher passwordHasher;
    private LoginThrottle throttle;
    private ExecutorService hashExecutor;
    private String storageFailure;
    private boolean enabled;

    public AuthModule(NekaraRPGPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
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
        AuthConfig config = config();
        passwordHasher = new PasswordHasher(config.passwordIterations());
        throttle = new LoginThrottle(config.maximumAttempts(), Duration.ofSeconds(config.lockoutSeconds()));
        hashExecutor = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(32), new AuthThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        storageFailure = null;
        try {
            accounts = new YamlAccountRepository(
                    new File(plugin.getDataFolder(), config.storageFile()));
        } catch (IOException | RuntimeException exception) {
            accounts = null;
            storageFailure = exception.getMessage();
            plugin.getLogger().severe("NekaraAuth storage is unavailable; logins will be rejected: "
                    + exception.getMessage());
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        enabled = true;
        if (Bukkit.getOnlineMode()) {
            plugin.getLogger().warning("NekaraAuth is enabled while online-mode=true. Players will authenticate twice.");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (storageFailure == null) {
                requireAuthentication(player);
            } else {
                player.kick(Component.text("Přihlašování NekaraAuth není dostupné. Kontaktuj administrátora."));
            }
        }
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof AuthMenuHolder
                    || player.getOpenInventory() instanceof AnvilView
                    && prompts.containsKey(player.getUniqueId())) {
                if (player.getOpenInventory() instanceof AnvilView) {
                    player.getOpenInventory().getTopInventory().clear();
                }
                player.closeInventory();
            }
        }
        HandlerList.unregisterAll(this);
        for (BukkitTask task : timeoutTasks.values()) {
            task.cancel();
        }
        timeoutTasks.clear();
        prompts.clear();
        processing.clear();
        authenticated.clear();
        if (hashExecutor != null) {
            hashExecutor.shutdownNow();
            hashExecutor = null;
        }
        accounts = null;
        storageFailure = null;
    }

    @Override
    public void reload() {
        AuthConfig current = config();
        passwordHasher = new PasswordHasher(current.passwordIterations());
        throttle = new LoginThrottle(current.maximumAttempts(), Duration.ofSeconds(current.lockoutSeconds()));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAuthenticated(Player player) {
        return authenticated.contains(player.getUniqueId());
    }

    public int authenticatedCount() {
        return authenticated.size();
    }

    public int accountCount() {
        return accounts == null ? 0 : accounts.count();
    }

    public void openMenu(Player player) {
        if (!enabled) {
            messages.send(player, "auth-disabled");
            return;
        }
        boolean loggedIn = isAuthenticated(player);
        boolean registered = findAccount(player.getName()).isPresent();
        AuthMenuHolder holder = new AuthMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27,
                Component.text("NekaraAuth", NamedTextColor.DARK_AQUA));
        holder.inventory = inventory;

        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "));
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        if (loggedIn) {
            inventory.setItem(ACTION_SLOT, item(Material.LIME_DYE,
                    Component.text("Účet je přihlášený", NamedTextColor.GREEN),
                    Component.text(player.getName(), NamedTextColor.GRAY)));
            inventory.setItem(LOGOUT_SLOT, item(Material.BARRIER,
                    Component.text("Odhlásit se", NamedTextColor.RED),
                    Component.text("Po odhlášení bude pohyb znovu uzamčen.", NamedTextColor.GRAY)));
        } else if (registered) {
            inventory.setItem(ACTION_SLOT, item(Material.TRIPWIRE_HOOK,
                    Component.text("Přihlásit se", NamedTextColor.AQUA),
                    Component.text("Klikni a zadej heslo v kovadlině.", NamedTextColor.GRAY)));
        } else {
            inventory.setItem(ACTION_SLOT, item(Material.WRITABLE_BOOK,
                    Component.text("Vytvořit účet", NamedTextColor.GREEN),
                    Component.text("Klikni a zvol heslo v kovadlině.", NamedTextColor.GRAY),
                    Component.text("Registrací si chráníš tento nick.", NamedTextColor.DARK_GRAY)));
        }
        player.openInventory(inventory);
    }

    public void startGuiAuthentication(Player player) {
        if (!enabled || isAuthenticated(player) || processing.contains(player.getUniqueId())) {
            return;
        }
        Optional<AuthAccount> account = findAccount(player.getName());
        PromptStep step = account.isPresent() ? PromptStep.LOGIN : PromptStep.REGISTER_FIRST;
        openPasswordPrompt(player, new Prompt(step, null));
    }

    public void login(Player player, char[] password) {
        if (!canStart(player)) {
            clear(password);
            return;
        }
        Optional<AuthAccount> account = findAccount(player.getName());
        if (account.isEmpty()) {
            clear(password);
            messages.send(player, "auth-not-registered");
            reopenMenu(player);
            return;
        }
        Duration lockout = throttle.remainingLockout(player.getName(), Instant.now());
        if (!lockout.isZero()) {
            clear(password);
            messages.send(player, "auth-locked", Map.of("seconds", roundedSeconds(lockout)));
            reopenMenu(player);
            return;
        }

        setProcessing(player, true);
        AccountRepository repository = accounts;
        runHashTask(player, password, () -> {
            boolean matches = passwordHasher.verify(password, account.get().passwordHash());
            if (matches) {
                AuthAccount updated = account.get().withSuccessfulLogin(player.getUniqueId(), Instant.now());
                if (passwordHasher.needsRehash(account.get().passwordHash())) {
                    updated = updated.withPasswordHash(passwordHasher.hash(password));
                }
                repository.update(updated);
            }
            return matches;
        }, matches -> {
            if (!matches) {
                LoginThrottle.Failure failure = throttle.registerFailure(player.getName(), Instant.now());
                if (!player.isOnline()) {
                    return;
                }
                if (failure.locked()) {
                    messages.send(player, "auth-locked", Map.of("seconds", roundedSeconds(failure.lockout())));
                    player.kick(messages.component("auth-too-many-attempts", Map.of()));
                } else {
                    messages.send(player, "auth-login-failed",
                            Map.of("remaining", failure.remainingAttempts()));
                    reopenMenu(player);
                }
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            throttle.registerSuccess(player.getName());
            authenticate(player, "auth-login-success");
        });
    }

    public void register(Player player, char[] password, char[] confirmation) {
        if (!passwordsEqual(password, confirmation)) {
            clear(password);
            clear(confirmation);
            messages.send(player, "auth-password-mismatch");
            reopenMenu(player);
            return;
        }
        clear(confirmation);
        registerValidated(player, password);
    }

    public void logout(Player player) {
        if (!enabled || !authenticated.remove(player.getUniqueId())) {
            messages.send(player, "auth-not-logged-in");
            return;
        }
        requireAuthentication(player);
        messages.send(player, "auth-logout-success");
    }

    public boolean unregister(String username) throws IOException {
        if (accounts == null) {
            throw new IOException(storageFailure == null ? "Storage is unavailable." : storageFailure);
        }
        boolean removed = accounts.delete(username);
        if (removed) {
            Player online = Bukkit.getPlayerExact(username);
            if (online != null) {
                authenticated.remove(online.getUniqueId());
                requireAuthentication(online);
            }
        }
        return removed;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (storageFailure != null || accounts == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    "Přihlašování NekaraAuth není dostupné. Kontaktuj administrátora.");
            return;
        }
        if (!config().exactNameCase()) {
            return;
        }
        accounts.findByUsername(event.getName()).ifPresent(account -> {
            if (!account.username().equals(event.getName())) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        "Tento nick je chráněný. Použij přesný tvar: " + account.username());
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        requireAuthentication(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!requiresAuthentication(event.getPlayer())) {
            return;
        }
        if (event.hasChangedPosition()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!requiresAuthentication(event.getPlayer())) {
            return;
        }
        String command = event.getMessage().trim().toLowerCase(java.util.Locale.ROOT);
        if (command.startsWith("/login ") || command.startsWith("/l ")
                || command.startsWith("/register ") || command.startsWith("/reg ")
                || command.equals("/nekaraauth") || command.equals("/nauth")) {
            return;
        }
        event.setCancelled(true);
        messages.send(event.getPlayer(), "auth-required");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onOpenInventory(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !requiresAuthentication(player)) {
            return;
        }
        if (event.getInventory().getHolder() instanceof AuthMenuHolder
                || event.getView() instanceof AnvilView && prompts.containsKey(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (event.getInventory().getHolder() instanceof AuthMenuHolder) {
            event.setCancelled(true);
            if (event.getRawSlot() == ACTION_SLOT && !isAuthenticated(player)) {
                startGuiAuthentication(player);
            } else if (event.getRawSlot() == LOGOUT_SLOT && isAuthenticated(player)) {
                player.closeInventory();
                logout(player);
            }
            return;
        }
        Prompt prompt = prompts.get(playerId);
        if (prompt != null && event.getView() instanceof AnvilView anvilView) {
            event.setCancelled(true);
            if (event.getRawSlot() == 2) {
                submitPrompt(player, prompt, anvilView);
            }
            return;
        }
        if (requiresAuthentication(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && requiresAuthentication(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        if (!prompts.containsKey(player.getUniqueId())) {
            return;
        }
        event.getView().setRepairCost(0);
        event.getView().setMaximumRepairCost(0);
        event.getView().setBypassCost(true);
        event.setResult(item(Material.LIME_DYE,
                Component.text("Potvrdit heslo", NamedTextColor.GREEN)));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || isAuthenticated(player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (event.getView() instanceof AnvilView) {
            event.getInventory().clear();
            prompts.remove(playerId);
        }
        if (enabled && !processing.contains(playerId)) {
            reopenMenu(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && requiresAuthentication(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = attackingPlayer(event.getDamager());
        if (attacker != null && requiresAuthentication(attacker)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onFoodLevel(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && requiresAuthentication(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && requiresAuthentication(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (requiresAuthentication(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private void registerValidated(Player player, char[] password) {
        if (!canStart(player)) {
            clear(password);
            return;
        }
        if (findAccount(player.getName()).isPresent()) {
            clear(password);
            messages.send(player, "auth-already-registered");
            reopenMenu(player);
            return;
        }
        String validationMessage = validatePassword(password);
        if (validationMessage != null) {
            clear(password);
            messages.send(player, validationMessage, Map.of(
                    "min", config().passwordMinimumLength(),
                    "max", config().passwordMaximumLength()));
            reopenMenu(player);
            return;
        }

        setProcessing(player, true);
        AccountRepository repository = accounts;
        String username = player.getName();
        UUID playerId = player.getUniqueId();
        Instant createdAt = Instant.now();
        runHashTask(player, password, () -> {
            String encoded = passwordHasher.hash(password);
            return repository.create(new AuthAccount(
                    username, AuthAccount.normalize(username), playerId,
                    encoded, createdAt, createdAt));
        }, created -> {
            if (!player.isOnline()) {
                return;
            }
            if (!created) {
                messages.send(player, "auth-already-registered");
                reopenMenu(player);
                return;
            }
            authenticate(player, "auth-register-success");
        });
    }

    private void submitPrompt(Player player, Prompt prompt, AnvilView anvilView) {
        if (processing.contains(player.getUniqueId())) {
            return;
        }
        String input = anvilView.getRenameText();
        char[] password = input == null ? new char[0] : input.toCharArray();
        prompts.remove(player.getUniqueId());
        processing.add(player.getUniqueId());
        anvilView.getTopInventory().clear();
        player.closeInventory();
        processing.remove(player.getUniqueId());

        if (prompt.step() == PromptStep.LOGIN) {
            login(player, password);
            return;
        }
        String validationMessage = validatePassword(password);
        if (validationMessage != null) {
            clear(password);
            messages.send(player, validationMessage, Map.of(
                    "min", config().passwordMinimumLength(),
                    "max", config().passwordMaximumLength()));
            reopenMenu(player);
            return;
        }
        if (prompt.step() == PromptStep.REGISTER_FIRST) {
            setProcessing(player, true);
            runHashTask(player, password, () -> passwordHasher.hash(password), encoded -> {
                if (!player.isOnline()) {
                    return;
                }
                messages.send(player, "auth-confirm-password");
                openPasswordPrompt(player, new Prompt(PromptStep.REGISTER_CONFIRM, encoded));
            });
            return;
        }

        setProcessing(player, true);
        AccountRepository repository = accounts;
        String username = player.getName();
        UUID playerId = player.getUniqueId();
        Instant createdAt = Instant.now();
        runHashTask(player, password,
                () -> {
                    if (!passwordHasher.verify(password, prompt.pendingHash())) {
                        return RegistrationResult.PASSWORD_MISMATCH;
                    }
                    boolean created = repository.create(new AuthAccount(
                            username, AuthAccount.normalize(username), playerId,
                            prompt.pendingHash(), createdAt, createdAt));
                    return created ? RegistrationResult.CREATED : RegistrationResult.ALREADY_EXISTS;
                }, result -> {
                    if (!player.isOnline()) {
                        return;
                    }
                    if (result == RegistrationResult.PASSWORD_MISMATCH) {
                        messages.send(player, "auth-password-mismatch");
                        reopenMenu(player);
                        return;
                    }
                    if (result == RegistrationResult.ALREADY_EXISTS) {
                        messages.send(player, "auth-already-registered");
                        reopenMenu(player);
                        return;
                    }
                    authenticate(player, "auth-register-success");
                });
    }

    private <T> void runHashTask(Player player, char[] password,
                                 java.util.concurrent.Callable<T> operation,
                                 java.util.function.Consumer<T> completion) {
        UUID playerId = player.getUniqueId();
        messages.send(player, "auth-processing");
        try {
            hashExecutor.submit(() -> {
                T result;
                try {
                    result = operation.call();
                } catch (Exception exception) {
                    plugin.getLogger().severe("NekaraAuth authentication operation failed: "
                            + exception.getMessage());
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        processing.remove(playerId);
                        if (player.isOnline()) {
                            messages.send(player, "auth-error");
                            reopenMenu(player);
                        }
                    });
                    return;
                } finally {
                    clear(password);
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    processing.remove(playerId);
                    if (enabled && requiresAuthentication(player)) {
                        completion.accept(result);
                    }
                });
            });
        } catch (RejectedExecutionException exception) {
            clear(password);
            processing.remove(playerId);
            messages.send(player, "auth-busy");
            reopenMenu(player);
        }
    }

    private void authenticate(Player player, String messageKey) {
        UUID playerId = player.getUniqueId();
        authenticated.add(playerId);
        processing.remove(playerId);
        prompts.remove(playerId);
        cancelTimeout(playerId);
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof AuthMenuHolder
                || player.getOpenInventory() instanceof AnvilView) {
            if (player.getOpenInventory() instanceof AnvilView) {
                player.getOpenInventory().getTopInventory().clear();
            }
            player.closeInventory();
        }
        messages.send(player, messageKey);
    }

    private void requireAuthentication(Player player) {
        UUID playerId = player.getUniqueId();
        authenticated.remove(playerId);
        processing.remove(playerId);
        prompts.remove(playerId);
        cancelTimeout(playerId);
        int timeoutSeconds = config().authenticationTimeoutSeconds();
        timeoutTasks.put(playerId, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && requiresAuthentication(player)) {
                player.kick(messages.component("auth-timeout", Map.of()));
            }
        }, timeoutSeconds * 20L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !requiresAuthentication(player)) {
                return;
            }
            messages.send(player, findAccount(player.getName()).isPresent()
                    ? "auth-login-required" : "auth-register-required");
            if (config().openMenuOnJoin()) {
                openMenu(player);
            }
        }, 2L);
    }

    private void openPasswordPrompt(Player player, Prompt prompt) {
        prompts.put(player.getUniqueId(), prompt);
        InventoryView opened = player.openAnvil(null, true);
        if (!(opened instanceof AnvilView anvilView)) {
            prompts.remove(player.getUniqueId());
            messages.send(player, "auth-error");
            reopenMenu(player);
            return;
        }
        AnvilInventory inventory = anvilView.getTopInventory();
        ItemStack input = item(Material.PAPER, Component.empty());
        inventory.setFirstItem(input);
        anvilView.setRepairCost(0);
        anvilView.setMaximumRepairCost(0);
        anvilView.setBypassCost(true);
    }

    private void reopenMenu(Player player) {
        if (!enabled || !player.isOnline() || isAuthenticated(player)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (enabled && player.isOnline() && requiresAuthentication(player)
                    && !processing.contains(player.getUniqueId())
                    && !prompts.containsKey(player.getUniqueId())) {
                openMenu(player);
            }
        }, 2L);
    }

    private boolean canStart(Player player) {
        if (!enabled || storageFailure != null || accounts == null) {
            messages.send(player, "auth-error");
            return false;
        }
        if (isAuthenticated(player)) {
            messages.send(player, "auth-already-logged-in");
            return false;
        }
        if (processing.contains(player.getUniqueId())) {
            messages.send(player, "auth-processing");
            return false;
        }
        return true;
    }

    private Optional<AuthAccount> findAccount(String username) {
        return accounts == null ? Optional.empty() : accounts.findByUsername(username);
    }

    private boolean requiresAuthentication(Player player) {
        return enabled && !authenticated.contains(player.getUniqueId());
    }

    private void setProcessing(Player player, boolean value) {
        if (value) {
            processing.add(player.getUniqueId());
        } else {
            processing.remove(player.getUniqueId());
        }
    }

    private String validatePassword(char[] password) {
        if (password.length < config().passwordMinimumLength()) {
            return "auth-password-too-short";
        }
        if (password.length > config().passwordMaximumLength()) {
            return "auth-password-too-long";
        }
        return null;
    }

    private void cleanupPlayer(UUID playerId) {
        authenticated.remove(playerId);
        processing.remove(playerId);
        prompts.remove(playerId);
        cancelTimeout(playerId);
    }

    private void cancelTimeout(UUID playerId) {
        BukkitTask task = timeoutTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private Player attackingPlayer(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player player) {
                return player;
            }
        }
        return null;
    }

    private AuthConfig config() {
        return plugin.configuration().get().auth();
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

    private long roundedSeconds(Duration duration) {
        return Math.max(1L, (duration.toMillis() + 999L) / 1_000L);
    }

    private boolean passwordsEqual(char[] first, char[] second) {
        int difference = first.length ^ second.length;
        int maximum = Math.max(first.length, second.length);
        for (int index = 0; index < maximum; index++) {
            char left = index < first.length ? first[index] : 0;
            char right = index < second.length ? second[index] : 0;
            difference |= left ^ right;
        }
        return difference == 0;
    }

    private void clear(char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }

    private enum PromptStep {
        LOGIN,
        REGISTER_FIRST,
        REGISTER_CONFIRM
    }

    private enum RegistrationResult {
        CREATED,
        ALREADY_EXISTS,
        PASSWORD_MISMATCH
    }

    private record Prompt(PromptStep step, String pendingHash) {
    }

    private static final class AuthMenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class AuthThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "NekaraAuth-Hash-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
