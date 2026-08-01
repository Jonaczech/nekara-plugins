package cz.nekara.rpg;

import cz.nekara.rpg.command.NekaraRPGCommand;
import cz.nekara.rpg.configuration.ConfigurationService;
import cz.nekara.rpg.configuration.PluginConfig;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.ModuleRegistry;
import cz.nekara.rpg.modules.campfire.CampfireModule;
import cz.nekara.rpg.modules.fishing.FishingModule;
import cz.nekara.rpg.modules.sitting.SittingModule;
import cz.nekara.rpg.sounds.SoundService;
import org.bukkit.plugin.java.JavaPlugin;

public final class NekaraRPGPlugin extends JavaPlugin {
    private ConfigurationService configuration;
    private MessageService messages;
    private SoundService sounds;
    private ModuleRegistry modules;
    private FishingModule fishingModule;
    private SittingModule sittingModule;
    private CampfireModule campfireModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configuration = new ConfigurationService(this);
        messages = new MessageService(this);
        messages.reload();
        sounds = new SoundService(this);

        modules = new ModuleRegistry();
        fishingModule = new FishingModule(this, messages, sounds);
        sittingModule = new SittingModule(this, messages);
        campfireModule = new CampfireModule(this, messages, sounds, sittingModule);
        modules.register(fishingModule);
        modules.register(sittingModule);
        modules.register(campfireModule);
        fishingModule.registerCommand(new NekaraRPGCommand(
                this, fishingModule, sittingModule, campfireModule, modules, messages));

        reloadPlugin();
        getLogger().info("NekaraRPG " + getDescription().getVersion()
                + " enabled with modules: " + String.join(", ", modules.enabledModuleIds()) + ".");
    }

    public void reloadPlugin() {
        PluginConfig config = configuration.reload();
        messages.reload();
        sounds.reload(config.sounds());
        modules.applyConfig(config);
    }

    @Override
    public void onDisable() {
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

    public SittingModule sittingModule() {
        return sittingModule;
    }

    public CampfireModule campfireModule() {
        return campfireModule;
    }
}
