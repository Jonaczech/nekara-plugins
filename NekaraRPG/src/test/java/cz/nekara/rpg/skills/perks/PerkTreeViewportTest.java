package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkTreeViewportTest {
    @Test
    void movesOnlyInsideTheVirtualPerkGraph() {
        List<PerkDefinition> perks = List.of(
            perk("left", new PerkPosition(0, 0)),
            perk("far", new PerkPosition(12, 7))
        );
        PerkTreeViewport viewport = PerkTreeViewport.initial(perks, 9, 4);

        assertTrue(viewport.contains(new PerkPosition(0, 0)));
        assertFalse(viewport.canMove(-1, 0, perks));
        assertTrue(viewport.canMove(1, 0, perks));
        assertEquals(new PerkTreeViewport(5, 5, 9, 4), viewport.move(99, 99, perks));
    }

    @Test
    void translatesVisiblePositionsIntoViewportSlots() {
        PerkTreeViewport viewport = new PerkTreeViewport(3, 2, 9, 4);

        assertEquals(0, viewport.slot(new PerkPosition(3, 2)));
        assertEquals(32, viewport.slot(new PerkPosition(8, 5)));
    }

    @Test
    void movesDiagonallyWhenTheVirtualGraphContinuesInBothDirections() {
        List<PerkDefinition> perks = List.of(
            perk("origin", new PerkPosition(0, 0)),
            perk("far", new PerkPosition(12, 7))
        );
        PerkTreeViewport viewport = PerkTreeViewport.initial(perks, 9, 4);

        assertTrue(viewport.canMove(1, 1, perks));
        assertEquals(new PerkTreeViewport(1, 1, 9, 4), viewport.move(1, 1, perks));
    }

    @Test
    void keepsOneCellOfPaddingAroundTheGraphWhenPossible() {
        List<PerkDefinition> perks = List.of(
            perk("top", new PerkPosition(4, 1)),
            perk("bottom", new PerkPosition(4, 7))
        );

        assertEquals(new PerkTreeViewport(0, 0, 9, 5), PerkTreeViewport.initial(perks, 9, 5));
        assertEquals(new PerkTreeViewport(0, 4, 9, 5),
            PerkTreeViewport.initial(perks, 9, 5).move(0, 99, perks));
    }

    private PerkDefinition perk(String id, PerkPosition position) {
        return new PerkDefinition(
            new PerkId("tezba." + id), SkillId.MINING, 1, 1, 0, Set.of(),
            List.of(new MechanicPerkEffect(MechanicId.VEIN_MINING)), position
        );
    }
}
