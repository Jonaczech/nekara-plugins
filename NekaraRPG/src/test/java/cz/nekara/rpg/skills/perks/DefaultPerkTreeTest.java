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

        assertEquals(90, tree.catalog().size());
        for (SkillId skill : SkillId.gameplaySkills()) {
            assertEquals(6, tree.catalog().forSkill(skill).size(), skill.id());
            tree.catalog().forSkill(skill).forEach(perk -> tree.presentation(perk.id()));
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
