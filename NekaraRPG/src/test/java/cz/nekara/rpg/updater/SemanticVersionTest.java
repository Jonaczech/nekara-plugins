package cz.nekara.rpg.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticVersionTest {
    @Test
    void parsesPluginVersionsAndGitHubTags() {
        assertEquals(new SemanticVersion(1, 2, 0), SemanticVersion.parseStable("1.2.0"));
        assertEquals(new SemanticVersion(1, 2, 0), SemanticVersion.parseStable("v1.2.0"));
    }

    @Test
    void comparesMajorMinorAndPatchComponents() {
        assertTrue(SemanticVersion.parseStable("1.2.1")
                .compareTo(SemanticVersion.parseStable("1.2.0")) > 0);
        assertTrue(SemanticVersion.parseStable("1.10.0")
                .compareTo(SemanticVersion.parseStable("1.9.9")) > 0);
        assertTrue(SemanticVersion.parseStable("2.0.0")
                .compareTo(SemanticVersion.parseStable("1.99.99")) > 0);
    }

    @Test
    void rejectsPrereleaseAndMalformedVersions() {
        assertThrows(IllegalArgumentException.class,
                () -> SemanticVersion.parseStable("1.2.0-SNAPSHOT"));
        assertThrows(IllegalArgumentException.class,
                () -> SemanticVersion.parseStable("1.2"));
        assertThrows(IllegalArgumentException.class,
                () -> SemanticVersion.parseStable("01.2.0"));
    }
}
