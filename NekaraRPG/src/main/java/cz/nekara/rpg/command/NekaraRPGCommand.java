package cz.nekara.rpg.command;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.minigame.FishingMinigameManager;
import cz.nekara.rpg.modules.ModuleRegistry;
import cz.nekara.rpg.modules.fishing.FishingModule;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NekaraRPGCommand implements CommandExecutor, TabCompleter {
    private final NekaraRPGPlugin plugin;
    private final FishingModule fishingModule;
    private final ModuleRegistry modules;
    private final MessageService messages;

    public NekaraRPGCommand(
            NekaraRPGPlugin plugin,
            FishingModule fishingModule,
            ModuleRegistry modules,
            MessageService messages
    ) {
        this.plugin = plugin;
        this.fishingModule = fishingModule;
        this.modules = modules;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "help" -> {
                if (!require(sender, "nekararpg.command.help", "nekarafishing.command.help")) yield true;
                messages.send(sender, "help");
                yield true;
            }
            case "reload" -> {
                if (!require(sender, "nekararpg.command.reload", "nekarafishing.command.reload")) yield true;
                plugin.reloadPlugin();
                messages.send(sender, "reloaded");
                yield true;
            }
            case "status" -> {
                if (!require(sender, "nekararpg.command.status", "nekarafishing.command.status")) yield true;
                FishingMinigameManager manager = fishingModule.minigames();
                messages.send(sender, "status", Map.of(
                        "version", plugin.getDescription().getVersion(),
                        "mode", manager.modeName(),
                        "active", manager.activeCount(),
                        "modules", modules.enabledModuleIds().isEmpty()
                                ? "none"
                                : String.join(", ", modules.enabledModuleIds())
                ));
                yield true;
            }
            case "test" -> {
                if (!require(sender, "nekararpg.command.test", "nekarafishing.command.test")) yield true;
                if (!modules.isEnabled(FishingModule.ID)) {
                    messages.send(sender, "module-disabled", Map.of("module", FishingModule.ID));
                    yield true;
                }
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "test-player-only");
                    yield true;
                }
                FishingMinigameManager manager = fishingModule.minigames();
                if (manager.startTest(player)) {
                    messages.send(sender, "test-started");
                } else {
                    messages.send(sender, "test-already-active");
                }
                yield true;
            }
            case "cancel" -> {
                if (!require(sender, "nekararpg.command.cancel", "nekarafishing.command.cancel")) yield true;
                FishingMinigameManager manager = fishingModule.minigames();
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayerExact(args[1]);
                    if (target == null) {
                        messages.send(sender, "player-not-found");
                        yield true;
                    }
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    messages.send(sender, "invalid-command");
                    yield true;
                }
                if (!manager.cancelByCommand(target.getUniqueId(), sender, target.getName())) {
                    messages.send(sender, "no-active-game");
                }
                yield true;
            }
            default -> {
                messages.send(sender, "invalid-command");
                yield true;
            }
        };
    }

    private boolean require(CommandSender sender, String permission, String legacyPermission) {
        if (sender.hasPermission(permission) || sender.hasPermission(legacyPermission)) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefix(List.of("help", "reload", "status", "test", "cancel"), args[0]);
        }
        if (args.length == 2 && "cancel".equalsIgnoreCase(args[0])) {
            List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return prefix(names, args[1]);
        }
        return List.of();
    }

    private List<String> prefix(List<String> values, String prefix) {
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.regionMatches(true, 0, prefix, 0, prefix.length())) {
                result.add(value);
            }
        }
        return result;
    }
}
