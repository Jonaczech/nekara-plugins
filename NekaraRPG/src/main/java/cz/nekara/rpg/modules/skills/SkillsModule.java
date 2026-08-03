package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.SkillsConfig;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillStorageException;
import java.io.File;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SkillsModule implements NekaraModule {
    public static final String ID = "skills";

    private final NekaraRPGPlugin plugin;
    private final SkillsMenu menu;
    private volatile SkillProfileRepository repository;
    private volatile SkillProgressResolver progressResolver;
    private volatile String storageFailure;
    private volatile long generation;
    private boolean enabled;

    public SkillsModule(NekaraRPGPlugin plugin) {
        this.plugin = plugin;
        this.menu = new SkillsMenu(plugin);
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
        } catch (IOException | RuntimeException exception) {
            repository = null;
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
                        menu.open(player, snapshot, profile.spentPerkPoints());
                    }
                });
            } catch (SkillStorageException | IllegalStateException exception) {
                if (generation == requestGeneration) {
                    storageFailure = exception.getMessage();
                    plugin.getLogger().severe(
                        "Could not read Nekara Skills profile: " + exception.getMessage());
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (enabled && generation == requestGeneration && player.isOnline()) {
                        menu.showUnavailable(player);
                    }
                });
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
