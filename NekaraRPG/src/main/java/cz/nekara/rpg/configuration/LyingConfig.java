package cz.nekara.rpg.configuration;

public record LyingConfig(
        boolean enabled,
        boolean mannequinVisualEnabled,
        boolean wakeOnDamage,
        boolean skipNightWhenAlone,
        int fallAsleepSeconds
) {
}
