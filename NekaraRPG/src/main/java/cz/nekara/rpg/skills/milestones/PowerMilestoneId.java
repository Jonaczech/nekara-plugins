package cz.nekara.rpg.skills.milestones;

/** Server-authoritative gates derived from the player's Hlavní úroveň. */
public enum PowerMilestoneId {
    CAMPFIRE_RESTED("campfire_rested", 1),
    MOUNT("mount", 25),
    DRAGON_BOND("dragon_bond", 100),
    HERO_AURA("hero_aura", 200);

    private final String id;
    private final int requiredPowerLevel;

    PowerMilestoneId(String id, int requiredPowerLevel) {
        this.id = id;
        this.requiredPowerLevel = requiredPowerLevel;
    }

    public String id() { return id; }
    public int requiredPowerLevel() { return requiredPowerLevel; }

    public PowerMilestone milestone() {
        return new PowerMilestone(id, requiredPowerLevel);
    }
}
