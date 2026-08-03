package cz.nekara.rpg.skills.profile;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.PerkId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SkillProfile {
    private final String playerKey;
    private final Map<SkillId, Long> totalExperience;
    private final Map<PerkId, Integer> perkRanks;
    private final int spentPerkPoints;
    private final long revision;

    public SkillProfile(
        String playerKey,
        Map<SkillId, Long> totalExperience,
        Map<PerkId, Integer> perkRanks,
        int spentPerkPoints,
        long revision
    ) {
        Objects.requireNonNull(playerKey, "playerKey");
        Objects.requireNonNull(totalExperience, "totalExperience");
        Objects.requireNonNull(perkRanks, "perkRanks");
        if (playerKey.isBlank()) {
            throw new IllegalArgumentException("Player key cannot be blank");
        }
        if (spentPerkPoints < 0 || revision < 0) {
            throw new IllegalArgumentException("Spent points and revision cannot be negative");
        }

        EnumMap<SkillId, Long> normalizedExperience = new EnumMap<>(SkillId.class);
        for (Map.Entry<SkillId, Long> entry : totalExperience.entrySet()) {
            SkillId skill = Objects.requireNonNull(entry.getKey(), "experience skill");
            Long experience = Objects.requireNonNull(entry.getValue(), "experience value");
            if (!skill.gainsExperience()) {
                throw new IllegalArgumentException("Derived skills cannot store direct experience");
            }
            if (experience < 0) {
                throw new IllegalArgumentException("Skill experience cannot be negative");
            }
            if (experience > 0) {
                normalizedExperience.put(skill, experience);
            }
        }

        Map<PerkId, Integer> normalizedPerks = new HashMap<>();
        for (Map.Entry<PerkId, Integer> entry : perkRanks.entrySet()) {
            PerkId perk = Objects.requireNonNull(entry.getKey(), "perk ID");
            Integer rank = Objects.requireNonNull(entry.getValue(), "perk rank");
            if (rank < 1) {
                throw new IllegalArgumentException("Stored perk ranks must be positive");
            }
            normalizedPerks.put(perk, rank);
        }

        this.playerKey = playerKey;
        this.totalExperience = Map.copyOf(normalizedExperience);
        this.perkRanks = Map.copyOf(normalizedPerks);
        this.spentPerkPoints = spentPerkPoints;
        this.revision = revision;
    }

    public static SkillProfile empty(String playerKey) {
        return new SkillProfile(playerKey, Map.of(), Map.of(), 0, 0);
    }

    public String playerKey() {
        return playerKey;
    }

    public Map<SkillId, Long> totalExperience() {
        return totalExperience;
    }

    public long totalExperience(SkillId skill) {
        Objects.requireNonNull(skill, "skill");
        if (!skill.gainsExperience()) {
            throw new IllegalArgumentException("Derived skills do not store direct experience");
        }
        return totalExperience.getOrDefault(skill, 0L);
    }

    public Map<PerkId, Integer> perkRanks() {
        return perkRanks;
    }

    public int perkRank(PerkId perkId) {
        return perkRanks.getOrDefault(Objects.requireNonNull(perkId, "perkId"), 0);
    }

    public int spentPerkPoints() {
        return spentPerkPoints;
    }

    public long revision() {
        return revision;
    }

    public SkillProfile withExperience(SkillId skill, long newTotalExperience) {
        Objects.requireNonNull(skill, "skill");
        if (!skill.gainsExperience()) {
            throw new IllegalArgumentException("Derived skills cannot receive direct experience");
        }
        if (newTotalExperience < 0) {
            throw new IllegalArgumentException("Skill experience cannot be negative");
        }
        EnumMap<SkillId, Long> updated = new EnumMap<>(SkillId.class);
        updated.putAll(totalExperience);
        if (newTotalExperience == 0) {
            updated.remove(skill);
        } else {
            updated.put(skill, newTotalExperience);
        }
        return new SkillProfile(playerKey, updated, perkRanks, spentPerkPoints, revision);
    }

    public SkillProfile withRevision(long newRevision) {
        return new SkillProfile(playerKey, totalExperience, perkRanks, spentPerkPoints, newRevision);
    }
}
