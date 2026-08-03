package cz.nekara.rpg;

import cz.nekara.rpg.command.NekaraRPGCommand;
import cz.nekara.rpg.command.AuthCommand;
import cz.nekara.rpg.configuration.ConfigurationService;
import cz.nekara.rpg.configuration.PluginConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.menu.NekaraRPGMenu;
import cz.nekara.rpg.modules.ModuleRegistry;
import cz.nekara.rpg.modules.auth.AuthModule;
import cz.nekara.rpg.modules.campfire.CampfireModule;
import cz.nekara.rpg.modules.fishing.FishingModule;
import cz.nekara.rpg.modules.mining.MiningModule;
import cz.nekara.rpg.modules.mounts.MountsModule;
import cz.nekara.rpg.modules.sitting.SittingModule;
import cz.nekara.rpg.sounds.SoundService;
import cz.nekara.rpg.updater.UpdaterService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class NekaraRPGPlugin extends JavaPlugin {
    private ConfigurationService configuration;
    private MessageService messages;
    private SoundService sounds;
    private UpdaterService updater;
    private ModuleRegistry modules;
    private AuthModule authModule;
    private FishingModule fishingModule;
    private SittingModule sittingModule;
    private CampfireModule campfireModule;
    private MiningModule miningModule;
    private MountsModule mountsModule;
    private NekaraRPGMenu mainMenu;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configuration = new ConfigurationService(this);
        messages = new MessageService(this);
        messages.reload();
        sounds = new SoundService(this);
        updater = new UpdaterService(this, messages);
        getServer().getPluginManager().registerEvents(updater, this);

        modules = new ModuleRegistry();
        authModule = new AuthModule(this, messages);
        fishingModule = new FishingModule(this, messages, sounds);
        sittingModule = new SittingModule(this, messages);
        campfireModule = new CampfireModule(this, messages, sounds, sittingModule);
        miningModule = new MiningModule(this, messages, sounds, fishingModule);
        mountsModule = new MountsModule(this, messages);
        modules.register(authModule);
        modules.register(fishingModule);
        modules.register(campfireModule);
        modules.register(miningModule);
        modules.register(mountsModule);
        mainMenu = new NekaraRPGMenu(this, messages, modules, authModule, fishingModule,
                sittingModule, campfireModule, mountsModule);
        fishingModule.registerCommand(new NekaraRPGCommand(
                this, fishingModule, sittingModule, campfireModule, miningModule, mountsModule,
                modules, messages, updater, mainMenu));
        AuthCommand authCommand = new AuthCommand(authModule, messages);
        for (String commandName : new String[]{"nekaraauth", "login", "register", "logout"}) {
            var command = Objects.requireNonNull(getCommand(commandName),
                    "Missing command declaration: " + commandName);
            command.setExecutor(authCommand);
            command.setTabCompleter(authCommand);
        }

        reloadPlugin();
        getLogger().info("NekaraRPG " + getDescription().getVersion()
                + " enabled with modules: " + String.join(", ", modules.enabledModuleIds()) + ".");
    }

    public void reloadPlugin() {
        PluginConfig config = configuration.reload();
        messages.reload();
        sounds.reload(config.sounds());
        modules.applyConfig(config);
        if (mainMenu != null) {
            mainMenu.closeOpenMenus();
        }
        updater.reload(config.updater());
    }

    @Override
    public void onDisable() {
        if (mainMenu != null) {
            mainMenu.shutdown();
        }
        if (updater != null) {
            updater.shutdown();
        }
        if (modules != null) {
            modules.disableAll();
        }
        getLogger().info("NekaraRPG disabled; active modules cleaned up.");
    }

    public ConfigurationService configuration() {
        return configuration;
    }

    public FishingModule fishingModule() {
        return fishingModule;
    }

    public AuthModule authModule() {
        return authModule;
    }

    public SittingModule sittingModule() {
        return sittingModule;
    }

    public CampfireModule campfireModule() {
        return campfireModule;
    }

    public MiningModule miningModule() {
        return miningModule;
    }

    public MountsModule mountsModule() {
        return mountsModule;
    }

    public UpdaterService updater() {
        return updater;
    }

    public void openMainMenu(org.bukkit.entity.Player player) {
        if (mainMenu != null) {
            mainMenu.open(player);
        }
    }
}
