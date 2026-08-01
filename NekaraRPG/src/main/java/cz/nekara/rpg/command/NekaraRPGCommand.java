package cz.nekara.rpg.command;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.minigame.FishingMinigameManager;
import cz.nekara.rpg.modules.ModuleRegistry;
import cz.nekara.rpg.modules.campfire.CampfireModule;
import cz.nekara.rpg.modules.fishing.FishingModule;
import cz.nekara.rpg.modules.sitting.SittingModule;
import cz.nekara.rpg.sitting.SitResult;
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
    private final SittingModule sittingModule;
    private final CampfireModule campfireModule;
    private final ModuleRegistry modules;
    private final MessageService messages;

    public NekaraRPGCommand(
            NekaraRPGPlugin plugin,
            FishingModule fishingModule,
            SittingModule sittingModule,
            CampfireModule campfireModule,
            ModuleRegistry modules,
            MessageService messages
    ) {
        this.plugin = plugin;
        this.fishingModule = fishingModule;
        this.sittingModule = sittingModule;
        this.campfireModule = campfireModule;
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
                        "seated", sittingModule.seatedCount(),
                        "resting", campfireModule.restingCount(),
                        "rested", campfireModule.restedCount(),
                        "modules", modules.enabledModuleIds().isEmpty()
                                ? "none"
                                : String.join(", ", modules.enabledModuleIds())
                ));
                yield true;
            }
            case "sit" -> {
                if (!require(sender, "nekararpg.sitting.use")) yield true;
                if (!modules.isEnabled(SittingModule.ID)) {
                    messages.send(sender, "module-disabled", Map.of("module", SittingModule.ID));
                    yield true;
                }
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    yield true;
                }
                SitResult result = sittingModule.sit(player);
                messages.send(player, switch (result) {
                    case SUCCESS -> "sitting-started";
                    case ALREADY_SITTING -> "sitting-already";
                    case ALREADY_RIDING -> "sitting-riding";
                    case NOT_ON_GROUND -> "sitting-ground-required";
                    case INVALID_STATE -> "sitting-invalid-state";
                    case MODULE_DISABLED -> "sitting-disabled";
                    case FAILED -> "sitting-failed";
                });
                yield true;
            }
            case "stand" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    yield true;
                }
                messages.send(player, sittingModule.stand(player)
                        ? "sitting-stopped" : "sitting-not-seated");
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

    private boolean require(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return prefix(List.of("help", "reload", "status", "sit", "stand", "test", "cancel"), args[0]);
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
