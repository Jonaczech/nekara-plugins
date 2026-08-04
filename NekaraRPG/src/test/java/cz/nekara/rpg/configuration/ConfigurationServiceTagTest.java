package cz.nekara.rpg.configuration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigurationServiceTagTest {
    @Test
    void copiesAnImmutableConfiguredBlockTableBeforeProcessingTags() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("skills.digging.experience-sources.tags.UNKNOWN_TAG", 2);
        AtomicInteger warnings = new AtomicInteger();

        Map<Material, Long> experience = ConfigurationService.applyMaterialTags(
            configuration,
            "skills.digging.experience-sources.tags",
            Map.of(Material.DIRT, 7L),
            ignored -> warnings.incrementAndGet()
        );

        assertEquals(7L, experience.get(Material.DIRT));
        assertEquals(1, warnings.get());
    }
}
