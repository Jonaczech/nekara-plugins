package cz.nekara.rpg.modules.mounts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MountRecallPolicyTest {
    @Test
    void teleportsOnlyWhenTheHorseIsAtLeastThreeChunksAway() {
        assertFalse(MountRecallPolicy.shouldTeleport(10, 10, 12, 12, 3));
        assertTrue(MountRecallPolicy.shouldTeleport(10, 10, 13, 10, 3));
        assertTrue(MountRecallPolicy.shouldTeleport(10, 10, 8, 13, 3));
    }
}
