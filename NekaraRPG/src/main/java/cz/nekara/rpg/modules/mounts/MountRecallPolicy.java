package cz.nekara.rpg.modules.mounts;

final class MountRecallPolicy {
    private MountRecallPolicy() {
    }

    static boolean shouldTeleport(int mountChunkX, int mountChunkZ, int playerChunkX, int playerChunkZ,
                                  int minimumChunkDistance) {
        if (minimumChunkDistance < 1) {
            throw new IllegalArgumentException("Minimum chunk distance must be positive");
        }
        return Math.max(Math.abs(mountChunkX - playerChunkX), Math.abs(mountChunkZ - playerChunkZ))
            >= minimumChunkDistance;
    }
}
