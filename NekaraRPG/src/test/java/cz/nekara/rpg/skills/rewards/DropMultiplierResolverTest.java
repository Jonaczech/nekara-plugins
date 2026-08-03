package cz.nekara.rpg.skills.rewards;

import cz.nekara.rpg.skills.combat.ChanceRoller;
import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatEngine;
import cz.nekara.rpg.skills.stats.StatId;
import cz.nekara.rpg.skills.stats.StatModifier;
import cz.nekara.rpg.skills.stats.StatSnapshot;
import java.util.ArrayDeque;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DropMultiplierResolverTest {
    private final DropMultiplierResolver resolver = new DropMultiplierResolver();

    @Test
    void tripleAndDoubleRewardsAreMutuallyExclusive() {
        StatSnapshot stats = statsWithBothChances();
        SequenceRoller roller = new SequenceRoller(true, true);

        assertEquals(3, resolver.resolve(stats, roller));
        assertEquals(1, roller.rollCount());
    }

    @Test
    void doubleRewardIsCheckedOnlyWhenTripleFails() {
        StatSnapshot stats = statsWithBothChances();
        SequenceRoller roller = new SequenceRoller(false, true);

        assertEquals(2, resolver.resolve(stats, roller));
        assertEquals(2, roller.rollCount());
    }

    @Test
    void ordinaryRewardRemainsUntouchedWhenBothRollsFail() {
        assertEquals(1, resolver.resolve(statsWithBothChances(), new SequenceRoller(false, false)));
    }

    private static StatSnapshot statsWithBothChances() {
        return new StatEngine().resolve(List.of(
            new StatModifier("triple", StatId.TRIPLE_DROP_CHANCE, ModifierOperation.ADD, 0.5),
            new StatModifier("double", StatId.DOUBLE_DROP_CHANCE, ModifierOperation.ADD, 0.5)
        ));
    }

    private static final class SequenceRoller implements ChanceRoller {
        private final ArrayDeque<Boolean> results;
        private int rollCount;

        private SequenceRoller(Boolean... results) {
            this.results = new ArrayDeque<>(List.of(results));
        }

        @Override
        public boolean succeeds(double chance) {
            rollCount++;
            return results.removeFirst();
        }

        private int rollCount() {
            return rollCount;
        }
    }
}
