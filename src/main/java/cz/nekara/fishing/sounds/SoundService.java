package cz.nekara.fishing.sounds;

import cz.nekara.fishing.configuration.SoundSettings;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class SoundService {
    private final JavaPlugin plugin;
    private Map<String, SoundSettings> sounds = Map.of();

    public SoundService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload(Map<String, SoundSettings> sounds) {
        this.sounds = sounds;
        sounds.forEach((event, settings) -> validate(event, settings));
    }

    public void play(Player player, String event) {
        SoundSettings settings = sounds.get(event);
        if (settings == null || !settings.enabled() || settings.id().isBlank()) {
            return;
        }
        if (!validate(event, settings)) {
            return;
        }
        player.playSound(player.getLocation(), settings.id(), settings.volume(), settings.pitch());
    }

    private boolean validate(String event, SoundSettings settings) {
        NamespacedKey key = NamespacedKey.fromString(settings.id());
        if (key == null) {
            plugin.getLogger().warning("Invalid sound id for '" + event + "': " + settings.id() + "; skipping it.");
            return false;
        }
        if ("minecraft".equals(key.getNamespace()) && Registry.SOUNDS.get(key) == null) {
            plugin.getLogger().warning("Unknown vanilla sound for '" + event + "': " + settings.id() + "; skipping it.");
            return false;
        }
        return true;
    }
}
