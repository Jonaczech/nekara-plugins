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
            var perks = tree.catalog().forSkill(skill);
            assertEquals(6, perks.size(), skill.id());
            perks.forEach(perk -> tree.presentation(perk.id()));
            int width = perks.stream().mapToInt(perk -> perk.position().column()).max().orElseThrow()
                - perks.stream().mapToInt(perk -> perk.position().column()).min().orElseThrow();
            int height = perks.stream().mapToInt(perk -> perk.position().row()).max().orElseThrow()
                - perks.stream().mapToInt(perk -> perk.position().row()).min().orElseThrow();
            assertTrue(width <= 8, () -> skill.id() + " perk tree is too wide: " + width);
            assertTrue(height <= 10, () -> skill.id() + " perk tree is too tall: " + height);
            perks.forEach(perk -> perk.requirements().forEach(requirement -> {
                PerkDefinition prerequisite = tree.catalog().require(requirement.perkId());
                int intermediaryCells = Math.abs(perk.position().column() - prerequisite.position().column())
                    + Math.abs(perk.position().row() - prerequisite.position().row()) - 1;
                assertTrue(intermediaryCells >= 2 && intermediaryCells <= 4,
                    () -> perk.id().value() + " must use 2-4 intermediary connection cells, got " + intermediaryCells);
            }));
        }
    }

    @Test
    void themedSkillsUseTheirOwnPerkLayouts() {
        DefaultPerkTree tree = DefaultPerkTree.create();

        assertMatchesLayout(tree, SkillId.HEAVY_WEAPONS);
        assertMatchesLayout(tree, SkillId.SMITHING);
        assertMatchesLayout(tree, SkillId.ARCHERY);
        assertMatchesLayout(tree, SkillId.FISHING);
        assertMatchesLayout(tree, SkillId.FARMING);
    }

    private static void assertMatchesLayout(DefaultPerkTree tree, SkillId skill) {
        PerkTreeLayout layout = PerkTreeLayout.forSkill(skill);
        Set<PerkPosition> expected = Set.of(
            layout.root(), layout.left(), layout.right(), layout.leftDeep(), layout.rightDeep(), layout.crown());
        Set<PerkPosition> actual = tree.catalog().forSkill(skill).stream()
            .map(PerkDefinition::position).collect(java.util.stream.Collectors.toSet());
        assertEquals(expected, actual, skill.id());
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
