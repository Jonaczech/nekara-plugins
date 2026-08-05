package cz.nekara.rpg.skills.perks;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerkConnectionPathTest {
    @Test
    void fansOutFromAParentBeforeDescending() {
        assertEquals(List.of(
            new PerkPosition(3, 1),
            new PerkPosition(2, 1),
            new PerkPosition(1, 1),
            new PerkPosition(1, 2),
            new PerkPosition(1, 3)
        ), PerkConnectionPath.between(
            new PerkPosition(4, 1),
            new PerkPosition(1, 4),
            PerkConnectionPath.BendOrder.HORIZONTAL_FIRST
        ));
    }

    @Test
    void convergesIntoAChildFromTheSide() {
        assertEquals(List.of(
            new PerkPosition(1, 9),
            new PerkPosition(1, 10),
            new PerkPosition(1, 11),
            new PerkPosition(2, 11),
            new PerkPosition(3, 11)
        ), PerkConnectionPath.between(
            new PerkPosition(1, 8),
            new PerkPosition(4, 11),
            PerkConnectionPath.BendOrder.VERTICAL_FIRST
        ));
    }
}
