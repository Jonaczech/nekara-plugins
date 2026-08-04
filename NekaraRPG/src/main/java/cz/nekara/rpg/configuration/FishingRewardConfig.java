package cz.nekara.rpg.configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Extra treasure only; it never substitutes a real vanilla fishing catch. */
public record FishingRewardConfig(boolean treasureEnabled, Map<Material, Integer> treasureWeights) {
    public FishingRewardConfig {
        Objects.requireNonNull(treasureWeights, "treasureWeights");
        EnumMap<Material, Integer> normalized = new EnumMap<>(Material.class);
        treasureWeights.forEach((material, weight) -> {
            if (material == null || weight == null || !material.isItem() || weight < 1) {
                throw new IllegalArgumentException("Fishing treasure entries must be positive item weights");
            }
            normalized.put(material, weight);
        });
        treasureWeights = Map.copyOf(normalized);
    }
}
