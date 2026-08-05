package cz.nekara.rpg.configuration;

import java.util.Comparator;
import java.util.List;

public record FishingDifficultyConfig(
        boolean enabled,
        int maxLevelRequiredHits,
        int maxLevelMaxMisses,
        List<FishingDifficultyTier> tiers
) {
    public FishingDifficultyConfig {
        tiers = tiers.stream()
                .sorted(Comparator.comparingInt(FishingDifficultyTier::minLevel))
                .toList();
    }

    public FishingDifficultyTier tierForLevel(int level) {
        if (tiers.isEmpty()) {
            return new FishingDifficultyTier("default", 0, 0, 3, 5, 1);
        }
        for (FishingDifficultyTier tier : tiers) {
            if (tier.appliesTo(level)) {
                return tier;
            }
        }
        return tiers.getLast();
    }
}
