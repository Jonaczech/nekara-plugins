package cz.nekara.fishing;

import cz.nekara.fishing.command.NekaraFishingCommand;
import cz.nekara.fishing.compatibility.ValhallaExperienceBridge;
import cz.nekara.fishing.configuration.ConfigurationService;
import cz.nekara.fishing.configuration.PluginConfig;
import cz.nekara.fishing.fishing.FishingListener;
import cz.nekara.fishing.fishing.MinigameInputListener;
import cz.nekara.fishing.messages.MessageService;
import cz.nekara.fishing.minigame.FishingMinigameManager;
import cz.nekara.fishing.sounds.SoundService;
import org.bukkit.plugin.java.JavaPlugin;

public final class NekaraFishingPlugin extends JavaPlugin {
    private ConfigurationService configuration;
    private MessageService messages;
    private SoundService sounds;
    private FishingMinigameManager minigames;
    private ValhallaExperienceBridge valhallaExperienceBridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configuration = new ConfigurationService(this);
        messages = new MessageService(this);
        messages.reload();
        sounds = new SoundService(this);
        minigames = new FishingMinigameManager(this, messages, sounds);
        valhallaExperienceBridge = new ValhallaExperienceBridge(this, minigames);
        minigames.setValhallaExperienceBridge(valhallaExperienceBridge);
        reloadPlugin();

        getServer().getPluginManager().registerEvents(new FishingListener(this, minigames), this);
        getServer().getPluginManager().registerEvents(new MinigameInputListener(this, minigames), this);
        valhallaExperienceBridge.register();

        NekaraFishingCommand command = new NekaraFishingCommand(this, minigames, messages);
        if (getCommand("nekarafishing") != null) {
            getCommand("nekarafishing").setExecutor(command);
            getCommand("nekarafishing").setTabCompleter(command);
        }
        minigames.startTicker();
        getLogger().info("NekaraFishing " + getDescription().getVersion()
                + " enabled in " + minigames.modeName() + " mode.");
    }

    public void reloadPlugin() {
        PluginConfig config = configuration.reload();
        messages.reload();
        sounds.reload(config.sounds());
        minigames.cancelAll(false);
        minigames.applyConfig(config);
    }

    @Override
    public void onDisable() {
        if (minigames != null) {
            minigames.shutdown();
        }
        getLogger().info("NekaraFishing disabled; active fishing sessions cleaned up.");
    }

    public ConfigurationService configuration() {
        return configuration;
    }
}
