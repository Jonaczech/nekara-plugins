package cz.nekara.rpg.modules.sitting;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyingVisualTransformTest {
    private final LyingVisualTransform transform =
        new LyingVisualTransform(0.0, -0.9, 0.0, 0.0);

    @Test
    void alignsSouthFacingPlayerAndCentresTheSleepingBody() {
        Location result = transform.apply(new Location(null, 10.0, 64.0, 20.0, 0.0F, 35.0F));

        assertEquals(10.0, result.getX(), 0.0001);
        assertEquals(64.0, result.getY(), 0.0001);
        assertEquals(19.1, result.getZ(), 0.0001);
        assertEquals(0.0F, result.getYaw(), 0.0001);
        assertEquals(0.0F, result.getPitch(), 0.0001);
    }

    @Test
    void rotatesOffsetsWithThePlayersFacingDirection() {
        Location result = transform.apply(new Location(null, 10.0, 64.0, 20.0, 90.0F, 0.0F));

        assertEquals(10.9, result.getX(), 0.0001);
        assertEquals(20.0, result.getZ(), 0.0001);
        assertEquals(90.0F, result.getYaw(), 0.0001);
    }

    @Test
    void normalizesConfiguredYaw() {
        assertEquals(100.0F, LyingVisualTransform.normalizeYaw(-260.0F), 0.0001);
        assertEquals(-180.0F, LyingVisualTransform.normalizeYaw(180.0F), 0.0001);
    }
}
