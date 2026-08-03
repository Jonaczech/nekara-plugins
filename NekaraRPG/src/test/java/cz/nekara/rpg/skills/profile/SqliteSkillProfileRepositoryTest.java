package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.admin.SkillAdminActor;
import cz.nekara.rpg.skills.admin.SkillAuditRecord;
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

    @Test
    void versionOneDatabaseMigratesToVersionTwoWithoutLosingProfiles() throws Exception {
        File database = new File(temporaryDirectory, "version-one.db");
        Class.forName("org.sqlite.JDBC");
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            statement.execute("INSERT INTO metadata(key,value) VALUES('schema-version','1')");
            statement.execute("CREATE TABLE profiles (player_key TEXT PRIMARY KEY, "
                + "spent_perk_points INTEGER NOT NULL, revision INTEGER NOT NULL)");
            statement.execute("CREATE TABLE skill_experience (player_key TEXT NOT NULL, skill_id TEXT NOT NULL, "
                + "total_experience INTEGER NOT NULL, PRIMARY KEY(player_key,skill_id), "
                + "FOREIGN KEY(player_key) REFERENCES profiles(player_key) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE perk_ranks (player_key TEXT NOT NULL, perk_id TEXT NOT NULL, "
                + "rank INTEGER NOT NULL, PRIMARY KEY(player_key,perk_id), "
                + "FOREIGN KEY(player_key) REFERENCES profiles(player_key) ON DELETE CASCADE)");
            statement.execute("INSERT INTO profiles VALUES('player-1',1,4)");
            statement.execute("INSERT INTO skill_experience VALUES('player-1','mining',1234)");
            statement.execute("INSERT INTO perk_ranks VALUES('player-1','mining.yield',1)");
        }

        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(database)) {
            SkillProfile migrated = repository.find("player-1").orElseThrow();
            assertEquals(1_234, migrated.totalExperience(SkillId.MINING));
            assertEquals(1, migrated.perkRank(new PerkId("mining.yield")));
            assertEquals(4, migrated.revision());

            SkillProfile updated = migrated.withExperience(SkillId.MINING, 2_000);
            repository.saveAdminMutation(updated, migrated.revision(), new SkillAuditRecord(
                new SkillAdminActor("console", "Console"),
                "Player",
                "grant_xp",
                "skill=mining;granted=766",
                1_775_390_400_000L
            ));
            assertEquals(1, repository.findRecentAuditEntries("player-1", 10).size());
        }

        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
             var statement = connection.prepareStatement(
                 "SELECT value FROM metadata WHERE key='schema-version'");
             var result = statement.executeQuery()) {
            assertTrue(result.next());
            assertEquals("2", result.getString(1));
        }
    }

    @Test
    void staleAdministrativeMutationRollsBackAuditAndProfileTogether() throws Exception {
        File database = new File(temporaryDirectory, "admin-atomic.db");
        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(database)) {
            SkillProfile original = repository.save(SkillProfile.empty("player-1"), 0);
            SkillProfile current = repository.save(
                original.withExperience(SkillId.MINING, 100), original.revision());

            assertThrows(ConcurrentProfileUpdateException.class, () -> repository.saveAdminMutation(
                original.withExperience(SkillId.MINING, 999),
                original.revision(),
                new SkillAuditRecord(
                    new SkillAdminActor("admin", "Admin"),
                    "Player",
                    "grant_xp",
                    "skill=mining;granted=999",
                    1L
                )
            ));

            SkillProfile loaded = repository.find("player-1").orElseThrow();
            assertEquals(current.revision(), loaded.revision());
            assertEquals(current.totalExperience(), loaded.totalExperience());
            assertEquals(current.perkRanks(), loaded.perkRanks());
            assertEquals(current.spentPerkPoints(), loaded.spentPerkPoints());
            assertTrue(repository.findRecentAuditEntries("player-1", 10).isEmpty());
        }
    }

    @Test
    void failedAuditInsertRollsBackAdministrativeProfileMutation() throws Exception {
        File database = new File(temporaryDirectory, "admin-audit-failure.db");
        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(database)) {
            SkillProfile original = repository.save(SkillProfile.empty("player-1"), 0);
            try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
                 var statement = connection.createStatement()) {
                statement.execute("CREATE TRIGGER reject_admin_audit BEFORE INSERT ON admin_audit "
                    + "BEGIN SELECT RAISE(ABORT, 'test audit failure'); END");
            }

            assertThrows(SkillStorageException.class, () -> repository.saveAdminMutation(
                original.withExperience(SkillId.MINING, 999),
                original.revision(),
                new SkillAuditRecord(
                    new SkillAdminActor("admin", "Admin"),
                    "Player",
                    "grant_xp",
                    "skill=mining;granted=999",
                    1L
                )
            ));

            SkillProfile loaded = repository.find("player-1").orElseThrow();
            assertEquals(original.revision(), loaded.revision());
            assertEquals(0, loaded.totalExperience(SkillId.MINING));
            assertTrue(repository.findRecentAuditEntries("player-1", 10).isEmpty());
        }
    }
}
