package cz.nekara.rpg.skills.admin;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.PerkId;
import java.util.Objects;

public record SkillAdminOperation(
    Type type,
    SkillId skill,
    PerkId perkId,
    long amount,
    int rank
) {
    public SkillAdminOperation {
        Objects.requireNonNull(type, "type");
    }

    public static SkillAdminOperation grantExperience(SkillId skill, long amount) {
        Objects.requireNonNull(skill, "skill");
        if (!skill.gainsExperience()) {
            throw new IllegalArgumentException("Power cannot receive direct experience");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("Granted experience must be positive");
        }
        return new SkillAdminOperation(Type.GRANT_EXPERIENCE, skill, null, amount, 0);
    }

    public static SkillAdminOperation grantPerk(PerkId perkId, int rank) {
        Objects.requireNonNull(perkId, "perkId");
        if (rank < 1) {
            throw new IllegalArgumentException("Granted perk rank must be positive");
        }
        return new SkillAdminOperation(Type.GRANT_PERK, null, perkId, 0, rank);
    }

    public static SkillAdminOperation adjustBonusPerkPoints(int amount) {
        if (amount == 0) {
            throw new IllegalArgumentException("Perk point adjustment cannot be zero");
        }
        return new SkillAdminOperation(Type.ADJUST_BONUS_PERK_POINTS, null, null, amount, 0);
    }

    public static SkillAdminOperation resetSkill(SkillId skill) {
        Objects.requireNonNull(skill, "skill");
        if (!skill.gainsExperience()) {
            throw new IllegalArgumentException("Power cannot be reset directly");
        }
        return new SkillAdminOperation(Type.RESET_SKILL, skill, null, 0, 0);
    }

    public static SkillAdminOperation resetPerks() {
        return new SkillAdminOperation(Type.RESET_PERKS, null, null, 0, 0);
    }

    public static SkillAdminOperation resetAll() {
        return new SkillAdminOperation(Type.RESET_ALL, null, null, 0, 0);
    }
    public static SkillAdminOperation maxAll() { return new SkillAdminOperation(Type.MAX_ALL, null, null, 0, 0); }

    public enum Type {
        GRANT_EXPERIENCE("grant_xp"),
        GRANT_PERK("grant_perk"),
        ADJUST_BONUS_PERK_POINTS("adjust_bonus_perk_points"),
        RESET_SKILL("reset_skill"),
        RESET_PERKS("reset_perks"),
        RESET_ALL("reset_all"), MAX_ALL("max_all");

        private final String auditId;

        Type(String auditId) {
            this.auditId = auditId;
        }

        public String auditId() {
            return auditId;
        }
    }
}
