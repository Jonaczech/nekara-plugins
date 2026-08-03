package cz.nekara.rpg.messages;

import cz.nekara.rpg.skills.SkillId;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private FileConfiguration bundledMessages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        bundledMessages = null;
        try (InputStream stream = plugin.getResource("messages.yml")) {
            if (stream != null) {
                bundledMessages = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                mergeSkillMessages(loaded, (YamlConfiguration) bundledMessages);
                loaded.setDefaults(bundledMessages);
            }
        } catch (java.io.IOException exception) {
            plugin.getLogger().warning("Could not close the bundled messages resource: " + exception.getMessage());
        }
        messages = loaded;
    }

    private void mergeSkillMessages(
            YamlConfiguration loaded,
            YamlConfiguration bundled
    ) {
        for (SkillId skill : SkillId.gameplaySkills()) {
            String resourcePath = "skills/" + skill.id() + "/messages.yml";
            File file = new File(plugin.getDataFolder(), resourcePath);
            if (!file.isFile()) {
                plugin.saveResource(resourcePath, false);
            }
            YamlConfiguration skillMessages = YamlConfiguration.loadConfiguration(file);
            try (InputStream stream = plugin.getResource(resourcePath)) {
                if (stream == null) {
                    throw new IllegalStateException("Missing bundled messages: " + resourcePath);
                }
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                mergeUnder("skills." + skill.id(), defaults, bundled);
                mergeUnder("skills." + skill.id(), skillMessages, loaded);
            } catch (java.io.IOException exception) {
                plugin.getLogger().warning(
                        "Could not close bundled skill messages " + resourcePath + ": "
                                + exception.getMessage());
            }
        }
    }

    private void mergeUnder(String root, FileConfiguration source, YamlConfiguration target) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(root + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    public Component component(String key, Map<String, ?> placeholders) {
        if (messages == null) {
            reload();
        }
        String value = configuredString(key, key);
        String prefix = configuredString("prefix", "");
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

    private String configuredString(String key, String fallback) {
        String value = messages.getString(key);
        if (value == null && bundledMessages != null) {
            value = bundledMessages.getString(key);
        }
        return value == null ? fallback : value;
    }

    public void send(CommandSender sender, String key, Map<String, ?> placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void sendActionBar(Player player, String key, Map<String, ?> placeholders) {
        player.sendActionBar(component(key, placeholders));
    }

    public String legacyText(String key, Map<String, ?> placeholders) {
        return LegacyComponentSerializer.legacySection().serialize(component(key, placeholders));
    }
}
