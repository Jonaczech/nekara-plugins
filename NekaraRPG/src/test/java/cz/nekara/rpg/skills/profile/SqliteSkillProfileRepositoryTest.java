package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.PerkId;
import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteSkillProfileRepositoryTest {
    @TempDir
    File temporaryDirectory;

    @Test
    void profileRoundTripsAcrossRepositoryRestart() throws Exception {
        File database = new File(temporaryDirectory, "skills/data.db");
        SkillProfile saved;
        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(database)) {
            SkillProfile profile = new SkillProfile(
                "player-1",
                Map.of(SkillId.MINING, 12_345L, SkillId.FISHING, 456L),
                Map.of(new PerkId("mining.prospector"), 2),
                2,
                0
            );
            saved = repository.save(profile, 0);
            assertEquals(1, saved.revision());
        }

        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(database)) {
            SkillProfile loaded = repository.find("player-1").orElseThrow();
            assertEquals(saved.totalExperience(), loaded.totalExperience());
            assertEquals(saved.perkRanks(), loaded.perkRanks());
            assertEquals(saved.spentPerkPoints(), loaded.spentPerkPoints());
            assertEquals(saved.revision(), loaded.revision());
        }
    }

    @Test
    void optimisticRevisionRejectsStaleWriterWithoutPartialChanges() throws Exception {
        File database = new File(temporaryDirectory, "skills.db");
        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(database)) {
            SkillProfile original = repository.save(SkillProfile.empty("player-1"), 0);
            SkillProfile current = repository.save(
                original.withExperience(SkillId.MINING, 100), original.revision());

            assertThrows(ConcurrentProfileUpdateException.class,
                () -> repository.save(original.withExperience(SkillId.FISHING, 999), original.revision()));

            SkillProfile loaded = repository.find("player-1").orElseThrow();
            assertEquals(current.revision(), loaded.revision());
            assertEquals(100, loaded.totalExperience(SkillId.MINING));
            assertEquals(0, loaded.totalExperience(SkillId.FISHING));
        }
    }

    @Test
    void missingProfileIsNotCreatedByRead() throws Exception {
        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(
            new File(temporaryDirectory, "skills.db"))) {
            assertTrue(repository.find("missing").isEmpty());
        }
    }

    @Test
    void unknownFutureSchemaIsRejectedBeforeCurrentTablesAreCreated() throws Exception {
        File database = new File(temporaryDirectory, "future.db");
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            statement.execute("INSERT INTO metadata(key,value) VALUES('schema-version','99')");
        }

        assertThrows(IOException.class, () -> new SqliteSkillProfileRepository(database));

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             var statement = connection.prepareStatement(
                 "SELECT 1 FROM sqlite_master WHERE type='table' AND name='profiles'");
             var result = statement.executeQuery()) {
            assertFalse(result.next());
        }
    }
}
