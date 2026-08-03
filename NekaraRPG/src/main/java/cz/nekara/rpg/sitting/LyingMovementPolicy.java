package cz.nekara.rpg.sitting;

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
}
