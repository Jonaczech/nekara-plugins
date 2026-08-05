package cz.nekara.rpg.configuration;

/** Native Nekara Skills XP bonus granted while the player is Rested. */
public record RestedExperienceConfig(
        boolean enabled,
        double multiplier
) {
}
