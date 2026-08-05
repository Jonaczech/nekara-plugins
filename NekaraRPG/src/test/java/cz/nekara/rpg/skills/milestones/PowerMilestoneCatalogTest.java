package cz.nekara.rpg.skills.milestones;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class PowerMilestoneCatalogTest {
    private final PowerMilestoneCatalog catalog = new PowerMilestoneCatalog(
        Arrays.stream(PowerMilestoneId.values()).map(PowerMilestoneId::milestone).toList());

    @Test
    void campfireUnlocksAtOneAndMountAtTwentyFive() {
        assertEquals(1, catalog.unlockedAt(1).size());
        assertEquals(1, catalog.unlockedAt(24).size());
        assertEquals(2, catalog.unlockedAt(25).size());
    }
}
