package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkEffectPresentationTest {
    @Test
    void showsOneCompactLineForTheDisplayedRank() {
        PerkDefinition perk = perk(5, new StatPerkEffect(
            StatId.DOUBLE_DROP_CHANCE, ModifierOperation.ADD, 0.025));

        assertEquals(
            "Hodnost 1/5: šance na dvojitý výtěžek +2,5 p. b.",
            PerkEffectPresentation.describe(perk, 0).getFirst());
        assertEquals(
            "Hodnost 5/5: šance na dvojitý výtěžek +12,5 p. b.",
            PerkEffectPresentation.describe(perk, 5).getFirst());
    }

    @Test
    void givesAConcreteDescriptionForMechanics() {
        PerkDefinition perk = perk(1, new MechanicPerkEffect(MechanicId.FIELD_HARVEST));

        assertTrue(PerkEffectPresentation.describe(perk, 0).getFirst().contains("5×5"));
    }

    @Test
    void givesTheExactWorkshopRepairContract() {
        PerkDefinition perk = perk(1, new MechanicPerkEffect(MechanicId.TINKERING));

        assertTrue(PerkEffectPresentation.describe(perk, 0).getFirst().contains("25 %"));
    }

    @Test
    void describesEveryEffectInTheDefaultNinetyPerkCatalog() {
        DefaultPerkTree tree = DefaultPerkTree.create();

        for (SkillId skill : SkillId.gameplaySkills()) {
            for (PerkDefinition perk : tree.catalog().forSkill(skill)) {
                List<String> description = PerkEffectPresentation.describe(perk, 0);
                assertEquals(1, description.size(), perk.id().value());
                assertTrue(description.stream().noneMatch(String::isBlank), perk.id().value());
            }
        }
    }

    private PerkDefinition perk(int maxRank, PerkEffectDefinition effect) {
        return new PerkDefinition(
            new PerkId("farming.test"), SkillId.FARMING, maxRank, 1, 0, Set.of(),
            List.of(effect), new PerkPosition(1, 1));
    }
}
