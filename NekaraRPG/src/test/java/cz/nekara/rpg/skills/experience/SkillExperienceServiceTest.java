package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SqliteSkillProfileRepository;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillExperienceServiceTest {
    @TempDir
    File temporaryDirectory;

    @Test
    void awardPersistsOnceAndReturnsUpdatedSkillAndPower() throws Exception {
        AtomicLong now = new AtomicLong(1_000);
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillExperienceService service = service(repository, now);
            ExperienceAwardRequest request = request("source-1", 100, normalContext(0));

            ExperienceAwardResult awarded = service.award(request);
            ExperienceAwardResult duplicate = service.award(request);

            assertEquals(ExperienceAwardStatus.AWARDED, awarded.status());
            assertEquals(100, awarded.awardedExperience());
            assertEquals(1, awarded.progress().orElseThrow().skill(SkillId.MINING).level());
            assertEquals(0, awarded.progress().orElseThrow().power().level());
            assertEquals(ExperienceAwardStatus.DUPLICATE, duplicate.status());
            assertEquals(100, repository.find("player-1").orElseThrow()
                .totalExperience(SkillId.MINING));
        }
    }

    @Test
    void deniedEventNeverCreatesAProfileOrConsumesFingerprint() throws Exception {
        AtomicLong now = new AtomicLong(1_000);
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillExperienceService service = service(repository, now);
            ExperienceContext cancelled = new ExperienceContext(
                SkillId.MINING, true, false, false, false, false, false, 0);

            assertEquals(ExperienceAwardStatus.DENIED,
                service.award(request("source-1", 100, cancelled)).status());
            assertTrue(repository.find("player-1").isEmpty());
            assertEquals(ExperienceAwardStatus.AWARDED,
                service.award(request("source-1", 100, normalContext(0))).status());
        }
    }

    @Test
    void awardsAreClampedAtLevelOneHundred() throws Exception {
        AtomicLong now = new AtomicLong(1_000);
        SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();
        long cap = curve.cumulativeExperienceForLevel(100);
        try (SqliteSkillProfileRepository repository = repository()) {
            SkillProfile nearlyCapped = SkillProfile.empty("player-1")
                .withExperience(SkillId.MINING, cap - 5);
            repository.save(nearlyCapped, 0);
            SkillExperienceService service = service(repository, now);

            ExperienceAwardResult result = service.award(request("source-1", 100, normalContext(0)));

            assertEquals(5, result.awardedExperience());
            assertEquals(100, result.progress().orElseThrow().skill(SkillId.MINING).level());
            now.addAndGet(10_000);
            assertEquals(ExperienceAwardStatus.CAPPED,
                service.award(request("source-2", 100, normalContext(0))).status());
        }
    }

    private SqliteSkillProfileRepository repository() throws Exception {
        return new SqliteSkillProfileRepository(new File(temporaryDirectory, "skills.db"));
    }

    private static SkillExperienceService service(
        SqliteSkillProfileRepository repository,
        AtomicLong now
    ) {
        return new SkillExperienceService(
            repository,
            SkillProgressionCurve.defaultCurve(),
            ExperiencePolicy.defaultPolicy(),
            new ExperienceGrantGuard(Duration.ofSeconds(5), 100, now::get),
            3
        );
    }

    private static ExperienceAwardRequest request(
        String sourceKey,
        long experience,
        ExperienceContext context
    ) {
        return new ExperienceAwardRequest(
            "player-1",
            SkillId.MINING,
            experience,
            context,
            new ExperienceFingerprint("player-1", SkillId.MINING, "block_break", sourceKey)
        );
    }

    private static ExperienceContext normalContext(int chunkAwards) {
        return new ExperienceContext(
            SkillId.MINING, false, false, false, false, false, false, chunkAwards);
    }
}
