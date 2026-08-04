package cz.nekara.rpg.configuration;

public record LyingConfig(
        boolean enabled,
        boolean mannequinVisualEnabled,
        double mannequinYawOffsetDegrees,
        double mannequinForwardOffset,
        double mannequinSideOffset,
        double mannequinVerticalOffset,
        boolean wakeOnDamage,
        boolean skipNightWhenAlone,
        int fallAsleepSeconds
) {
}
