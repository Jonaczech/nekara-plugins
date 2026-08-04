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
import cz.nekara.rpg.modules.skills.SkillsModule;
import cz.nekara.rpg.modules.sitting.SittingModule;
import cz.nekara.rpg.sitting.SitResult;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillPresentation;
import cz.nekara.rpg.skills.admin.SkillAdminActor;
import cz.nekara.rpg.skills.admin.SkillAdminInspection;
import cz.nekara.rpg.skills.admin.SkillAdminOperation;
import cz.nekara.rpg.skills.admin.SkillAuditEntry;
import cz.nekara.rpg.skills.admin.SkillAdminResult;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.telemetry.SkillRuntimeMetricsSnapshot;
import cz.nekara.rpg.updater.UpdaterService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class NekaraRPGCommand implements CommandExecutor, TabCompleter {
    private static final DateTimeFormatter AUDIT_TIME = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
        .withZone(ZoneOffset.UTC);

    private final NekaraRPGPlugin plugin;
    private final FishingModule fishingModule;
    private final SittingModule sittingModule;
    private final CampfireModule campfireModule;
    private final MiningModule miningModule;
    private final MountsModule mountsModule;
    private final SkillsModule skillsModule;
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
            SkillsModule skillsModule,
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
        this.skillsModule = skillsModule;
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
            case "skills" -> {
                handleSkillsAdmin(sender, args);
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

    private void handleSkillsAdmin(CommandSender sender, String[] args) {
        if (!require(sender, "nekararpg.skills.admin")) {
            return;
        }
        if (args.length < 3 || !"admin".equalsIgnoreCase(args[1])) {
            messages.send(sender, "skills-admin-usage");
            return;
        }
        if (!modules.isEnabled(SkillsModule.ID)) {
            messages.send(sender, "module-disabled", Map.of("module", SkillsModule.ID));
            return;
        }
        String requestedAction = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
        if ("xp-boost".equals(requestedAction) || "xp-boost-clear".equals(requestedAction)
            || "xp-boost-status".equals(requestedAction)) {
            handleExperienceBoost(sender, args, requestedAction);
            return;
        }
        if (args.length < 4) {
            String action = args[2].toLowerCase(Locale.ROOT);
            if ("metrics".equals(action) && args.length == 3) {
                sendSkillMetrics(sender, skillsModule.runtimeMetrics());
                return;
            }
            if ("export".equals(action) && args.length == 3) {
                dispatchSkillExport(sender);
                return;
            }
            messages.send(sender, "skills-admin-usage");
            return;
        }

        OfflinePlayer target = findKnownPlayer(args[3]);
        if (target == null || target.getName() == null) {
            messages.send(sender, "player-not-found");
            return;
        }
        String action = args[2].toLowerCase(Locale.ROOT);
        if ("inspect".equals(action)) {
            if (args.length != 4) {
                messages.send(sender, "skills-admin-usage");
                return;
            }
            dispatchInspection(sender, target);
            return;
        }

        SkillAdminOperation operation;
        try {
            operation = switch (action) {
                case "grant-xp" -> parseGrantExperience(args);
                case "grant-perk" -> parseGrantPerk(args);
                case "reset" -> parseReset(args);
                default -> null;
            };
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "skills-admin-invalid-input", Map.of("reason", exception.getMessage()));
            return;
        }
        if (operation == null) {
            messages.send(sender, "skills-admin-usage");
            return;
        }

        SkillAdminActor actor = sender instanceof Player player
            ? new SkillAdminActor(player.getUniqueId().toString(), player.getName())
            : new SkillAdminActor("console:" + sender.getName().toLowerCase(Locale.ROOT), sender.getName());
        SkillsModule.AdminDispatchStatus status = skillsModule.executeAdmin(
            actor,
            target.getUniqueId(),
            target.getName(),
            operation,
            result -> messages.send(sender, "skills-admin-result", Map.of(
                "player", target.getName(),
                "summary", adminSummary(result),
                "revision", result.profile().revision(),
                "audit", result.changed() ? "zapsán" : "beze změny"
            )),
            exception -> messages.send(sender, "skills-admin-storage-error")
        );
        sendAdminDispatchStatus(sender, status);
    }

    private SkillAdminOperation parseGrantExperience(String[] args) {
        if (args.length != 6) {
            throw new IllegalArgumentException("Použij grant-xp <hráč> <skill> <množství>.");
        }
        SkillId skill = parseGameplaySkill(args[4]);
        long amount;
        try {
            amount = Long.parseLong(args[5]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Množství XP musí být celé kladné číslo.");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("Množství XP musí být celé kladné číslo.");
        }
        return SkillAdminOperation.grantExperience(skill, amount);
    }

    private SkillAdminOperation parseGrantPerk(String[] args) {
        if (args.length < 5 || args.length > 6) {
            throw new IllegalArgumentException("Použij grant-perk <hráč> <perk-id> [rank].");
        }
        PerkId perkId;
        try {
            perkId = new PerkId(args[4].toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("ID perku musí mít tvar skill.perk_name.");
        }
        int rank = 1;
        if (args.length == 6) {
            try {
                rank = Integer.parseInt(args[5]);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Rank musí být celé kladné číslo.");
            }
        }
        int maximumRank;
        try {
            maximumRank = skillsModule.adminPerkMaxRank(perkId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Neznámé ID perku: " + perkId.value() + ".");
        }
        if (rank < 1 || rank > maximumRank) {
            throw new IllegalArgumentException("Povolený rank tohoto perku je 1 až " + maximumRank + ".");
        }
        return SkillAdminOperation.grantPerk(perkId, rank);
    }

    private SkillAdminOperation parseReset(String[] args) {
        if (args.length != 5) {
            throw new IllegalArgumentException("Použij reset <hráč> <skill|perks|all>.");
        }
        return switch (args[4].toLowerCase(Locale.ROOT)) {
            case "perks" -> SkillAdminOperation.resetPerks();
            case "all" -> SkillAdminOperation.resetAll();
            default -> SkillAdminOperation.resetSkill(parseGameplaySkill(args[4]));
        };
    }

    private SkillId parseGameplaySkill(String value) {
        for (SkillId skill : SkillId.gameplaySkills()) {
            if (skill.id().equalsIgnoreCase(value)) {
                return skill;
            }
        }
        if (SkillId.POWER.id().equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Hlavní úroveň je odvozená a nelze jí přímo měnit XP.");
        }
        throw new IllegalArgumentException("Neznámé ID dovednosti: " + value + ".");
    }

    private void dispatchInspection(CommandSender sender, OfflinePlayer target) {
        SkillsModule.AdminDispatchStatus status = skillsModule.inspectAdmin(
            target.getUniqueId().toString(),
            inspection -> sendInspection(sender, target.getName(), inspection),
            exception -> messages.send(sender, "skills-admin-storage-error")
        );
        sendAdminDispatchStatus(sender, status);
    }

    private void handleExperienceBoost(CommandSender sender, String[] args, String action) {
        int requiredLength = "xp-boost".equals(action) ? 6
            : "xp-boost-clear".equals(action) ? 5 : 4;
        if (args.length != requiredLength) {
            messages.send(sender, "skills-admin-usage");
            return;
        }
        OfflinePlayer target = findKnownPlayer(args[3]);
        if (target == null || target.getName() == null) {
            messages.send(sender, "player-not-found");
            return;
        }
        try {
            if ("xp-boost-status".equals(action)) {
                String overrides = skillsModule.skillExperienceBoosts(target.getUniqueId()).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(java.util.Comparator.comparing(SkillId::id)))
                    .map(entry -> SkillPresentation.czechName(entry.getKey()) + " x" + formatMultiplier(entry.getValue()))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("žádné");
                messages.send(sender, "skills-xp-boost-status", Map.of("player", target.getName(),
                    "all", formatMultiplier(skillsModule.allExperienceBoost(target.getUniqueId())),
                    "overrides", overrides));
                return;
            }
            SkillId skill = "all".equalsIgnoreCase(args[4]) ? null : parseGameplaySkill(args[4]);
            if ("xp-boost-clear".equals(action)) {
                skillsModule.clearExperienceBoost(target.getUniqueId(), skill);
                messages.send(sender, "skills-xp-boost-cleared", Map.of("player", target.getName(),
                    "skill", skill == null ? "vše" : SkillPresentation.czechName(skill)));
                return;
            }
            double multiplier = Double.parseDouble(args[5]);
            skillsModule.setExperienceBoost(target.getUniqueId(), skill, multiplier);
            messages.send(sender, "skills-xp-boost-set", Map.of("player", target.getName(),
                "skill", skill == null ? "vše" : SkillPresentation.czechName(skill),
                "multiplier", formatMultiplier(multiplier)));
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "skills-admin-invalid-input", Map.of("reason", exception.getMessage()));
        }
    }

    private static String formatMultiplier(double multiplier) {
        return String.format(Locale.ROOT, "%.2f", multiplier);
    }

    private void dispatchSkillExport(CommandSender sender) {
        SkillsModule.AdminDispatchStatus status = skillsModule.exportAdmin(
            result -> messages.send(sender, "skills-admin-export-complete", Map.of(
                "file", result.archive().getFileName(),
                "profiles", result.profileCount(),
                "size", result.sizeBytes(),
                "sha256", result.sha256()
            )),
            exception -> messages.send(sender, "skills-admin-export-error")
        );
        switch (status) {
            case STARTED -> messages.send(sender, "skills-admin-export-started");
            case MODULE_DISABLED -> messages.send(sender, "module-disabled", Map.of("module", SkillsModule.ID));
            case STORAGE_UNAVAILABLE -> messages.send(sender, "skills-admin-storage-error");
            case BUSY -> messages.send(sender, "skills-admin-export-busy");
        }
    }

    private void sendSkillMetrics(CommandSender sender, SkillRuntimeMetricsSnapshot metrics) {
        long uptimeSeconds = Math.max(0L,
            (System.currentTimeMillis() - metrics.startedAtEpochMillis()) / 1_000L);
        messages.send(sender, "skills-admin-metrics", Map.ofEntries(
            Map.entry("uptime", uptimeSeconds),
            Map.entry("submitted", metrics.submitted()),
            Map.entry("queue_rejected", metrics.queueRejected()),
            Map.entry("completed", metrics.completed()),
            Map.entry("awarded", metrics.awarded()),
            Map.entry("denied", metrics.denied()),
            Map.entry("duplicate", metrics.duplicate()),
            Map.entry("capped", metrics.capped()),
            Map.entry("failed", metrics.failed()),
            Map.entry("xp", metrics.awardedExperience()),
            Map.entry("queue", metrics.queueDepth()),
            Map.entry("queue_high", metrics.queueHighWater()),
            Map.entry("avg_ms", String.format(Locale.ROOT, "%.3f", metrics.averageLatencyMicros() / 1_000.0)),
            Map.entry("max_ms", String.format(Locale.ROOT, "%.3f", metrics.maximumLatencyMicros() / 1_000.0))
        ));
    }

    private void sendInspection(
        CommandSender sender,
        String targetName,
        SkillAdminInspection inspection
    ) {
        int availablePoints = Math.max(
            0,
            inspection.progress().power().level() - inspection.profile().spentPerkPoints()
        );
        messages.send(sender, "skills-admin-inspect-header", Map.of(
            "player", targetName,
            "revision", inspection.profile().revision(),
            "power", inspection.progress().power().level(),
            "spent", inspection.profile().spentPerkPoints(),
            "available", availablePoints
        ));
        for (SkillId skill : SkillId.gameplaySkills()) {
            messages.send(sender, "skills-admin-inspect-skill", Map.of(
                "name", SkillPresentation.czechName(skill),
                "id", skill.id(),
                "level", inspection.progress().skill(skill).level(),
                "experience", inspection.profile().totalExperience(skill)
            ));
        }
        String perks = inspection.profile().perkRanks().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> entry.getKey().value() + "=" + entry.getValue())
            .reduce((left, right) -> left + ", " + right)
            .orElse("žádné");
        messages.send(sender, "skills-admin-inspect-perks", Map.of("perks", perks));
        if (inspection.recentAuditEntries().isEmpty()) {
            messages.send(sender, "skills-admin-inspect-no-audit");
            return;
        }
        messages.send(sender, "skills-admin-inspect-audit-header");
        for (SkillAuditEntry entry : inspection.recentAuditEntries()) {
            messages.send(sender, "skills-admin-inspect-audit", Map.of(
                "time", AUDIT_TIME.format(Instant.ofEpochMilli(entry.occurredAtEpochMillis())),
                "actor", entry.actor().displayName(),
                "operation", entry.operation(),
                "before", entry.revisionBefore(),
                "after", entry.revisionAfter(),
                "detail", entry.detail()
            ));
        }
    }

    private String adminSummary(SkillAdminResult result) {
        if (!result.changed()) {
            return switch (result.status()) {
                case ALREADY_CAPPED -> "Dovednost už dosáhla maximální úrovně.";
                case RANK_ALREADY_PRESENT -> "Perk už má stejný nebo vyšší rank.";
                case SKILL_ALREADY_EMPTY -> "Dovednost už má 0 XP.";
                case PERKS_ALREADY_EMPTY -> "Profil už nemá žádné perky ani utracené body.";
                case PROFILE_ALREADY_EMPTY -> "Profil už je prázdný.";
                case CHANGED -> throw new IllegalStateException("Changed result cannot be unchanged");
            };
        }
        return switch (result.operation().type()) {
            case GRANT_EXPERIENCE -> "Přidáno " + result.affectedValue() + " XP do "
                + result.operation().skill().id() + ".";
            case GRANT_PERK -> "Perk " + result.operation().perkId().value()
                + " nastaven na rank " + result.operation().rank() + ".";
            case RESET_SKILL -> "XP dovednosti " + result.operation().skill().id()
                + " byla vynulována.";
            case RESET_PERKS -> "Perky byly resetovány a utracené body vráceny.";
            case RESET_ALL -> "XP, perky i utracené body byly resetovány.";
        };
    }

    private void sendAdminDispatchStatus(
        CommandSender sender,
        SkillsModule.AdminDispatchStatus status
    ) {
        switch (status) {
            case STARTED -> messages.send(sender, "skills-admin-processing");
            case MODULE_DISABLED -> messages.send(sender, "module-disabled", Map.of("module", SkillsModule.ID));
            case STORAGE_UNAVAILABLE -> messages.send(sender, "skills-admin-storage-error");
            case BUSY -> messages.send(sender, "skills-admin-busy");
        }
    }

    private OfflinePlayer findKnownPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(name)) {
                return offline;
            }
        }
        return null;
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
            return prefix(List.of("menu", "help", "reload", "status", "update", "skills", "sit", "stand", "lay", "rise", "mount", "test", "cancel"), args[0]);
        }
        if (sender.hasPermission("nekararpg.skills.admin")
                && args.length == 2 && "skills".equalsIgnoreCase(args[0])) {
            return prefix(List.of("admin"), args[1]);
        }
        if (sender.hasPermission("nekararpg.skills.admin")
                && args.length == 3 && "skills".equalsIgnoreCase(args[0])
                && "admin".equalsIgnoreCase(args[1])) {
            return prefix(List.of("inspect", "grant-xp", "grant-perk", "reset", "metrics", "export"), args[2]);
        }
        if (sender.hasPermission("nekararpg.skills.admin")
                && args.length == 4 && "skills".equalsIgnoreCase(args[0])
                && "admin".equalsIgnoreCase(args[1])) {
            String action = args[2].toLowerCase(Locale.ROOT);
            if ("metrics".equals(action) || "export".equals(action)) {
                return List.of();
            }
            return prefix(Bukkit.getOfflinePlayers().length == 0
                ? List.of()
                : java.util.Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .filter(java.util.Objects::nonNull)
                    .toList(), args[3]);
        }
        if (sender.hasPermission("nekararpg.skills.admin")
                && args.length == 5 && "skills".equalsIgnoreCase(args[0])
                && "admin".equalsIgnoreCase(args[1])) {
            String action = args[2].toLowerCase(Locale.ROOT);
            if ("grant-xp".equals(action)) {
                return prefix(SkillId.gameplaySkills().stream().map(SkillId::id).toList(), args[4]);
            }
            if ("grant-perk".equals(action)) {
                return prefix(skillsModule.adminPerkIds(), args[4]);
            }
            if ("reset".equals(action)) {
                List<String> targets = new ArrayList<>(
                    SkillId.gameplaySkills().stream().map(SkillId::id).toList());
                targets.add("perks");
                targets.add("all");
                return prefix(targets, args[4]);
            }
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
