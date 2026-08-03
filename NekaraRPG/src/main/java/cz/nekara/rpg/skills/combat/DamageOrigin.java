package cz.nekara.rpg.skills.combat;

public enum DamageOrigin {
    PLAYER_ATTACK(true),
    BLEED_TICK(false),
    ACTIVE_ABILITY(false),
    EXTERNAL_PLUGIN(false),
    ENVIRONMENT(false);

    private final boolean canTriggerCombatEffects;

    DamageOrigin(boolean canTriggerCombatEffects) {
        this.canTriggerCombatEffects = canTriggerCombatEffects;
    }

    public boolean canTriggerCombatEffects() {
        return canTriggerCombatEffects;
    }
}
