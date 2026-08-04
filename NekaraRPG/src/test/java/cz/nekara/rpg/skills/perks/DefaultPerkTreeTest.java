package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPerkTreeTest {
    @Test
    void everyGameplaySkillHasSixPresentedNodes() {
        DefaultPerkTree tree = DefaultPerkTree.create();
        Set<PerkPosition> navigationPositions = Set.of(
            new PerkPosition(0, 0),
            new PerkPosition(4, 0),
            new PerkPosition(8, 0),
            new PerkPosition(0, 2),
            new PerkPosition(8, 2),
            new PerkPosition(0, 4),
            new PerkPosition(4, 4),
            new PerkPosition(8, 4)
        );

        assertEquals(90, tree.catalog().size());
        for (SkillId skill : SkillId.gameplaySkills()) {
            assertEquals(6, tree.catalog().forSkill(skill).size(), skill.id());
            tree.catalog().forSkill(skill).forEach(perk -> {
                tree.presentation(perk.id());
                assertTrue(!navigationPositions.contains(perk.position()),
                    () -> skill.id() + " perk overlaps the fixed tree navigation: " + perk.position());
            });
        }
    }

    @Test
    void includesEveryDeclaredActiveMechanicFamily() {
        DefaultPerkTree tree = DefaultPerkTree.create();
        Set<MechanicId> found = EnumSet.noneOf(MechanicId.class);
        for (SkillId skill : SkillId.gameplaySkills()) {
            for (PerkDefinition perk : tree.catalog().forSkill(skill)) {
                for (PerkEffectDefinition effect : perk.effects()) {
                    if (effect instanceof MechanicPerkEffect mechanic) {
                        found.add(mechanic.mechanicId());
                    }
                }
            }
        }

        assertTrue(found.containsAll(EnumSet.allOf(MechanicId.class)));
    }
}
