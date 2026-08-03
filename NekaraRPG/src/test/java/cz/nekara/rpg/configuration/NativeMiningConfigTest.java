package cz.nekara.rpg.configuration;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeMiningConfigTest {
    @Test
    void bundledTableRewardsOresMoreThanHostStone() {
        var values = NativeMiningConfig.defaultExperienceByMaterial();

        assertTrue(values.get(Material.DIAMOND_ORE) > values.get(Material.STONE));
        assertTrue(values.get(Material.ANCIENT_DEBRIS) > values.get(Material.DIAMOND_ORE));
        assertEquals(23, values.size());
    }
}
