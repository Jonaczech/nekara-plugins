package cz.nekara.rpg.configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

/** Extra treasure only; it never substitutes a real vanilla fishing catch. */
public record FishingRewardConfig(
    boolean treasureEnabled,
    Map<Material, Integer> treasureWeights,
    int blankRuneWeight,
    double blankRuneChestChance
) {
    public FishingRewardConfig {
        Objects.requireNonNull(treasureWeights, "treasureWeights");
        if (blankRuneWeight < 0) throw new IllegalArgumentException("Blank rune weight cannot be negative");
        if (blankRuneChestChance < 0.0 || blankRuneChestChance > 1.0) {
            throw new IllegalArgumentException("Blank rune chest chance must be within 0 and 1");
        }
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