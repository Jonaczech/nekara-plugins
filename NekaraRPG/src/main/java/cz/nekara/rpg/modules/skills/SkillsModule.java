package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.SkillsConfig;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.PerkDefinition;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.perks.PerkPurchasePolicy;
import cz.nekara.rpg.skills.perks.PerkPurchaseService;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillStorageException;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SkillsModule implements NekaraModule {
    public static final String ID = "skills";

    private final NekaraRPGPlugin plugin;
    private final DefaultPerkTree perkTree;
    private final SkillsMenu menu;
    private final Set<UUID> pendingPurchases = ConcurrentHashMap.newKeySet();
    private volatile SkillProfileRepository repository;
    private volatile SkillProgressResolver progressResolver;
    private volatile PerkPurchaseService purchaseService;
    private volatile String storageFailure;
    private volatile long generation;
    private boolean enabled;

    public SkillsModule(NekaraRPGPlugin plugin) {
        this.plugin = plugin;
        this.perkTree = DefaultPerkTree.create();
        this.menu = new SkillsMenu(plugin, this, perkTree);
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
        generation++;
        SkillsConfig config = plugin.configuration().get().skills();
        progressResolver = new SkillProgressResolver(new SkillProgressionCurve(
            SkillProgressionCurve.DEFAULT_MAX_LEVEL,
            config.baseExperience(),
            config.linearGrowth(),
            config.quadraticGrowth()
        ));
        storageFailure = null;
        try {
            repository = new SqliteSkillProfileRepository(
                new File(plugin.getDataFolder(), config.databaseFile()));
            purchaseService = new PerkPurchaseService(
                repository,
                progressResolver,
                perkTree.catalog(),
                new PerkPurchasePolicy(),
                3
            );
        } catch (IOException | RuntimeException exception) {
            repository = null;
            purchaseService = null;
            storageFailure = exception.getMessage();
            plugin.getLogger().severe("Nekara Skills storage is unavailable; progression is locked: "
                + exception.getMessage());
        }
        menu.enable();
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        enabled = false;
        generation++;
        menu.disable();
        SkillProfileRepository closing = repository;
        repository = null;
        progressResolver = null;
        purchaseService = null;
        pendingPurchases.clear();
        if (closing != null) {
            try {
                closing.close();
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("Could not close Nekara Skills storage: " + exception.getMessage());
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void openMenu(Player player) {
        loadProfile(player, (profile, snapshot) -> menu.openOverview(player, profile, snapshot));
    }

    void openSkillTree(Player player, SkillId skill) {
        loadProfile(player, (profile, snapshot) -> menu.openTree(player, profile, snapshot, skill));
    }

    void openPerkConfirmation(Player player, PerkId perkId) {
        PerkDefinition perk = perkTree.catalog().require(perkId);
        loadProfile(player, (profile, snapshot) ->
            menu.openConfirmation(player, profile, snapshot, perk));
    }

    void purchasePerk(Player player, PerkId perkId) {
        PerkPurchaseService activeService = purchaseService;
        if (!enabled || activeService == null || !pendingPurchases.add(player.getUniqueId())) {
            if (activeService == null) {
                menu.showUnavailable(player);
            } else {
                player.sendActionBar(Component.text(
                    "Kronika ještě zapisuje předchozí volbu.",
                    NamedTextColor.YELLOW));
            }
            return;
        }
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var result = activeService.purchase(player.getUniqueId().toString(), perkId);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pendingPurchases.remove(player.getUniqueId());
                    if (enabled && generation == requestGeneration && player.isOnline()) {
                        menu.openTree(
                            player,
                            result.profile(),
                            result.progress(),
                            perkTree.catalog().require(perkId).skill()
                        );
                        menu.showPurchaseStatus(player, result.status());
                    }
                });
            } catch (SkillStorageException | IllegalStateException exception) {
                pendingPurchases.remove(player.getUniqueId());
                handleStorageFailure(player, requestGeneration, exception);
            }
        });
    }

    private void loadProfile(
        Player player,
        BiConsumer<SkillProfile, SkillProgressSnapshot> callback
    ) {
        if (!enabled || repository == null || progressResolver == null) {
            menu.showUnavailable(player);
            return;
        }
        String playerKey = player.getUniqueId().toString();
        SkillProfileRepository activeRepository = repository;
        SkillProgressResolver activeResolver = progressResolver;
        long requestGeneration = generation;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                SkillProfile profile = activeRepository.find(playerKey)
                    .orElseGet(() -> SkillProfile.empty(playerKey));
                var snapshot = activeResolver.resolve(profile);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (enabled && generation == requestGeneration && player.isOnline()) {
                        callback.accept(profile, snapshot);
                    }
                });
            } catch (SkillStorageException | IllegalStateException exception) {
                handleStorageFailure(player, requestGeneration, exception);
            }
        });
    }

    private void handleStorageFailure(Player player, long requestGeneration, RuntimeException exception) {
        if (generation == requestGeneration) {
            storageFailure = exception.getMessage();
            plugin.getLogger().severe(
                "Could not access Nekara Skills profile: " + exception.getMessage());
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (enabled && generation == requestGeneration && player.isOnline()) {
                menu.showUnavailable(player);
            }
        });
    }

    public String storageStatus() {
        if (!enabled) {
            return "vypnuto";
        }
        return repository == null ? "uzamčeno" : "připraveno";
    }

    public String storageFailure() {
        return storageFailure;
    }
}
