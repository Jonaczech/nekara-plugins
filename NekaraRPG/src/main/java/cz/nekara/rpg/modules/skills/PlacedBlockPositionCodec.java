package cz.nekara.rpg.modules.skills;

final class PlacedBlockPositionCodec {
    private PlacedBlockPositionCodec() {
    }

    static int encode(int blockX, int blockY, int blockZ, int minimumHeight) {
        int relativeY = blockY - minimumHeight;
        if (relativeY < 0 || relativeY > 0x00FF_FFFF) {
            throw new IllegalArgumentException("Block height is outside the persistent origin format");
        }
        return (relativeY << 8) | ((blockX & 15) << 4) | (blockZ & 15);
    }
}
