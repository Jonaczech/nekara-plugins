package cz.nekara.rpg.sitting;

import org.junit.jupiter.api.Test;
import org.bukkit.entity.Pose;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LyingMovementPolicyTest {
    @Test
    void rotationWithoutPositionChangeIsAllowed() {
        assertFalse(LyingMovementPolicy.changesPosition(1.0, 64.0, 2.0, 1.0, 64.0, 2.0));
    }

    @Test
    void evenSmallServerPositionCorrectionIsBlockedWithoutEndingLying() {
        assertTrue(LyingMovementPolicy.changesPosition(1.0, 64.0, 2.0, 1.0, 63.999, 2.0));
    }

    @Test
    void standingPoseIsRefreshedAfterLying() {
        assertTrue(LyingMovementPolicy.shouldRefreshPose(Pose.STANDING));
        assertFalse(LyingMovementPolicy.shouldRefreshPose(Pose.SLEEPING));
    }
}
