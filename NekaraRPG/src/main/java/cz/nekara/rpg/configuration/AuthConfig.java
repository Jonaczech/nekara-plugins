package cz.nekara.rpg.configuration;

public record AuthConfig(
        String storageFile,
        int passwordMinimumLength,
        int passwordMaximumLength,
        int passwordIterations,
        int maximumAttempts,
        int lockoutSeconds,
        int authenticationTimeoutSeconds,
        boolean exactNameCase,
        boolean openMenuOnJoin
) {
}
