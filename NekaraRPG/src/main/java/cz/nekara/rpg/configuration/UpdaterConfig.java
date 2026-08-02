package cz.nekara.rpg.configuration;

public record UpdaterConfig(
        boolean enabled,
        boolean automaticChecks,
        boolean autoDownload,
        boolean notifyAdmins,
        int startupDelaySeconds,
        int checkIntervalHours,
        int requestTimeoutSeconds,
        int maximumJarSizeMegabytes
) {
}
