package cz.nekara.rpg.updater;

import cz.nekara.rpg.NekaraRPGPlugin;
import cz.nekara.rpg.configuration.UpdaterConfig;
import cz.nekara.rpg.messages.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdaterService implements Listener {
    private static final URI LATEST_RELEASE_URI = URI.create(
            "https://api.github.com/repos/Jonaczech/nekara-plugins/releases/latest");
    private static final String GITHUB_API_VERSION = "2026-03-10";
    private static final String NOTIFY_PERMISSION = "nekararpg.update.notify";

    private final NekaraRPGPlugin plugin;
    private final MessageService messages;
    private final GitHubReleaseParser releaseParser = new GitHubReleaseParser();
    private final HttpClient httpClient;
    private final Path stagedJarPath;
    private final Path runningJarPath;
    private final Path backupDirectory;
    private final AtomicBoolean checking = new AtomicBoolean();
    private volatile UpdaterConfig config;
    private volatile UpdaterSnapshot snapshot;
    private BukkitTask automaticTask;
    private String lastNotifiedVersion;

    public UpdaterService(NekaraRPGPlugin plugin, MessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.stagedJarPath = Bukkit.getUpdateFolderFile().toPath()
                .toAbsolutePath()
                .normalize()
                .resolve(GitHubReleaseParser.ASSET_NAME);
        this.runningJarPath = resolveRunningJarPath();
        this.backupDirectory = plugin.getDataFolder().toPath()
                .toAbsolutePath()
                .normalize()
                .resolve("backups");
        this.snapshot = UpdaterSnapshot.initial(plugin.getDescription().getVersion());
    }

    public void reload(UpdaterConfig newConfig) {
        stopAutomaticTask();
        config = newConfig;
        if (!newConfig.enabled()) {
            snapshot = new UpdaterSnapshot(
                    UpdaterState.DISABLED,
                    plugin.getDescription().getVersion(),
                    snapshot.latestVersion(),
                    "",
                    snapshot.checkedAt()
            );
            return;
        }
        if (snapshot.state() == UpdaterState.DISABLED) {
            snapshot = UpdaterSnapshot.initial(plugin.getDescription().getVersion());
        }
        if (newConfig.automaticChecks()) {
            long delayTicks = newConfig.startupDelaySeconds() * 20L;
            long intervalTicks = newConfig.checkIntervalHours() * 60L * 60L * 20L;
            automaticTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> requestCheck(null),
                    delayTicks,
                    intervalTicks
            );
        }
    }

    public void shutdown() {
        stopAutomaticTask();
    }

    public UpdaterSnapshot snapshot() {
        return snapshot;
    }

    public void requestCheck(CommandSender requester) {
        UpdaterConfig activeConfig = config;
        if (activeConfig == null || !activeConfig.enabled()) {
            if (requester != null) {
                messages.send(requester, "update-disabled");
            }
            return;
        }
        if (!checking.compareAndSet(false, true)) {
            if (requester != null) {
                messages.send(requester, "update-check-busy");
            }
            return;
        }

        snapshot = new UpdaterSnapshot(
                UpdaterState.CHECKING,
                plugin.getDescription().getVersion(),
                snapshot.latestVersion(),
                "",
                snapshot.checkedAt()
        );
        if (requester != null) {
            messages.send(requester, "update-check-started");
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UpdaterSnapshot result;
            try {
                result = performCheck(activeConfig);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                result = failedSnapshot("Kontrola byla prerusena.");
            } catch (Exception exception) {
                result = failedSnapshot(safeDetail(exception));
            }
            finishCheck(requester, result);
        });
    }

    public void sendStatus(CommandSender sender) {
        sendSnapshot(sender, snapshot);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UpdaterConfig activeConfig = config;
        if (activeConfig == null || !activeConfig.notifyAdmins()
                || !event.getPlayer().hasPermission(NOTIFY_PERMISSION)) {
            return;
        }
        UpdaterSnapshot current = snapshot;
        if (current.state() == UpdaterState.STAGED || current.state() == UpdaterState.AVAILABLE) {
            sendSnapshot(event.getPlayer(), current);
        }
    }

    private UpdaterSnapshot performCheck(UpdaterConfig activeConfig)
            throws IOException, InterruptedException {
        HttpRequest request = baseRequest(LATEST_RELEASE_URI, activeConfig)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("GitHub API vratilo HTTP " + response.statusCode() + ".");
        }

        GitHubRelease release = releaseParser.parse(response.body());
        SemanticVersion currentVersion = SemanticVersion.parseStable(
                plugin.getDescription().getVersion());
        Instant checkedAt = Instant.now();
        if (release.version().compareTo(currentVersion) <= 0) {
            return new UpdaterSnapshot(
                    UpdaterState.CURRENT,
                    currentVersion.toString(),
                    release.version().toString(),
                    "",
                    checkedAt
            );
        }
        if (!activeConfig.autoDownload()) {
            return new UpdaterSnapshot(
                    UpdaterState.AVAILABLE,
                    currentVersion.toString(),
                    release.version().toString(),
                    "",
                    checkedAt
            );
        }

        backupCurrentJar(currentVersion);
        stageRelease(release, activeConfig);
        return new UpdaterSnapshot(
                UpdaterState.STAGED,
                currentVersion.toString(),
                release.version().toString(),
                stagedJarPath.toString(),
                checkedAt
        );
    }

    private void stageRelease(GitHubRelease release, UpdaterConfig activeConfig)
            throws IOException, InterruptedException {
        long maximumBytes = activeConfig.maximumJarSizeMegabytes() * 1_024L * 1_024L;
        if (release.assetSize() > maximumBytes) {
            throw new IOException("Release JAR prekrocil povoleny limit velikosti.");
        }
        Files.createDirectories(stagedJarPath.getParent());
        if (Files.isRegularFile(stagedJarPath)) {
            try {
                JarArtifactVerifier.verify(
                        stagedJarPath,
                        release.assetSize(),
                        release.sha256(),
                        release.version().toString()
                );
                return;
            } catch (IOException ignored) {
                Files.delete(stagedJarPath);
            }
        }

        Path temporary = stagedJarPath.resolveSibling(
                ".NekaraRPG-" + UUID.randomUUID() + ".part");
        try {
            HttpRequest request = baseRequest(release.downloadUri(), activeConfig)
                    .header("Accept", "application/octet-stream")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                response.body().close();
                throw new IOException("GitHub download vratil HTTP " + response.statusCode() + ".");
            }
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (contentLength > maximumBytes) {
                response.body().close();
                throw new IOException("Stahovany JAR prekrocil povoleny limit velikosti.");
            }
            try (InputStream input = response.body();
                 OutputStream output = Files.newOutputStream(
                         temporary, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                copyWithLimit(input, output, maximumBytes);
            }
            JarArtifactVerifier.verify(
                    temporary,
                    release.assetSize(),
                    release.sha256(),
                    release.version().toString()
            );
            moveAtomically(temporary, stagedJarPath);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void backupCurrentJar(SemanticVersion currentVersion) throws IOException {
        if (runningJarPath == null || !Files.isRegularFile(runningJarPath)) {
            throw new IOException("Running NekaraRPG JAR could not be located for backup.");
        }
        Files.createDirectories(backupDirectory);
        Path backup = backupDirectory.resolve("NekaraRPG-" + currentVersion + ".jar");
        String runningHash = JarArtifactVerifier.sha256(runningJarPath);
        if (Files.isRegularFile(backup)
                && Files.size(backup) == Files.size(runningJarPath)
                && runningHash.equals(JarArtifactVerifier.sha256(backup))) {
            return;
        }

        Path temporary = backup.resolveSibling("." + backup.getFileName()
                + "-" + UUID.randomUUID() + ".part");
        try {
            Files.copy(runningJarPath, temporary, StandardCopyOption.REPLACE_EXISTING);
            if (Files.size(temporary) != Files.size(runningJarPath)
                    || !runningHash.equals(JarArtifactVerifier.sha256(temporary))) {
                throw new IOException("NekaraRPG rollback backup verification failed.");
            }
            moveAtomically(temporary, backup);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private Path resolveRunningJarPath() {
        try {
            URI location = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            return "file".equalsIgnoreCase(location.getScheme())
                    ? Path.of(location).toAbsolutePath().normalize()
                    : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private HttpRequest.Builder baseRequest(URI uri, UpdaterConfig activeConfig) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(activeConfig.requestTimeoutSeconds()))
                .header("User-Agent", "NekaraRPG/" + plugin.getDescription().getVersion())
                .header("X-GitHub-Api-Version", GITHUB_API_VERSION);
    }

    private void copyWithLimit(InputStream input, OutputStream output, long maximumBytes)
            throws IOException {
        byte[] buffer = new byte[16_384];
        long total = 0L;
        int read;
        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > maximumBytes) {
                throw new IOException("Stahovany JAR prekrocil povoleny limit velikosti.");
            }
            output.write(buffer, 0, read);
        }
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void finishCheck(CommandSender requester, UpdaterSnapshot result) {
        snapshot = result;
        checking.set(false);
        if (!plugin.isEnabled()) {
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (requester != null) {
                    sendSnapshot(requester, result);
                }
                logResult(result);
                notifyAdmins(result, requester);
            });
        } catch (RuntimeException ignored) {
            // The plugin may have been disabled between the state check and scheduling.
        }
    }

    private void logResult(UpdaterSnapshot result) {
        switch (result.state()) {
            case STAGED -> plugin.getLogger().info("NekaraRPG " + result.latestVersion()
                    + " was verified and staged for the next server restart.");
            case AVAILABLE -> plugin.getLogger().info("NekaraRPG " + result.latestVersion()
                    + " is available on GitHub.");
            case FAILED -> plugin.getLogger().warning("NekaraRPG update check failed: " + result.detail());
            default -> {
            }
        }
    }

    private void notifyAdmins(UpdaterSnapshot result, CommandSender requester) {
        UpdaterConfig activeConfig = config;
        if (activeConfig == null || !activeConfig.notifyAdmins()
                || (result.state() != UpdaterState.STAGED
                && result.state() != UpdaterState.AVAILABLE)
                || result.latestVersion() == null
                || result.latestVersion().equals(lastNotifiedVersion)) {
            return;
        }
        lastNotifiedVersion = result.latestVersion();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != requester && player.hasPermission(NOTIFY_PERMISSION)) {
                sendSnapshot(player, result);
            }
        }
    }

    private void sendSnapshot(CommandSender sender, UpdaterSnapshot value) {
        Map<String, Object> placeholders = Map.of(
                "current", value.currentVersion() == null ? "?" : value.currentVersion(),
                "latest", value.latestVersion() == null ? "?" : value.latestVersion(),
                "reason", value.detail() == null || value.detail().isBlank() ? "-" : value.detail()
        );
        String key = switch (value.state()) {
            case DISABLED -> "update-disabled";
            case IDLE -> "update-status-idle";
            case CHECKING -> "update-check-busy";
            case CURRENT -> "update-current";
            case AVAILABLE -> "update-available";
            case STAGED -> "update-staged";
            case FAILED -> "update-failed";
        };
        messages.send(sender, key, placeholders);
    }

    private UpdaterSnapshot failedSnapshot(String detail) {
        return new UpdaterSnapshot(
                UpdaterState.FAILED,
                plugin.getDescription().getVersion(),
                snapshot.latestVersion(),
                detail,
                Instant.now()
        );
    }

    private String safeDetail(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        message = message.replace('\r', ' ').replace('\n', ' ').trim();
        return message.length() <= 240 ? message : message.substring(0, 240);
    }

    private void stopAutomaticTask() {
        if (automaticTask != null) {
            automaticTask.cancel();
            automaticTask = null;
        }
    }
}
