package cz.nekara.rpg.modules.runes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RuneSocketPolicyTest {
    @Test
    void qualityControlsSocketCapacity() {
        assertEquals(1, RuneSocketPolicy.capacityForQuality(1));
        assertEquals(1, RuneSocketPolicy.capacityForQuality(2));
        assertEquals(2, RuneSocketPolicy.capacityForQuality(3));
        assertEquals(2, RuneSocketPolicy.capacityForQuality(4));
        assertEquals(3, RuneSocketPolicy.capacityForQuality(5));
        assertEquals(0, RuneSocketPolicy.capacityForQuality(0));
    }

    @Test
    void insightTiersUseApprovedExperienceBonuses() {
        assertEquals(0.01, RuneSocketPolicy.experienceBonus(RuneTier.I));
        assertEquals(0.03, RuneSocketPolicy.experienceBonus(RuneTier.II));
        assertEquals(0.05, RuneSocketPolicy.experienceBonus(RuneTier.III));
    }
}
