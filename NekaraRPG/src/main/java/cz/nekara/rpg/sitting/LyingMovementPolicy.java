package cz.nekara.rpg.sitting;

import org.bukkit.entity.Pose;

public final class LyingMovementPolicy {
    private LyingMovementPolicy() {
    }

    public static boolean changesPosition(
            double fromX,
            double fromY,
            double fromZ,
            double toX,
            double toY,
            double toZ
    ) {
        return Double.compare(fromX, toX) != 0
                || Double.compare(fromY, toY) != 0
                || Double.compare(fromZ, toZ) != 0;
    }

    public static boolean shouldRefreshPose(Pose pose) {
        return pose != Pose.SLEEPING;
    }
}
