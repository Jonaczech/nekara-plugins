package cz.nekara.rpg.modules.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlacedBlockPositionCodecTest {
    @Test
    void distinguishesLocalCoordinatesAndNegativeWorldHeight() {
        int first = PlacedBlockPositionCodec.encode(16, -64, 16, -64);
        int nextX = PlacedBlockPositionCodec.encode(17, -64, 16, -64);
        int nextY = PlacedBlockPositionCodec.encode(16, -63, 16, -64);
        int nextZ = PlacedBlockPositionCodec.encode(16, -64, 17, -64);

        assertNotEquals(first, nextX);
        assertNotEquals(first, nextY);
        assertNotEquals(first, nextZ);
    }

    @Test
    void rejectsCoordinatesBelowTheWorldFloor() {
        assertThrows(IllegalArgumentException.class,
            () -> PlacedBlockPositionCodec.encode(0, -65, 0, -64));
    }
}
