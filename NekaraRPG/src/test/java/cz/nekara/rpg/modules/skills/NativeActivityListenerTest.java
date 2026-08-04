package cz.nekara.rpg.modules.skills;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NativeActivityListenerTest {
    @Test
    void categorizesNaturalForagingSourcesForHospodarstvi() {
        assertEquals("wild_flower", NativeActivityListener.wildForageSource("CORNFLOWER"));
        assertEquals("wild_mushroom", NativeActivityListener.wildForageSource("BROWN_MUSHROOM"));
        assertEquals("grass_bundle", NativeActivityListener.wildForageSource("SHORT_GRASS"));
        assertNull(NativeActivityListener.wildForageSource("OAK_LOG"));
    }
}
