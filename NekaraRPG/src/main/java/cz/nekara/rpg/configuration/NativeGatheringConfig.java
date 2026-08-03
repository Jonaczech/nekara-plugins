package cz.nekara.rpg.configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

public record NativeGatheringConfig(
    boolean experienceEnabled,
    int chunkWindowSeconds,
    int chunkSoftLimit,
    int chunkHardLimit,
    double farmFloorMultiplier,
    boolean finalDropMultiplierEnabled,
    boolean rareDropsEnabled,
    Map<Material, Long> experienceByMaterial,
    Map<Material, Integer> rareDropWeights
) implements GatheringSkillConfig {
    public NativeGatheringConfig {
        if (chunkWindowSeconds < 1) {
            throw new IllegalArgumentException("Chunk activity window must be positive");
        }
        if (chunkSoftLimit < 0 || chunkHardLimit <= chunkSoftLimit) {
            throw new IllegalArgumentException("Chunk hard limit must be greater than its soft limit");
        }
        if (!Double.isFinite(farmFloorMultiplier)
            || farmFloorMultiplier <= 0 || farmFloorMultiplier > 1) {
            throw new IllegalArgumentException("Farm floor multiplier must be between 0 and 1");
        }
        experienceByMaterial = normalizeExperience(experienceByMaterial);
        rareDropWeights = normalizeWeights(rareDropWeights);
    }

    private static Map<Material, Long> normalizeExperience(Map<Material, Long> source) {
        Objects.requireNonNull(source, "experienceByMaterial");
        EnumMap<Material, Long> values = new EnumMap<>(Material.class);
        source.forEach((material, experience) -> {
            if (material == null || experience == null || !material.isBlock() || experience < 1) {
                throw new IllegalArgumentException("Gathering experience entries must be positive block values");
            }
            values.put(material, experience);
        });
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Gathering experience table cannot be empty");
        }
        return Map.copyOf(values);
    }

    private static Map<Material, Integer> normalizeWeights(Map<Material, Integer> source) {
        Objects.requireNonNull(source, "rareDropWeights");
        EnumMap<Material, Integer> values = new EnumMap<>(Material.class);
        source.forEach((material, weight) -> {
            if (material == null || weight == null || !material.isItem() || weight < 1) {
                throw new IllegalArgumentException("Rare drop entries must be positive item weights");
            }
            values.put(material, weight);
        });
        return Map.copyOf(values);
    }

    public static Map<Material, Long> defaultWoodcuttingExperience() {
        EnumMap<Material, Long> values = new EnumMap<>(Material.class);
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_LOG") || name.endsWith("_WOOD")
                || name.endsWith("_STEM") || name.endsWith("_HYPHAE")) {
                values.put(material, name.contains("STRIPPED") ? 3L : 4L);
            }
        }
        values.put(Material.BAMBOO_BLOCK, 2L);
        values.put(Material.STRIPPED_BAMBOO_BLOCK, 2L);
        return Map.copyOf(values);
    }

    public static Map<Material, Long> defaultDiggingExperience() {
        EnumMap<Material, Long> values = new EnumMap<>(Material.class);
        put(values, 2, Material.DIRT, Material.GRASS_BLOCK, Material.COARSE_DIRT,
            Material.ROOTED_DIRT, Material.PODZOL, Material.MYCELIUM,
            Material.SAND, Material.RED_SAND, Material.GRAVEL,
            Material.SOUL_SAND, Material.SOUL_SOIL, Material.SNOW_BLOCK);
        put(values, 3, Material.MUD, Material.CLAY);
        return Map.copyOf(values);
    }

    public static Map<Material, Integer> defaultWoodcuttingRareDrops() {
        return Map.of(Material.STICK, 12, Material.APPLE, 4, Material.HONEYCOMB, 1);
    }

    public static Map<Material, Integer> defaultDiggingRareDrops() {
        return Map.of(Material.IRON_NUGGET, 10, Material.GOLD_NUGGET, 4,
            Material.AMETHYST_SHARD, 2, Material.PRISMARINE_SHARD, 1);
    }

    private static void put(EnumMap<Material, Long> values, long experience, Material... materials) {
        for (Material material : materials) {
            values.put(material, experience);
        }
    }
}
