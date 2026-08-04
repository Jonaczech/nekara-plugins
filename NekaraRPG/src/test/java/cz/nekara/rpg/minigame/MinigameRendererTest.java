package cz.nekara.rpg.minigame;

import cz.nekara.rpg.configuration.DisplayMode;
import cz.nekara.rpg.configuration.IndicatorDirection;
import cz.nekara.rpg.configuration.MinigameConfig;
import java.util.Random;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinigameRendererTest {
    @Test
    void rendersTheFishingTrackUsingTheSharedRpgSegmentLanguage() {
        MinigameEngine engine = new MinigameEngine(new MinigameConfig(
            true, DisplayMode.ACTION_BAR, 8, 1, 2, 6, 2, 1, 100, 20,
            true, false, IndicatorDirection.RIGHT, 150), new Random(1));

        String rendered = PlainTextComponentSerializer.plainText().serialize(new MinigameRenderer().render(engine));
        String track = rendered.substring(0, rendered.indexOf("  "));

        assertEquals(8, track.length());
        assertTrue(track.contains("⚓"));
        assertTrue(track.contains("▰"));
        assertTrue(track.contains("▱"));
        assertTrue(rendered.endsWith("5.0s"));
    }
}
