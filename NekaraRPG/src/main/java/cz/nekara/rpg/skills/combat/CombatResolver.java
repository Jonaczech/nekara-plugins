package cz.nekara.rpg.skills.combat;

import cz.nekara.rpg.skills.stats.StatId;
import cz.nekara.rpg.skills.stats.StatSnapshot;
import java.util.Objects;

public final class CombatResolver {
    public CombatResolution resolve(
        CombatContext context,
        StatSnapshot stats,
        ChanceRoller chanceRoller
    ) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(stats, "stats");
        Objects.requireNonNull(chanceRoller, "chanceRoller");

        double damage = context.baseDamage();
        if (!context.origin().canTriggerCombatEffects()) {
            return new CombatResolution(damage, false, false, false);
        }

        damage *= stats.value(StatId.DAMAGE_MULTIPLIER);
        boolean critical = chanceRoller.succeeds(stats.value(StatId.CRITICAL_CHANCE));
        if (critical) {
            damage *= stats.value(StatId.CRITICAL_DAMAGE_MULTIPLIER);
        }

        boolean bleed = !context.bleedImmune()
            && chanceRoller.succeeds(stats.value(StatId.BLEED_CHANCE));
        boolean stun = !context.stunImmune()
            && chanceRoller.succeeds(stats.value(StatId.STUN_CHANCE));
        return new CombatResolution(damage, critical, bleed, stun);
    }
}
