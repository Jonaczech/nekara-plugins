package cz.nekara.rpg.skills.newgameplus;

import cz.nekara.rpg.configuration.NewGamePlusConfig;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.perks.DefaultPerkTree;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NewGamePlusServiceTest {
    @TempDir File directory;

    @Test
    void maxedSkillResetsOnlyItsProgressionAndRefundsItsPerks() throws Exception {
        SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();
        DefaultPerkTree tree = DefaultPerkTree.create();
        var miningPerk = tree.catalog().forSkill(SkillId.MINING).getFirst();
        try (SqliteSkillProfileRepository repository = new SqliteSkillProfileRepository(new File(directory, "skills.db"))) {
            SkillProfile profile = new SkillProfile("player", Map.of(
                SkillId.MINING, curve.cumulativeExperienceForLevel(curve.maxLevel()), SkillId.FISHING, 100L),
                Map.of(miningPerk.id(), 1), miningPerk.pointCostPerRank(), 0);
            repository.save(profile, 0);
            NewGamePlusResult result = new NewGamePlusService(repository, curve, tree.catalog(),
                new NewGamePlusConfig(true, 0.90, 0.02)).rebirth("player", SkillId.MINING);
            assertEquals(NewGamePlusStatus.REBORN, result.status());
            assertEquals(0, result.profile().totalExperience(SkillId.MINING));
            assertEquals(100, result.profile().totalExperience(SkillId.FISHING));
            assertEquals(1, result.profile().newGamePlusRank(SkillId.MINING));
            assertEquals(0, result.profile().perkRank(miningPerk.id()));
            assertEquals(0, result.profile().spentPerkPoints());
            assertEquals(miningPerk.pointCostPerRank(), result.refundedPoints());
        }
    }
}
