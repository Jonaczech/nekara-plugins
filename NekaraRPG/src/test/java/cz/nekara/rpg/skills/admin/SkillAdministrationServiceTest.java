package cz.nekara.rpg.skills.admin;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillAdministrationServiceTest {
    private static final SkillAdminActor ACTOR = new SkillAdminActor("admin-id", "Admin");

    @TempDir
    File temporaryDirectory;

    @Test
    void grantsAreValidatedPersistedAndAuditedWithProfileRevisions() throws Exception {
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillAdministrationService service = service(repository);

            SkillAdminResult experience = service.execute(
                ACTOR,
                "player-id",
                "Player",
                SkillAdminOperation.grantExperience(SkillId.MINING, 1_000)
            );
            assertTrue(experience.changed());
            assertEquals(1_000, experience.profile().totalExperience(SkillId.MINING));
            assertEquals(1, experience.profile().revision());

            SkillAdminResult perk = service.execute(
                ACTOR,
                "player-id",
                "Player",
                SkillAdminOperation.grantPerk(new PerkId("tezba.yield"), 3)
            );
            assertEquals(3, perk.profile().perkRank(new PerkId("tezba.yield")));
            assertEquals(3, perk.profile().spentPerkPoints());
            assertEquals(2, perk.profile().revision());

            SkillAdminInspection inspection = service.inspect("player-id");
            assertEquals(2, inspection.recentAuditEntries().size());
            SkillAuditEntry latest = inspection.recentAuditEntries().getFirst();
            assertEquals("grant_perk", latest.operation());
            assertEquals("Admin", latest.actor().displayName());
            assertEquals(1, latest.revisionBefore());
            assertEquals(2, latest.revisionAfter());
            assertTrue(latest.detail().contains("new_rank=3"));
        }
    }

    @Test
    void noOpDoesNotCreateAuditEntryOrAdvanceRevision() throws Exception {
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillAdministrationService service = service(repository);
            SkillAdminResult first = service.execute(
                ACTOR,
                "player-id",
                "Player",
                SkillAdminOperation.grantPerk(new PerkId("tezba.yield"), 1)
            );
            SkillAdminResult repeated = service.execute(
                ACTOR,
                "player-id",
                "Player",
                SkillAdminOperation.grantPerk(new PerkId("tezba.yield"), 1)
            );

            assertFalse(repeated.changed());
            assertEquals(first.profile().revision(), repeated.profile().revision());
            assertEquals(1, repository.findRecentAuditEntries("player-id", 10).size());
        }
    }

    @Test
    void resetsKeepSkillAndPerkScopesExplicit() throws Exception {
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillAdministrationService service = service(repository);
            service.execute(ACTOR, "player-id", "Player",
                SkillAdminOperation.grantExperience(SkillId.MINING, 2_000));
            service.execute(ACTOR, "player-id", "Player",
                SkillAdminOperation.grantPerk(new PerkId("tezba.yield"), 2));

            SkillAdminResult skillReset = service.execute(
                ACTOR, "player-id", "Player", SkillAdminOperation.resetSkill(SkillId.MINING));
            assertEquals(0, skillReset.profile().totalExperience(SkillId.MINING));
            assertEquals(2, skillReset.profile().perkRank(new PerkId("tezba.yield")));
            assertEquals(2, skillReset.profile().spentPerkPoints());

            SkillAdminResult perkReset = service.execute(
                ACTOR, "player-id", "Player", SkillAdminOperation.resetPerks());
            assertTrue(perkReset.profile().perkRanks().isEmpty());
            assertEquals(0, perkReset.profile().spentPerkPoints());
        }
    }

    @Test
    void experienceGrantCapsAtMaximumAndPerkGrantRejectsInvalidRank() throws Exception {
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillAdministrationService service = service(repository);
            long cap = SkillProgressionCurve.defaultCurve().cumulativeExperienceForLevel(100);
            SkillAdminResult capped = service.execute(
                ACTOR,
                "player-id",
                "Player",
                SkillAdminOperation.grantExperience(SkillId.MINING, Long.MAX_VALUE)
            );
            assertEquals(cap, capped.profile().totalExperience(SkillId.MINING));
            assertEquals(100, capped.progress().skill(SkillId.MINING).level());

            assertThrows(IllegalArgumentException.class, () -> service.execute(
                ACTOR,
                "player-id",
                "Player",
                SkillAdminOperation.grantPerk(new PerkId("tezba.yield"), 6)
            ));
            assertThrows(IllegalArgumentException.class,
                () -> SkillAdminOperation.grantExperience(SkillId.POWER, 10));
        }
    }

    @Test
    void administratorCanAddAndRemoveOnlyTheExplicitTestingPointBonus() throws Exception {
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillAdministrationService service = service(repository);
            SkillAdminResult added = service.execute(
                ACTOR, "player-id", "Player", SkillAdminOperation.adjustBonusPerkPoints(12));
            assertEquals(12, added.profile().adminBonusPerkPoints());
            assertEquals("adjust_bonus_perk_points", repository.findRecentAuditEntries("player-id", 1)
                .getFirst().operation());

            SkillAdminResult removed = service.execute(
                ACTOR, "player-id", "Player", SkillAdminOperation.adjustBonusPerkPoints(-5));
            assertEquals(7, removed.profile().adminBonusPerkPoints());

            SkillAdminResult emptied = service.execute(
                ACTOR, "player-id", "Player", SkillAdminOperation.adjustBonusPerkPoints(-99));
            assertEquals(0, emptied.profile().adminBonusPerkPoints());
        }
    }

    private SqliteSkillProfileRepository repository() throws Exception {
        return new SqliteSkillProfileRepository(new File(temporaryDirectory, "skills.db"));
    }

    private SkillAdministrationService service(SqliteSkillProfileRepository repository) {
        return new SkillAdministrationService(
            repository,
            SkillProgressionCurve.defaultCurve(),
            DefaultPerkTree.create().catalog(),
            Clock.fixed(Instant.parse("2026-08-03T12:00:00Z"), ZoneOffset.UTC),
            3
        );
    }
}
