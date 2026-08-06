package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerkPurchaseServiceTest {
    @TempDir
    File temporaryDirectory;

    @Test
    void purchasePersistsOneRankAndOnePointAtomically() throws Exception {
        SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();
        DefaultPerkTree tree = DefaultPerkTree.create();
        try (SqliteSkillProfileRepository repository = repository()) {
            repository.save(profileAtEverySkillLevel(curve, 20), 0);
            PerkPurchaseService service = service(repository, curve, tree);

            PerkPurchaseResult result = service.purchase("player-1", new PerkId("tezba.yield"));

            assertEquals(PerkPurchaseStatus.PURCHASED, result.status());
            assertEquals(1, result.profile().perkRank(new PerkId("tezba.yield")));
            assertEquals(1, result.profile().spentPerkPoints());
            SkillProfile stored = repository.find("player-1").orElseThrow();
            assertEquals(result.profile().revision(), stored.revision());
            assertEquals(1, stored.spentPerkPoints());
        }
    }

    @Test
    void requirementsAndAvailablePointsAreAlwaysRecheckedServerSide() throws Exception {
        SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();
        DefaultPerkTree tree = DefaultPerkTree.create();
        try (SqliteSkillProfileRepository repository = repository()) {
            repository.save(profileAtEverySkillLevel(curve, 20), 0);
            PerkPurchaseService service = service(repository, curve, tree);

            assertEquals(PerkPurchaseStatus.PREREQUISITE_REQUIRED,
                service.purchase("player-1", new PerkId("tezba.tempo")).status());
            service.purchase("player-1", new PerkId("tezba.yield"));
            service.purchase("player-1", new PerkId("tezba.yield"));
            assertEquals(PerkPurchaseStatus.PURCHASED,
                service.purchase("player-1", new PerkId("tezba.tempo")).status());
        }
    }

    @Test
    void nextRankNeedsItsOwnSkillLevelEvenWhenThePlayerHasPoints() throws Exception {
        SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();
        DefaultPerkTree tree = DefaultPerkTree.create();
        try (SqliteSkillProfileRepository repository = repository()) {
            repository.save(profileAtEverySkillLevel(curve, 1), 0);
            PerkPurchaseService service = service(repository, curve, tree);

            assertEquals(PerkPurchaseStatus.PURCHASED,
                service.purchase("player-1", new PerkId("tezba.yield")).status());
            assertEquals(PerkPurchaseStatus.LEVEL_REQUIRED,
                service.purchase("player-1", new PerkId("tezba.yield")).status());
            assertEquals(1, repository.find("player-1").orElseThrow().spentPerkPoints());
        }
    }

    @Test
    void secondFiveRankBranchUsesItsOwnRankLevelRequirements() throws Exception {
        SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();
        DefaultPerkTree tree = DefaultPerkTree.create();
        try (SqliteSkillProfileRepository repository = repository()) {
            repository.save(profileAtEverySkillLevel(curve, 20), 0);
            PerkPurchaseService service = service(repository, curve, tree);

            service.purchase("player-1", new PerkId("tezba.yield"));
            service.purchase("player-1", new PerkId("tezba.yield"));
            service.purchase("player-1", new PerkId("tezba.tempo"));

            assertEquals(PerkPurchaseStatus.LEVEL_REQUIRED,
                service.purchase("player-1", new PerkId("tezba.tempo")).status());
        }
    }

    private SqliteSkillProfileRepository repository() throws Exception {
        return new SqliteSkillProfileRepository(new File(temporaryDirectory, "skills.db"));
    }

    private static PerkPurchaseService service(
        SqliteSkillProfileRepository repository,
        SkillProgressionCurve curve,
        DefaultPerkTree tree
    ) {
        return new PerkPurchaseService(
            repository,
            new SkillProgressResolver(curve),
            tree.catalog(),
            new PerkPurchasePolicy(),
            3
        );
    }

    private static SkillProfile profileAtEverySkillLevel(SkillProgressionCurve curve, int level) {
        SkillProfile profile = SkillProfile.empty("player-1");
        long experience = curve.cumulativeExperienceForLevel(level);
        for (SkillId skill : SkillId.gameplaySkills()) {
            profile = profile.withExperience(skill, experience);
        }
        return profile;
    }
}
