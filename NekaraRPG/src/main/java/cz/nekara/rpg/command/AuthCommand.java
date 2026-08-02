package cz.nekara.rpg.command;

import cz.nekara.rpg.messages.MessageService;
import cz.nekara.rpg.modules.auth.AuthModule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AuthCommand implements CommandExecutor, TabCompleter {
    private final AuthModule auth;
    private final MessageService messages;

    public AuthCommand(AuthModule auth, MessageService messages) {
        this.auth = auth;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ("login".equals(name)) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
            } else if (!auth.canUseFallbackCommands(player)) {
                Arrays.fill(args, "");
                messages.send(sender, "auth-fallback-disabled");
            } else if (args.length != 1) {
                messages.send(sender, "auth-login-usage");
            } else {
                char[] password = args[0].toCharArray();
                Arrays.fill(args, "");
                auth.login(player, password);
            }
            return true;
        }
        if ("register".equals(name)) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
            } else if (!auth.canUseFallbackCommands(player)) {
                Arrays.fill(args, "");
                messages.send(sender, "auth-fallback-disabled");
            } else if (args.length != 2) {
                messages.send(sender, "auth-register-usage");
            } else {
                char[] password = args[0].toCharArray();
                char[] confirmation = args[1].toCharArray();
                Arrays.fill(args, "");
                auth.register(player, password, confirmation);
            }
            return true;
        }
        if ("logout".equals(name)) {
            if (sender instanceof Player player) {
                auth.logout(player);
            } else {
                messages.send(sender, "player-only");
            }
            return true;
        }

        String action = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);
        if (sender instanceof Player player && !auth.isAuthenticated(player) && !"menu".equals(action)) {
            messages.send(sender, "auth-required");
            return true;
        }
        switch (action) {
            case "menu" -> {
                if (sender instanceof Player player) {
                    auth.openMenu(player);
                } else {
                    messages.send(sender, "player-only");
                }
            }
            case "status" -> {
                if (!sender.hasPermission("nekararpg.auth.admin")) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                messages.send(sender, "auth-status", Map.of(
                        "accounts", auth.accountCount(),
                        "authenticated", auth.authenticatedCount()));
            }
            case "unregister" -> {
                if (!sender.hasPermission("nekararpg.auth.admin")) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                if (args.length != 2) {
                    messages.send(sender, "auth-admin-usage");
                    return true;
                }
                try {
                    messages.send(sender, auth.unregister(args[1])
                            ? "auth-admin-unregistered" : "auth-admin-not-found",
                            Map.of("player", args[1]));
                } catch (IOException exception) {
                    messages.send(sender, "auth-error");
                }
            }
            default -> messages.send(sender, "auth-admin-usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!"nekaraauth".equals(command.getName()) || args.length != 1) {
            return List.of();
        }
        if (sender instanceof Player player && !auth.isAuthenticated(player)) {
            return List.of("menu");
        }
        return sender.hasPermission("nekararpg.auth.admin")
                ? List.of("menu", "status", "unregister") : List.of("menu");
    }
}
