package cz.nekara.rpg.configuration;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceYamlTest {
    @Test
    void bundledYamlResourcesAreValid() throws Exception {
        for (String resource : new String[]{"config.yml", "messages.yml", "plugin.yml"}) {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
            assertNotNull(stream, resource + " is missing from the test classpath");
            try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                new Yaml().load(reader);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void bundledCampfireDefaultsExposeExternalSeatsAndVisualFeedback() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("config.yml");
        assertNotNull(stream);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, Object> root = new Yaml().load(reader);
            Map<String, Object> updater = (Map<String, Object>) root.get("updater");
            Map<String, Object> modules = (Map<String, Object>) root.get("modules");
            Map<String, Object> echoVein = (Map<String, Object>) root.get("echo-vein");
            Map<String, Object> oreReveal = (Map<String, Object>) echoVein.get("ore-reveal");
            Map<String, Object> sitting = (Map<String, Object>) root.get("sitting");
            Map<String, Object> campfire = (Map<String, Object>) root.get("campfire");
            Map<String, Object> sounds = (Map<String, Object>) root.get("sounds");
            Map<String, Object> restedSound = (Map<String, Object>) sounds.get("campfire-rested");
            Map<String, Object> campfireRested = (Map<String, Object>) campfire.get("rested");
            Map<String, Object> haste = (Map<String, Object>) campfireRested.get("haste");
            Map<String, Object> valhallaExperience =
                    (Map<String, Object>) campfireRested.get("valhalla-experience");
            Map<String, Object> camping = (Map<String, Object>) campfire.get("camping");
            Map<String, Object> spawnProtection =
                    (Map<String, Object>) camping.get("spawn-protection");
            Map<String, Object> visuals = (Map<String, Object>) campfire.get("visuals");
            Map<String, Object> restingParticles =
                    (Map<String, Object>) visuals.get("resting-particles");
            Map<String, Object> rested = (Map<String, Object>) visuals.get("rested");

            assertEquals(0.20, ((Number) sitting.get("seat-y-offset")).doubleValue(), 0.0001);
            assertTrue((Boolean) updater.get("enabled"));
            assertNotNull(modules.get("mining"));
            assertFalse(modules.containsKey("echo-vein"));
            assertFalse(echoVein.containsKey("minimum-mining-level"));
            assertEquals(0.05, ((Number) echoVein.get("trigger-chance")).doubleValue(), 0.0001);
            assertEquals(0.50, ((Number) echoVein.get("chain-chance")).doubleValue(), 0.0001);
            assertFalse(echoVein.containsKey("cooldown-seconds"));
            assertEquals(0.25,
                    ((Number) echoVein.get("experience-bonus-multiplier")).doubleValue(), 0.0001);
            assertTrue((Boolean) echoVein.get("bonus-drop-enabled"));
            assertEquals(0.25, ((Number) oreReveal.get("chance")).doubleValue(), 0.0001);
            assertTrue((Boolean) updater.get("automatic-checks"));
            assertTrue((Boolean) updater.get("auto-download"));
            assertTrue((Boolean) updater.get("notify-admins"));
            assertEquals(6, ((Number) updater.get("check-interval-hours")).intValue());
            assertEquals(16, ((Number) updater.get("maximum-jar-size-megabytes")).intValue());
            assertTrue((Boolean) sitting.get("detect-external-seats"));
            assertEquals(List.of("ARMOR_STAND"), sitting.get("external-seat-entity-types"));
            assertEquals(5.0, ((Number) campfire.get("radius")).doubleValue(), 0.0001);
            assertEquals(300, ((Number) campfireRested.get("duration-seconds")).intValue());
            assertEquals(0.5, ((Number) campfireRested.get("hunger-loss-multiplier")).doubleValue(), 0.0001);
            assertTrue((Boolean) valhallaExperience.get("enabled"));
            assertEquals(1.10, ((Number) valhallaExperience.get("multiplier")).doubleValue(), 0.0001);
            assertTrue((Boolean) haste.get("enabled"));
            assertEquals(0, ((Number) haste.get("amplifier")).intValue());
            assertTrue((Boolean) haste.get("icon"));
            assertEquals(5.0, ((Number) camping.get("feature-radius")).doubleValue(), 0.0001);
            assertEquals(60, ((Number) camping.get("duration-per-feature-seconds")).intValue());
            assertEquals(List.of("CRAFTING_TABLE", "BED", "SMOKER", "BARREL",
                    "WATER_CAULDRON", "CARTOGRAPHY_TABLE", "GRINDSTONE"),
                    camping.get("features"));
            assertTrue((Boolean) spawnProtection.get("enabled"));
            assertTrue((Boolean) spawnProtection.get("natural-only"));
            assertEquals(24.0, ((Number) spawnProtection.get("radius")).doubleValue(), 0.0001);
            assertEquals("NekaraHostile", spawnProtection.get("mythic-hostile-faction"));
            assertTrue((Boolean) restingParticles.get("enabled"));
            assertEquals("ACTION_BAR", rested.get("indicator"));
            assertTrue((Boolean) restedSound.get("enabled"));
            assertEquals("minecraft:block.amethyst_block.chime", restedSound.get("sound"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void bundledMessagesContainTheRestedTimerFallback() throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("messages.yml");
        assertNotNull(stream);
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Map<String, Object> messages = new Yaml().load(reader);
            assertNotNull(messages.get("update-check-started"));
            assertNotNull(messages.get("update-current"));
            assertNotNull(messages.get("update-staged"));
            assertNotNull(messages.get("update-failed"));
            assertFalse(messages.containsKey("echo-vein-found"));
            assertNotNull(messages.get("echo-vein-test-unavailable"));
            assertEquals(
                    "<green>Odpočatý</green> <dark_gray>|</dark_gray> <white>%remaining_text%</white>",
                    messages.get("campfire-rested-timer")
            );
        }
    }
}
