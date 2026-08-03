package cz.nekara.rpg.skills.combat;

import cz.nekara.rpg.skills.stats.ModifierOperation;
import cz.nekara.rpg.skills.stats.StatEngine;
import cz.nekara.rpg.skills.stats.StatId;
import cz.nekara.rpg.skills.stats.StatModifier;
import cz.nekara.rpg.skills.stats.StatSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatResolverTest {
    private final CombatResolver resolver = new CombatResolver();

    @Test
    void primaryAttackCanCritBleedAndStunThroughOneResolutionPipeline() {
        StatSnapshot stats = new StatEngine().resolve(List.of(
            add(StatId.DAMAGE_MULTIPLIER, 0.5),
            add(StatId.CRITICAL_CHANCE, 1),
            add(StatId.CRITICAL_DAMAGE_MULTIPLIER, 0.5),
            add(StatId.BLEED_CHANCE, 1),
            add(StatId.STUN_CHANCE, 1)
        ));

        CombatResolution result = resolver.resolve(
            new CombatContext(DamageOrigin.PLAYER_ATTACK, 10, false, false),
            stats,
            chance -> true
        );

        assertEquals(30, result.finalDamage(), 0.000_001);
        assertTrue(result.critical());
        assertTrue(result.bleedApplied());
        assertTrue(result.stunApplied());
    }

    @Test
    void secondaryDamageCannotRecursivelyTriggerAnyCombatEffect() {
        StatSnapshot stats = new StatEngine().resolve(List.of(
            add(StatId.DAMAGE_MULTIPLIER, 5),
            add(StatId.CRITICAL_CHANCE, 1),
            add(StatId.BLEED_CHANCE, 1),
            add(StatId.STUN_CHANCE, 1)
        ));

        CombatResolution result = resolver.resolve(
            new CombatContext(DamageOrigin.BLEED_TICK, 4, false, false),
            stats,
            chance -> {
                throw new AssertionError("Secondary damage must not roll effects");
            }
        );

        assertEquals(4, result.finalDamage(), 0.000_001);
        assertFalse(result.critical());
        assertFalse(result.bleedApplied());
        assertFalse(result.stunApplied());
    }

    @Test
    void targetImmunitiesAreRespectedBeforeChanceRolls() {
        StatSnapshot stats = new StatEngine().resolve(List.of(
            add(StatId.BLEED_CHANCE, 1),
            add(StatId.STUN_CHANCE, 1)
        ));

        CombatResolution result = resolver.resolve(
            new CombatContext(DamageOrigin.PLAYER_ATTACK, 2, true, true),
            stats,
            chance -> false
        );

        assertFalse(result.bleedApplied());
        assertFalse(result.stunApplied());
    }

    private static StatModifier add(StatId stat, double amount) {
        return new StatModifier("test." + stat.name(), stat, ModifierOperation.ADD, amount);
    }
}
