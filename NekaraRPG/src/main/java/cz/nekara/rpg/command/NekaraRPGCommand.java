package cz.nekara.rpg.command;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.campfire.LieResult;
import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.menu.NekaraRPGMenu;
import cz.nekara.rpg.minigame.FishingMinigameManager;
import cz.nekara.rpg.modules.ModuleRegistry;
import cz.nekara.rpg.modules.campfire.CampfireModule;
import cz.nekara.rpg.modules.fishing.FishingModule;
import cz.nekara.rpg.modules.mining.MiningModule;
import cz.nekara.rpg.modules.mounts.MountsModule;
import cz.nekara.rpg.modules.sitting.SittingModule;
import cz.nekara.rpg.sitting.SitResult;
import cz.nekara.rpg.updater.UpdaterService;
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
    private final MiningModule miningModule;
    private final MountsModule mountsModule;
    private final ModuleRegistry modules;
    private final MessageService messages;
    private final UpdaterService updater;
    private final NekaraRPGMenu mainMenu;

    public NekaraRPGCommand(
            NekaraRPGPlugin plugin,
            FishingModule fishingModule,
            SittingModule sittingModule,
            CampfireModule campfireModule,
            MiningModule miningModule,
            MountsModule mountsModule,
            ModuleRegistry modules,
            MessageService messages,
            UpdaterService updater,
            NekaraRPGMenu mainMenu
    ) {
        this.plugin = plugin;
        this.fishingModule = fishingModule;
        this.sittingModule = sittingModule;
        this.campfireModule = campfireModule;
        this.miningModule = miningModule;
        this.mountsModule = mountsModule;
        this.modules = modules;
        this.messages = messages;
        this.updater = updater;
        this.mainMenu = mainMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0
                ? (sender instanceof Player ? "menu" : "help")
                : args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "menu" -> {
                if (!require(sender, "nekararpg.menu.use")) yield true;
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    yield true;
                }
                mainMenu.open(player);
                yield true;
            }
            case "help" -> {
                if (!require(sender, "nekararpg.command.help", "nekarafishing.command.help")) yield true;
                messages.send(sender, "help");
                messages.send(sender, "update-help");
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
                        "echo_active", miningModule.activeCount(),
                        "mounts_active", mountsModule.activeCount(),
                        "modules", modules.enabledModuleIds().isEmpty()
                                ? "none"
                                : String.join(", ", modules.enabledModuleIds())
                ));
                yield true;
            }
            case "mount" -> {
                if (!modules.isEnabled(MountsModule.ID)) {
                    messages.send(sender, "module-disabled", Map.of("module", MountsModule.ID));
                    yield true;
                }
                String action = args.length < 2 ? "menu" : args[1].toLowerCase(Locale.ROOT);
                if ("grant".equals(action)) {
                    if (!require(sender, "nekararpg.mount.admin")) yield true;
                    if (args.length < 3) {
                        messages.send(sender, "mount-admin-usage");
                        yield true;
                    }
                    Player target = Bukkit.getPlayerExact(args[2]);
                    if (target == null) {
                        messages.send(sender, "player-not-found");
                        yield true;
                    }
                    boolean granted = mountsModule.grant(target);
                    if (granted && !sender.equals(target)) {
                        messages.send(sender, "mount-admin-dispatched", Map.of("player", target.getName()));
                    }
                    yield true;
                }
                if (!require(sender, "nekararpg.mount.use")) yield true;
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    yield true;
                }
                switch (action) {
                    case "menu", "manage", "create" -> mountsModule.openMenu(player);
                    case "summon", "call" -> mountsModule.call(player);
                    case "dismiss", "recall" -> mountsModule.dismiss(player);
                    case "status" -> mountsModule.sendStatus(player);
                    case "whistle" -> {
                        String whistleAction = args.length < 3
                                ? "restore" : args[2].toLowerCase(Locale.ROOT);
                        if ("restore".equals(whistleAction)) {
                            mountsModule.restoreWhistle(player);
                        } else if ("remove".equals(whistleAction)) {
                            mountsModule.removeWhistle(player);
                        } else {
                            messages.send(sender, "mount-whistle-usage");
                        }
                    }
                    default -> messages.send(sender, "mount-usage");
                }
                yield true;
            }
            case "update" -> {
                if (!require(sender, "nekararpg.command.update")) yield true;
                String action = args.length < 2 ? "status" : args[1].toLowerCase(Locale.ROOT);
                if ("check".equals(action)) {
                    updater.requestCheck(sender);
                } else if ("status".equals(action)) {
                    updater.sendStatus(sender);
                } else {
                    messages.send(sender, "update-usage");
                }
                yield true;
            }
            case "sit" -> {
                if (!require(sender, "nekararpg.sitting.use")) yield true;
                if (!modules.isEnabled(CampfireModule.ID)) {
                    messages.send(sender, "module-disabled", Map.of("module", CampfireModule.ID));
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
            case "lay" -> {
                if (!require(sender, "nekararpg.campfire.use")) yield true;
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    yield true;
                }
                LieResult result = campfireModule.lieDown(player);
                messages.send(player, switch (result) {
                    case SUCCESS -> "campfire-lying-started";
                    case MODULE_DISABLED, LYING_DISABLED -> "campfire-lying-disabled";
                    case ALREADY_RESTING -> "campfire-already-resting";
                    case NOT_NEAR_CAMPFIRE -> "campfire-lying-needs-fire";
                    case INVALID_STATE -> "campfire-lying-invalid-state";
                });
                yield true;
            }
            case "rise" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    yield true;
                }
                messages.send(player, campfireModule.rise(player)
                        ? "campfire-lying-stopped" : "campfire-not-lying");
                yield true;
            }
            case "test" -> {
                if (!require(sender, "nekararpg.command.test", "nekarafishing.command.test")) yield true;
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "test-player-only");
                    yield true;
                }
                String testType = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "fishing";
                if ("vein".equals(testType) || "echo".equals(testType)) {
                    if (!modules.isEnabled(MiningModule.ID)) {
                        messages.send(sender, "module-disabled", Map.of("module", MiningModule.ID));
                    } else if (!miningModule.startTest(player)) {
                        messages.send(sender, "echo-vein-test-unavailable");
                    }
                    yield true;
                }
                if (!"fishing".equals(testType)) {
                    messages.send(sender, "test-usage");
                    yield true;
                }
                if (!modules.isEnabled(FishingModule.ID)) {
                    messages.send(sender, "module-disabled", Map.of("module", FishingModule.ID));
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
                boolean cancelledFishing = manager.cancelByCommand(
                        target.getUniqueId(), sender, target.getName());
                boolean cancelledEcho = !cancelledFishing && miningModule.cancelByCommand(
                        target.getUniqueId(), sender, target.getName());
                if (!cancelledFishing && !cancelledEcho) {
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
            return prefix(List.of("menu", "help", "reload", "status", "update", "sit", "stand", "lay", "rise", "mount", "test", "cancel"), args[0]);
        }
        if (args.length == 2 && "update".equalsIgnoreCase(args[0])) {
            return prefix(List.of("check", "status"), args[1]);
        }
        if (args.length == 2 && "test".equalsIgnoreCase(args[0])) {
            return prefix(List.of("fishing", "vein"), args[1]);
        }
        if (args.length == 2 && "mount".equalsIgnoreCase(args[0])) {
            return prefix(List.of("menu", "status", "call", "dismiss", "whistle", "grant"), args[1]);
        }
        if (args.length == 3 && "mount".equalsIgnoreCase(args[0])
                && "whistle".equalsIgnoreCase(args[1])) {
            return prefix(List.of("restore", "remove"), args[2]);
        }
        if (args.length == 3 && "mount".equalsIgnoreCase(args[0])
                && "grant".equalsIgnoreCase(args[1])) {
            return prefix(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[2]);
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
