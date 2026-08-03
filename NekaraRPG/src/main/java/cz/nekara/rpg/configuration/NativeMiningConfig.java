package cz.nekara.rpg.configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Material;

public record NativeMiningConfig(
    boolean experienceEnabled,
    int chunkWindowSeconds,
    int chunkSoftLimit,
    int chunkHardLimit,
    double farmFloorMultiplier,
    boolean finalDropMultiplierEnabled,
    Map<Material, Long> experienceByMaterial
) implements GatheringSkillConfig {
    public NativeMiningConfig {
        if (chunkWindowSeconds < 1) {
            throw new IllegalArgumentException("Chunk activity window must be positive");
        }
        if (chunkSoftLimit < 0 || chunkHardLimit <= chunkSoftLimit) {
            throw new IllegalArgumentException("Chunk hard limit must be greater than its soft limit");
        }
        if (!Double.isFinite(farmFloorMultiplier)
            || farmFloorMultiplier <= 0
            || farmFloorMultiplier > 1) {
            throw new IllegalArgumentException("Farm floor multiplier must be between 0 and 1");
        }
        Objects.requireNonNull(experienceByMaterial, "experienceByMaterial");
        EnumMap<Material, Long> normalized = new EnumMap<>(Material.class);
        for (Map.Entry<Material, Long> entry : experienceByMaterial.entrySet()) {
            Material material = Objects.requireNonNull(entry.getKey(), "experience material");
            Long experience = Objects.requireNonNull(entry.getValue(), "experience value");
            if (!material.isBlock() || experience < 1) {
                throw new IllegalArgumentException("Mining experience entries must be positive block values");
            }
            normalized.put(material, experience);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Mining experience table cannot be empty");
        }
        experienceByMaterial = Map.copyOf(normalized);
    }

    public static Map<Material, Long> defaultExperienceByMaterial() {
        EnumMap<Material, Long> values = new EnumMap<>(Material.class);
        put(values, 2, Material.STONE, Material.DEEPSLATE, Material.NETHERRACK, Material.END_STONE);
        put(values, 5, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE);
        put(values, 8, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.NETHER_QUARTZ_ORE);
        put(values, 12, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.NETHER_GOLD_ORE, Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE,
            Material.DEEPSLATE_LAPIS_ORE);
        put(values, 20, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE);
        put(values, 30, Material.ANCIENT_DEBRIS);
        return Map.copyOf(values);
    }

    private static void put(EnumMap<Material, Long> values, long experience, Material... materials) {
        for (Material material : materials) {
            values.put(material, experience);
        }
    }
}
