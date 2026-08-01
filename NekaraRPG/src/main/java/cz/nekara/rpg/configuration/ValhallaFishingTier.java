package cz.nekara.rpg.configuration;

public record ValhallaFishingTier(
        String name,
        int minLevel,
        int maxLevel,
        int requiredHitsMin,
        int requiredHitsMax,
        int maxMisses
) {
    public boolean appliesTo(int level) {
        return level >= minLevel && (maxLevel <= 0 || level <= maxLevel);
    }
}
