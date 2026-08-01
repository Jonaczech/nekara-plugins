package cz.nekara.rpg.messages;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private FileConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public Component component(String key, Map<String, ?> placeholders) {
        if (messages == null) {
            reload();
        }
        String value = messages.getString(key, key);
        String prefix = messages.getString("prefix", "");
        if (!"prefix".equals(key)) {
            value = value.replace("%prefix%", prefix);
        }
        for (Map.Entry<String, ?> entry : placeholders.entrySet()) {
            value = value.replace("%" + entry.getKey() + "%", String.valueOf(entry.getValue()));
        }
        if (value.contains("<") && value.contains(">")) {
            return MiniMessage.miniMessage().deserialize(value);
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(value);
    }

    public void send(CommandSender sender, String key, Map<String, ?> placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }
}
