package cz.nekara.rpg.crawling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrawlingPolicyTest {
    @Test
    void onlyStartsFromAStableGroundedState() {
        assertTrue(CrawlingPolicy.canStart(true, false, false, false, false, false, false));
        assertFalse(CrawlingPolicy.canStart(false, false, false, false, false, false, false));
        assertFalse(CrawlingPolicy.canStart(true, false, false, true, false, false, false));
        assertFalse(CrawlingPolicy.canStart(true, false, false, false, true, false, false));
        assertFalse(CrawlingPolicy.canStart(true, false, false, false, false, true, false));
    }
}
