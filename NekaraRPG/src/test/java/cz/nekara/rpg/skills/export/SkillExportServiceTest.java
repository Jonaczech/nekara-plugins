package cz.nekara.rpg.skills.export;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExportServiceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsConsistentDatabaseAndCsvWithoutChangingTheLiveProfile() throws Exception {
        Path database = temporaryDirectory.resolve("skills/data.db");
        Path exports = temporaryDirectory.resolve("skills/exports");
        Instant createdAt = Instant.parse("2026-08-04T10:15:30Z");
        String playerKey = "c5297cee-d020-4f61-b821-6de679b67fc1";

        try (SqliteSkillProfileRepository repository =
                 new SqliteSkillProfileRepository(database.toFile())) {
            SkillProfile saved = repository.save(
                SkillProfile.empty(playerKey).withExperience(SkillId.MINING, 1_234L), 0L);
            SkillExportService service = new SkillExportService(
                repository, exports, "2.2.0", Clock.fixed(createdAt, ZoneOffset.UTC));

            SkillExportResult result = service.export();

            assertEquals(createdAt, result.createdAt());
            assertEquals(1L, result.profileCount());
            assertEquals(64, result.sha256().length());
            assertTrue(Files.isRegularFile(result.archive()));
            assertEquals(Files.size(result.archive()), result.sizeBytes());
            assertProfile(saved, repository.find(playerKey).orElseThrow());

            Path extractedDatabase = temporaryDirectory.resolve("extracted.db");
            try (ZipFile zip = new ZipFile(result.archive().toFile())) {
                Set<String> entries = zip.stream().map(entry -> entry.getName())
                    .collect(java.util.stream.Collectors.toSet());
                assertEquals(Set.of(
                    "manifest.properties",
                    "data.db",
                    "profiles.csv",
                    "skill_experience.csv",
                    "perk_ranks.csv",
                    "new_game_plus.csv",
                    "admin_audit.csv"
                ), entries);
                String manifest = new String(
                    zip.getInputStream(zip.getEntry("manifest.properties")).readAllBytes(),
                    StandardCharsets.UTF_8);
                assertTrue(manifest.contains("format=nekara-skills-export-v1"));
                assertTrue(manifest.contains("plugin_version=2.2.0"));
                assertTrue(manifest.contains("profiles=1"));
                String experienceCsv = new String(
                    zip.getInputStream(zip.getEntry("skill_experience.csv")).readAllBytes(),
                    StandardCharsets.UTF_8);
                assertTrue(experienceCsv.contains(playerKey + ",mining,1234"));
                Files.copy(zip.getInputStream(zip.getEntry("data.db")), extractedDatabase);
            }

            try (SqliteSkillProfileRepository snapshot =
                     new SqliteSkillProfileRepository(extractedDatabase.toFile())) {
                assertProfile(saved, snapshot.find(playerKey).orElseThrow());
            }

            try (var files = Files.list(exports)) {
                assertFalse(files.anyMatch(path -> path.getFileName().toString().startsWith(".")));
            }
            assertNotNull(result.archive().getFileName());
        }
    }

    private static void assertProfile(SkillProfile expected, SkillProfile actual) {
        assertEquals(expected.playerKey(), actual.playerKey());
        assertEquals(expected.totalExperience(), actual.totalExperience());
        assertEquals(expected.newGamePlusRanks(), actual.newGamePlusRanks());
        assertEquals(expected.perkRanks(), actual.perkRanks());
        assertEquals(expected.spentPerkPoints(), actual.spentPerkPoints());
        assertEquals(expected.revision(), actual.revision());
    }
}
