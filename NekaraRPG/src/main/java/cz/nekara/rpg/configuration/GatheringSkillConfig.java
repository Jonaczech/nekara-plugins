package cz.nekara.rpg.configuration;

import java.util.Map;
import org.bukkit.Material;

public interface GatheringSkillConfig {
    boolean experienceEnabled();

    int chunkWindowSeconds();

    int chunkSoftLimit();

    int chunkHardLimit();

    double farmFloorMultiplier();

    boolean finalDropMultiplierEnabled();

    Map<Material, Long> experienceByMaterial();

    default long experience(Material material) {
        return experienceByMaterial().getOrDefault(material, 0L);
    }
}
