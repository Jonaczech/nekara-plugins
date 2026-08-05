package cz.nekara.rpg.modules.fishing;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.command.NekaraRPGCommand;
import cz.nekara.rpg.fishing.FishingListener;
import cz.nekara.rpg.fishing.MinigameInputListener;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.minigame.FishingMinigameManager;
import cz.nekara.rpg.modules.NekaraModule;
import cz.nekara.rpg.sounds.SoundService;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public final class FishingModule implements NekaraModule {
    public static final String ID = "fishing";

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final SoundService sounds;
    private final FishingMinigameManager minigames;
    private final List<Listener> listeners = new ArrayList<>();
    private boolean enabled;

    public FishingModule(NekaraRPGPlugin plugin, MessageService messages, SoundService sounds) {
        this.plugin = plugin;
        this.messages = messages;
        this.sounds = sounds;
        this.minigames = new FishingMinigameManager(plugin, messages, sounds);
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
        registerListener(new FishingListener(plugin, minigames));
        registerListener(new MinigameInputListener(plugin, minigames));
        minigames.applyConfig(plugin.configuration().get());
        minigames.startTicker();
        enabled = true;
    }

    @Override
    public void disable() {
        if (!enabled) {
            return;
        }
        minigames.shutdown();
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
        enabled = false;
    }

    @Override
    public void reload() {
        minigames.cancelAll(false);
        minigames.applyConfig(plugin.configuration().get());
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public FishingMinigameManager minigames() {
        return minigames;
    }

    public void registerCommand(NekaraRPGCommand command) {
        bindCommand("nekararpg", command);
    }

    private void bindCommand(String name, NekaraRPGCommand command) {
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand == null) {
            plugin.getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
            return;
        }
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
    }

    private void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        listeners.add(listener);
    }
}
