package cz.nekara.rpg.skills.combat;

import java.util.Objects;

public record CombatContext(
    DamageOrigin origin,
    double baseDamage,
    boolean bleedImmune,
    boolean stunImmune
) {
    public CombatContext {
        Objects.requireNonNull(origin, "origin");
        if (!Double.isFinite(baseDamage) || baseDamage < 0) {
            throw new IllegalArgumentException("Base damage must be finite and non-negative");
        }
    }
}
