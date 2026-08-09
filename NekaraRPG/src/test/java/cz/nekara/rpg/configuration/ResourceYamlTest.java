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
        for (String resource : new String[]{"config.yml", "messages.yml", "plugin.yml",
                "auth/config.yml", "fishing/config.yml", "campfire/config.yml",
                "mining/config.yml", "mounts/config.yml", "dragons/config.yml", "skills/config.yml",
                "skills/martial_arts/config.yml", "skills/martial_arts/messages.yml",
                "skills/trading/config.yml", "skills/trading/messages.yml",
                "skills/smithing/config.yml", "skills/smithing/messages.yml",
                "skills/runotepectvi/config.yml", "skills/runotepectvi/messages.yml",
                "skills/alchemy/config.yml", "skills/alchemy/messages.yml",
                "skills/tezba/config.yml", "skills/tezba/messages.yml",
                "skills/lesnictvi/config.yml", "skills/lesnictvi/messages.yml",
                "skills/kopani/config.yml", "skills/kopani/messages.yml",
                "skills/statkarstvi/config.yml", "skills/statkarstvi/messages.yml",
                "skills/rybareni/config.yml", "skills/rybareni/messages.yml",
                "skills/lehke_zbrane/config.yml", "skills/lehke_zbrane/messages.yml",
                "skills/heavy_weapons/config.yml", "skills/heavy_weapons/messages.yml",
                "skills/archery/config.yml", "skills/archery/messages.yml",
                "skills/light_armor/config.yml", "skills/light_armor/messages.yml",
                "skills/heavy_armor/config.yml", "skills/heavy_armor/messages.yml",
                "skills/lesnictvi/loot-tables.yml",
                "skills/kopani/loot-tables.yml", "skills/rybareni/loot-tables.yml"}) {
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
        Map<String, Object> root = loadYaml("config.yml");
        Map<String, Object> auth = loadYaml("auth/config.yml");
        Map<String, Object> campfire = loadYaml("campfire/config.yml");
        Map<String, Object> sitting = (Map<String, Object>) campfire.get("sitting");
        Map<String, Object> mining = loadYaml("mining/config.yml");
        Map<String, Object> fishing = loadYaml("fishing/config.yml");
        Map<String, Object> mountsConfig = loadYaml("mounts/config.yml");
        Map<String, Object> dragonsConfig = loadYaml("dragons/config.yml");
            Map<String, Object> skillsConfig = loadYaml("skills/config.yml");
            Map<String, Object> fishingLoot = loadYaml("skills/rybareni/loot-tables.yml");
        Map<String, Object> miningSkillConfig = loadYaml("skills/tezba/config.yml");
        Map<String, Object> woodcuttingSkillConfig = loadYaml("skills/lesnictvi/config.yml");
        Map<String, Object> diggingSkillConfig = loadYaml("skills/kopani/config.yml");

            Map<String, Object> updater = (Map<String, Object>) root.get("updater");
            Map<String, Object> modules = (Map<String, Object>) root.get("modules");
            Map<String, Object> authStorage = (Map<String, Object>) auth.get("storage");
            Map<String, Object> authPassword = (Map<String, Object>) auth.get("password");
            Map<String, Object> authLogin = (Map<String, Object>) auth.get("login");
            Map<String, Object> authSession = (Map<String, Object>) auth.get("session");
            Map<String, Object> authCommands = (Map<String, Object>) auth.get("commands");
            Map<String, Object> echoVein = (Map<String, Object>) mining.get("echo-vein");
            Map<String, Object> oreReveal = (Map<String, Object>) echoVein.get("ore-reveal");
            Map<String, Object> campfireSounds = (Map<String, Object>) campfire.get("sounds");
            Map<String, Object> miningSounds = (Map<String, Object>) mining.get("sounds");
            Map<String, Object> restedSound = (Map<String, Object>) campfireSounds.get("campfire-rested");
            Map<String, Object> veinSound = (Map<String, Object>) miningSounds.get("echo-vein-pulse");
            Map<String, Object> oreSound = (Map<String, Object>) miningSounds.get("echo-vein-ore-reveal");
            Map<String, Object> campfireRested = (Map<String, Object>) campfire.get("rested");
            Map<String, Object> haste = (Map<String, Object>) campfireRested.get("haste");
            Map<String, Object> skillsExperience =
                    (Map<String, Object>) campfireRested.get("skills-experience");
            Map<String, Object> camping = (Map<String, Object>) campfire.get("camping");
            Map<String, Object> spawnProtection =
                    (Map<String, Object>) camping.get("spawn-protection");
            Map<String, Object> visuals = (Map<String, Object>) campfire.get("visuals");
            Map<String, Object> restingParticles =
                    (Map<String, Object>) visuals.get("resting-particles");
            Map<String, Object> rested = (Map<String, Object>) visuals.get("rested");
            Map<String, Object> mountStorage = (Map<String, Object>) mountsConfig.get("storage");
            Map<String, Object> mountDeath = (Map<String, Object>) mountsConfig.get("death");
            Map<String, Object> mountCombat = (Map<String, Object>) mountsConfig.get("combat");
            Map<String, Object> mountSummoning = (Map<String, Object>) mountsConfig.get("summoning");
            Map<String, Object> mountPersistence = (Map<String, Object>) mountsConfig.get("persistence");
            Map<String, Object> mountNaming = (Map<String, Object>) mountsConfig.get("naming");
            Map<String, Object> mountWhistle = (Map<String, Object>) mountsConfig.get("whistle");
            Map<String, Object> dragonSummoning = (Map<String, Object>) dragonsConfig.get("summoning");
            Map<String, Object> dragonFlight = (Map<String, Object>) dragonsConfig.get("flight");
            Map<String, Object> skillStorage = (Map<String, Object>) skillsConfig.get("storage");
            Map<String, Object> skillProgression = (Map<String, Object>) skillsConfig.get("progression");
            Map<String, Object> skillWoodcutting = woodcuttingSkillConfig;
            Map<String, Object> skillDigging = diggingSkillConfig;
            Map<String, Object> miningAbilities =
                    (Map<String, Object>) miningSkillConfig.get("abilities");
            Map<String, Object> woodcuttingAbilities =
                    (Map<String, Object>) woodcuttingSkillConfig.get("abilities");

            assertEquals(0.20, ((Number) sitting.get("seat-y-offset")).doubleValue(), 0.0001);
            assertTrue((Boolean) updater.get("enabled"));
            assertEquals(2, ((Number) root.get("configuration-layout")).intValue());
            assertNotNull(modules.get("mining"));
            assertNotNull(modules.get("auth"));
            assertNotNull(modules.get("mounts"));
            assertNotNull(modules.get("dragons"));
            assertNotNull(modules.get("skills"));
            assertFalse((Boolean) ((Map<String, Object>) modules.get("mining")).get("enabled"));
            assertTrue((Boolean) ((Map<String, Object>) modules.get("skills")).get("enabled"));
            assertFalse(modules.containsKey("sitting"));
            assertFalse(root.containsKey("auth"));
            assertFalse(root.containsKey("campfire"));
            assertFalse(root.containsKey("minigame"));
            assertFalse(root.containsKey("sounds"));
            assertNotNull(fishing.get("minigame"));
            assertNotNull(fishing.get("sounds"));
            assertEquals("auth/accounts.yml", authStorage.get("file"));
            assertEquals(8, ((Number) authPassword.get("minimum-length")).intValue());
            assertEquals(64, ((Number) authPassword.get("maximum-length")).intValue());
            assertEquals(600_000, ((Number) authPassword.get("pbkdf2-iterations")).intValue());
            assertEquals(5, ((Number) authLogin.get("maximum-attempts")).intValue());
            assertEquals(120, ((Number) authLogin.get("timeout-seconds")).intValue());
            assertTrue((Boolean) authSession.get("enabled"));
            assertEquals(600, ((Number) authSession.get("duration-seconds")).intValue());
            assertFalse((Boolean) authCommands.get("fallback-enabled"));
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
            assertTrue((Boolean) skillsExperience.get("enabled"));
            assertEquals(1.10, ((Number) skillsExperience.get("multiplier")).doubleValue(), 0.0001);
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
            assertTrue((Boolean) veinSound.get("enabled"));
            assertTrue((Boolean) oreSound.get("enabled"));
            assertFalse(veinSound.get("sound").equals(oreSound.get("sound")));
            assertEquals("mounts/data.yml", mountStorage.get("file"));
            assertEquals("mounts/data.db", mountStorage.get("database-file"));
            assertEquals(3, ((Number) mountsConfig.get("configuration-version")).intValue());
            assertEquals(60, ((Number) mountDeath.get("cooldown-seconds")).intValue());
            assertEquals(15, ((Number) mountCombat.get("block-seconds")).intValue());
            assertEquals(List.of(), mountSummoning.get("allowed-worlds"));
            assertEquals(30, ((Number) mountSummoning.get("cooldown-seconds")).intValue());
            assertEquals(3, ((Number) mountSummoning.get("active-recall-cooldown-seconds")).intValue());
            assertEquals(3, ((Number) mountSummoning.get("active-teleport-distance-chunks")).intValue());
            assertEquals(7, ((Number) mountSummoning.get("minimum-spawn-distance")).intValue());
            assertEquals(12, ((Number) mountSummoning.get("maximum-spawn-distance")).intValue());
            assertEquals(3.0, ((Number) mountSummoning.get("waiting-radius")).doubleValue(), 0.0001);
            assertEquals(5.0, ((Number) mountSummoning.get("wandering-radius")).doubleValue(), 0.0001);
            assertEquals(100, ((Number) mountPersistence.get("autosave-period-ticks")).intValue());
            assertTrue((Boolean) mountPersistence.get("recall-on-quit"));
            assertEquals(2, ((Number) mountNaming.get("minimum-length")).intValue());
            assertEquals(24, ((Number) mountNaming.get("maximum-length")).intValue());
            assertEquals("GOAT_HORN", mountWhistle.get("material"));
            assertEquals(260102, ((Number) mountWhistle.get("custom-model-data")).intValue());
            assertEquals(10, ((Number) dragonSummoning.get("cooldown-seconds")).intValue());
            assertEquals(0.12, ((Number) dragonFlight.get("speed")).doubleValue(), 0.0001);
            assertEquals(256, ((Number) dragonFlight.get("maximum-altitude")).intValue());
            assertEquals("skills/data.db", skillStorage.get("database-file"));
            assertEquals(100, ((Number) skillProgression.get("base-experience")).intValue());
            assertEquals(35, ((Number) skillProgression.get("linear-growth")).intValue());
            assertEquals(2, ((Number) skillProgression.get("quadratic-growth")).intValue());
            assertTrue((Boolean) fishingLoot.get("treasures-enabled"));
            assertNotNull(fishingLoot.get("treasures"));
            assertNotNull(skillWoodcutting.get("experience"));
            assertNotNull(skillDigging.get("experience"));
            assertEquals(24, ((Number) ((Map<String, Object>)
                    miningAbilities.get("vein-mining")).get("maximum-blocks")).intValue());
            assertEquals(64, ((Number) ((Map<String, Object>)
                    woodcuttingAbilities.get("tree-feller")).get("maximum-blocks")).intValue());
            assertEquals(10, ((Number) ((Map<String, Object>)
                    woodcuttingAbilities.get("tree-feller")).get("duration-seconds")).intValue());
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
            assertNotNull(messages.get("auth-login-required"));
            assertNotNull(messages.get("auth-register-success"));
            assertNotNull(messages.get("auth-too-many-attempts"));
            assertNotNull(messages.get("auth-session-restored"));
            assertNotNull(messages.get("auth-time-remaining"));
            assertNotNull(messages.get("auth-fallback-disabled"));
            assertNotNull(messages.get("auth-change-current-prompt"));
            assertNotNull(messages.get("auth-change-new-prompt"));
            assertNotNull(messages.get("auth-change-confirm-prompt"));
            assertNotNull(messages.get("auth-current-password-invalid"));
            assertNotNull(messages.get("auth-password-changed"));
            assertNotNull(messages.get("menu-fishing-info"));
            assertNotNull(messages.get("menu-campfire-rested"));
            assertNotNull(messages.get("menu-mining-info"));
            assertNotNull(messages.get("mount-status"));
            assertNotNull(messages.get("mount-combat-blocked"));
            assertNotNull(messages.get("mount-created"));
            assertNotNull(messages.get("mount-summon-cooldown"));
            assertNotNull(messages.get("mount-recall-cooldown"));
            assertNotNull(messages.get("mount-whistle-foreign"));
            assertNotNull(messages.get("mount-whistle-bound"));
            assertNotNull(messages.get("mount-whistle-restored"));
            assertNotNull(messages.get("mount-switch-failed"));
            assertNotNull(messages.get("dragon-milestone-locked"));
            assertNotNull(messages.get("dragon-called"));
            assertNotNull(messages.get("dragon-model-failed"));
            assertNotNull(messages.get("mount-equipment-chest-only"));
            assertNotNull(messages.get("mount-storage-not-empty"));
            assertNotNull(messages.get("skills-admin-usage"));
            assertNotNull(messages.get("skills-admin-result"));
            assertNotNull(messages.get("skills-admin-inspect-audit"));
            assertNotNull(messages.get("skills-admin-metrics"));
            assertNotNull(messages.get("skills-admin-export-started"));
            assertNotNull(messages.get("skills-admin-export-complete"));
            assertNotNull(messages.get("skills-vein-mining-complete"));
            assertNotNull(messages.get("skills-tree-feller-complete"));
            assertNotNull(messages.get("skills-tree-feller-active"));
            assertNotNull(messages.get("skills-drilling-ready"));
            assertNotNull(messages.get("skills-ability-cooldown"));
            assertNotNull(messages.get("skills-experience-awarded"));
            assertEquals(
                    "<green>Odpočatý</green> <dark_gray>|</dark_gray> <white>%remaining_text%</white>",
                    messages.get("campfire-rested-timer")
            );
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void centralMenuPermissionIsAvailableToPlayersByDefault() throws Exception {
        Map<String, Object> plugin = loadYaml("plugin.yml");
        List<String> softDependencies = (List<String>) plugin.get("softdepend");
        assertFalse(softDependencies.contains("ProtocolLib"));
        Map<String, Object> permissions = (Map<String, Object>) plugin.get("permissions");
        Map<String, Object> menuPermission =
                (Map<String, Object>) permissions.get("nekararpg.menu.use");

        assertNotNull(menuPermission);
        assertEquals(true, menuPermission.get("default"));
        Map<String, Object> statusPermission =
                (Map<String, Object>) permissions.get("nekararpg.command.status");
        assertNotNull(statusPermission);
        assertEquals("op", statusPermission.get("default"));
        Map<String, Object> skillsAdminPermission =
                (Map<String, Object>) permissions.get("nekararpg.skills.admin");
        assertNotNull(skillsAdminPermission);
        assertEquals("op", skillsAdminPermission.get("default"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(String resource) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resource);
        assertNotNull(stream, resource + " is missing from the test classpath");
        try (stream; InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Yaml().load(reader);
        }
    }
}
