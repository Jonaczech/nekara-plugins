package cz.nekara.rpg.configuration;

import java.util.Comparator;
import java.util.List;

public record ValhallaFishingConfig(
        boolean enabled,
        int maxLevelRequiredHits,
        int maxLevelMaxMisses,
        List<ValhallaFishingTier> tiers
) {
    public ValhallaFishingConfig {
        tiers = tiers.stream()
                .sorted(Comparator.comparingInt(ValhallaFishingTier::minLevel))
                .toList();
    }

    public ValhallaFishingTier tierForLevel(int level) {
        if (tiers.isEmpty()) {
            return new ValhallaFishingTier("default", 0, 0, 3, 5, 1);
        }
        for (ValhallaFishingTier tier : tiers) {
            if (tier.appliesTo(level)) {
                return tier;
            }
        }
        return level < tiers.get(0).minLevel() ? tiers.get(0) : tiers.get(tiers.size() - 1);
    }
}
