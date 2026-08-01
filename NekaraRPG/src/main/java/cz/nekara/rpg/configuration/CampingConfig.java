package cz.nekara.rpg.configuration;

import cz.nekara.rpg.campfire.CampFeature;

import java.util.Set;

public record CampingConfig(
        double featureRadius,
        int durationPerFeatureSeconds,
        Set<CampFeature> enabledFeatures,
        boolean spawnProtectionEnabled,
        double spawnProtectionRadius,
        boolean spawnProtectionNaturalOnly,
        String mythicHostileFaction
) {
    public CampingConfig {
        enabledFeatures = Set.copyOf(enabledFeatures);
    }
}
