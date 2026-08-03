package cz.nekara.rpg.skills.stats;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StatEngineTest {
    private final StatEngine engine = new StatEngine();

    @Test
    void additiveModifiersResolveBeforeMultipliers() {
        double resolved = engine.resolve(StatId.DAMAGE_MULTIPLIER, List.of(
            new StatModifier("perk.one", StatId.DAMAGE_MULTIPLIER, ModifierOperation.ADD, 0.5),
            new StatModifier("item.sword", StatId.DAMAGE_MULTIPLIER, ModifierOperation.MULTIPLY, 2)
        ));

        assertEquals(3.0, resolved, 0.000_001);
    }

    @Test
    void repeatedModifierFromSameSourceReplacesInsteadOfStacking() {
        double resolved = engine.resolve(StatId.CRITICAL_CHANCE, List.of(
            new StatModifier("perk.precision", StatId.CRITICAL_CHANCE, ModifierOperation.ADD, 0.1),
            new StatModifier("perk.precision", StatId.CRITICAL_CHANCE, ModifierOperation.ADD, 0.2)
        ));

        assertEquals(0.2, resolved, 0.000_001);
    }

    @Test
    void probabilityStatsAreClampedAndInvalidFactorsAreRejected() {
        double resolved = engine.resolve(StatId.STUN_CHANCE, List.of(
            new StatModifier("test", StatId.STUN_CHANCE, ModifierOperation.ADD, 5)
        ));

        assertEquals(1.0, resolved, 0.000_001);
        assertThrows(IllegalArgumentException.class,
            () -> new StatModifier("test", StatId.DAMAGE_MULTIPLIER, ModifierOperation.MULTIPLY, -1));
    }
}
