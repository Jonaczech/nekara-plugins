package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.milestones.PowerMilestone;
import cz.nekara.rpg.skills.milestones.PowerMilestoneCatalog;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillProgressResolverTest {
    private final SkillProgressionCurve curve = SkillProgressionCurve.defaultCurve();
    private final SkillProgressResolver resolver = new SkillProgressResolver(curve);

    @Test
    void profileExperienceResolvesIntoSkillsAndDerivedPower() {
        EnumMap<SkillId, Long> experience = new EnumMap<>(SkillId.class);
        long levelFiftyExperience = curve.cumulativeExperienceForLevel(50);
        for (SkillId skill : SkillId.gameplaySkills()) {
            experience.put(skill, levelFiftyExperience);
        }
        SkillProfile profile = new SkillProfile("player-1", experience, Map.of(), 0, 0);

        SkillProgressSnapshot snapshot = resolver.resolve(profile);

        assertEquals(50, snapshot.skill(SkillId.FISHING).level());
        assertEquals(50, snapshot.power().level());
        assertThrows(IllegalArgumentException.class, () -> snapshot.skill(SkillId.POWER));
    }

    @Test
    void mountMilestoneCanBeExposedWithoutCouplingSkillsToMountImplementation() {
        PowerMilestoneCatalog catalog = new PowerMilestoneCatalog(List.of(
            new PowerMilestone("mount_companion", 50),
            new PowerMilestone("veteran_title", 75)
        ));

        assertEquals(List.of(), catalog.unlockedAt(49));
        assertEquals(List.of(new PowerMilestone("mount_companion", 50)), catalog.unlockedAt(50));
    }

    @Test
    void derivedPowerExperienceCannotBePersisted() {
        assertThrows(IllegalArgumentException.class,
            () -> new SkillProfile("player-1", Map.of(SkillId.POWER, 100L), Map.of(), 0, 0));
    }
}
