package cz.nekara.rpg.campfire;

import java.util.Set;

public record CampFeatureSnapshot(Set<CampFeature> features) {
    public CampFeatureSnapshot {
        features = Set.copyOf(features);
    }

    public boolean has(CampFeature feature) {
        return features.contains(feature);
    }

    public int count() {
        return features.size();
    }
}
