package cz.nekara.rpg.skills.combat;

public record CombatResolution(
    double finalDamage,
    boolean critical,
    boolean bleedApplied,
    boolean stunApplied
) {
    public CombatResolution {
        if (!Double.isFinite(finalDamage) || finalDamage < 0) {
            throw new IllegalArgumentException("Final damage must be finite and non-negative");
        }
    }
}
