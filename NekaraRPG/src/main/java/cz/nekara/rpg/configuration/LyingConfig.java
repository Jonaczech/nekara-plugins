package cz.nekara.rpg.configuration;

public record LyingConfig(
        boolean enabled,
        boolean wakeOnDamage,
        boolean skipNightWhenAlone,
        int fallAsleepSeconds
) {
}
