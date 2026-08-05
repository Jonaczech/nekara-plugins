package cz.nekara.rpg.skills.luck;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LuckChanceResolverTest {
    @Test
    void addsOnlyTheConfiguredBonusForEachLuckPoint() {
        assertEquals(0.06, LuckChanceResolver.rareLootChance(0.02, 2.0, 2, 0.02), 0.000001);
    }

    @Test
    void clampsLuckAndTheFinalChance() {
        assertEquals(0.08, LuckChanceResolver.rareLootChance(0.04, 100.0, 2, 0.02), 0.000001);
        assertEquals(1.0, LuckChanceResolver.rareLootChance(0.99, 2.0, 2, 0.02), 0.000001);
    }
}
