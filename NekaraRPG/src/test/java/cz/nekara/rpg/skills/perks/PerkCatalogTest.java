package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PerkCatalogTest {
    @Test
    void validGraphIsIndexedAndSortedByViewportPosition() {
        PerkDefinition root = perk("mining.prospector", SkillId.MINING, Set.of(), 4, 1);
        PerkDefinition branch = perk(
            "mining.deep_vein",
            SkillId.MINING,
            Set.of(new PerkRequirement(root.id(), 2)),
            2,
            2
        );

        PerkCatalog catalog = new PerkCatalog(List.of(branch, root));

        assertEquals(2, catalog.size());
        assertEquals(List.of(root, branch), catalog.forSkill(SkillId.MINING));
        assertEquals(branch, catalog.require(branch.id()));
    }

    @Test
    void unknownAndCrossSkillPrerequisitesAreRejected() {
        PerkDefinition unknown = perk(
            "mining.deep_vein",
            SkillId.MINING,
            Set.of(new PerkRequirement(new PerkId("mining.missing"), 1)),
            1,
            1
        );
        assertThrows(IllegalArgumentException.class, () -> new PerkCatalog(List.of(unknown)));

        PerkDefinition mining = perk("mining.prospector", SkillId.MINING, Set.of(), 1, 1);
        PerkDefinition farming = perk(
            "farming.harvester",
            SkillId.FARMING,
            Set.of(new PerkRequirement(mining.id(), 1)),
            2,
            1
        );
        assertThrows(IllegalArgumentException.class, () -> new PerkCatalog(List.of(mining, farming)));
    }

    @Test
    void cyclesAndDuplicatePositionsAreRejected() {
        PerkId firstId = new PerkId("mining.first");
        PerkId secondId = new PerkId("mining.second");
        PerkDefinition first = perk(firstId.value(), SkillId.MINING,
            Set.of(new PerkRequirement(secondId, 1)), 1, 1);
        PerkDefinition second = perk(secondId.value(), SkillId.MINING,
            Set.of(new PerkRequirement(firstId, 1)), 2, 1);
        assertThrows(IllegalArgumentException.class, () -> new PerkCatalog(List.of(first, second)));

        PerkDefinition duplicatePosition = perk("mining.third", SkillId.MINING, Set.of(), 1, 1);
        assertThrows(IllegalArgumentException.class,
            () -> new PerkCatalog(List.of(firstWithoutRequirements(), duplicatePosition)));
    }

    @Test
    void powerCannotOwnOrdinaryPerks() {
        PerkDefinition powerPerk = perk("power.vitality", SkillId.POWER, Set.of(), 1, 1);
        assertThrows(IllegalArgumentException.class, () -> new PerkCatalog(List.of(powerPerk)));
    }

    private static PerkDefinition firstWithoutRequirements() {
        return perk("mining.first", SkillId.MINING, Set.of(), 1, 1);
    }

    private static PerkDefinition perk(
        String id,
        SkillId skill,
        Set<PerkRequirement> requirements,
        int column,
        int row
    ) {
        return new PerkDefinition(
            new PerkId(id),
            skill,
            3,
            1,
            10,
            requirements,
            List.of(new StatPerkEffect(StatId.DOUBLE_DROP_CHANCE, ModifierOperation.ADD, 0.01)),
            new PerkPosition(column, row)
        );
    }
}
