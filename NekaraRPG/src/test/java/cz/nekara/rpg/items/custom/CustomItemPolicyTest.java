package cz.nekara.rpg.items.custom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CustomItemPolicyTest {
    @Test
    void normalizesStableIdWithoutBukkitBootstrap() {
        assertEquals("ocelovy_mec", CustomItemDefinition.normalizeId("  OCELOVY_MEC  "));
    }

    @Test
    void rejectsIdsThatCannotBeStoredAsOneStableYamlKey() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomItemDefinition.normalizeId("invalid.id"));
        assertThrows(IllegalArgumentException.class,
                () -> CustomItemDefinition.normalizeId("mezera v id"));
        assertThrows(IllegalArgumentException.class,
                () -> CustomItemDefinition.normalizeId("a"));
    }

    @Test
    void optionalStatsCanBeEnabledAndDisabled() {
        CustomItemStats stats = CustomItemStats.EMPTY.with(CustomItemStat.ATTACK_DAMAGE, 8.5);

        assertEquals(8.5, stats.attackDamage());
        assertNull(stats.with(CustomItemStat.ATTACK_DAMAGE, null).attackDamage());
    }

    @Test
    void rejectsUnsafeStatRanges() {
        assertThrows(IllegalArgumentException.class,
                () -> CustomItemStats.EMPTY.with(CustomItemStat.ARMOR, 101.0));
        assertThrows(IllegalArgumentException.class,
                () -> CustomItemStats.EMPTY.with(CustomItemStat.ATTACK_SPEED, Double.NaN));
    }
}
