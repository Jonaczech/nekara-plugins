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
    private final Map<SkillId, Integer> newGamePlusRanks;
    private final Map<PerkId, Integer> perkRanks;
    private final int spentPerkPoints;
    private final int adminBonusPerkPoints;
    private final long revision;

    public SkillProfile(String playerKey, Map<SkillId, Long> totalExperience,
                        Map<PerkId, Integer> perkRanks, int spentPerkPoints, long revision) {
        this(playerKey, totalExperience, Map.of(), perkRanks, spentPerkPoints, 0, revision);
    }

    public SkillProfile(String playerKey, Map<SkillId, Long> totalExperience,
                        Map<PerkId, Integer> perkRanks, int spentPerkPoints,
                        int adminBonusPerkPoints, long revision) {
        this(playerKey, totalExperience, Map.of(), perkRanks, spentPerkPoints, adminBonusPerkPoints, revision);
    }

    public SkillProfile(String playerKey, Map<SkillId, Long> totalExperience,
                        Map<SkillId, Integer> newGamePlusRanks, Map<PerkId, Integer> perkRanks,
                        int spentPerkPoints, int adminBonusPerkPoints, long revision) {
        Objects.requireNonNull(playerKey, "playerKey");
        Objects.requireNonNull(totalExperience, "totalExperience");
        Objects.requireNonNull(newGamePlusRanks, "newGamePlusRanks");
        Objects.requireNonNull(perkRanks, "perkRanks");
        if (playerKey.isBlank()) throw new IllegalArgumentException("Player key cannot be blank");
        if (spentPerkPoints < 0 || adminBonusPerkPoints < 0 || revision < 0) {
            throw new IllegalArgumentException("Perk points and revision cannot be negative");
        }
        EnumMap<SkillId, Long> experience = new EnumMap<>(SkillId.class);
        for (var entry : totalExperience.entrySet()) {
            SkillId skill = Objects.requireNonNull(entry.getKey(), "experience skill");
            Long value = Objects.requireNonNull(entry.getValue(), "experience value");
            if (!skill.gainsExperience() || value < 0) {
                throw new IllegalArgumentException("Experience must be non-negative for a gameplay skill");
            }
            if (value > 0) experience.put(skill, value);
        }
        EnumMap<SkillId, Integer> rebirths = new EnumMap<>(SkillId.class);
        for (var entry : newGamePlusRanks.entrySet()) {
            SkillId skill = Objects.requireNonNull(entry.getKey(), "new game plus skill");
            Integer rank = Objects.requireNonNull(entry.getValue(), "new game plus rank");
            if (!skill.gainsExperience() || rank != 1) {
                throw new IllegalArgumentException("New Game+ can be activated only once per gameplay skill");
            }
            rebirths.put(skill, rank);
        }
        Map<PerkId, Integer> perks = new HashMap<>();
        for (var entry : perkRanks.entrySet()) {
            PerkId perk = Objects.requireNonNull(entry.getKey(), "perk ID");
            Integer rank = Objects.requireNonNull(entry.getValue(), "perk rank");
            if (rank < 1) throw new IllegalArgumentException("Stored perk ranks must be positive");
            perks.put(perk, rank);
        }
        this.playerKey = playerKey;
        this.totalExperience = Map.copyOf(experience);
        this.newGamePlusRanks = Map.copyOf(rebirths);
        this.perkRanks = Map.copyOf(perks);
        this.spentPerkPoints = spentPerkPoints;
        this.adminBonusPerkPoints = adminBonusPerkPoints;
        this.revision = revision;
    }

    public static SkillProfile empty(String playerKey) {
        return new SkillProfile(playerKey, Map.of(), Map.of(), Map.of(), 0, 0, 0);
    }
    public String playerKey() { return playerKey; }
    public Map<SkillId, Long> totalExperience() { return totalExperience; }
    public Map<SkillId, Integer> newGamePlusRanks() { return newGamePlusRanks; }
    public Map<PerkId, Integer> perkRanks() { return perkRanks; }
    public int spentPerkPoints() { return spentPerkPoints; }
    public int adminBonusPerkPoints() { return adminBonusPerkPoints; }
    public long revision() { return revision; }
    public long totalExperience(SkillId skill) {
        requireGameplay(skill);
        return totalExperience.getOrDefault(skill, 0L);
    }
    public int newGamePlusRank(SkillId skill) {
        requireGameplay(skill);
        return newGamePlusRanks.getOrDefault(skill, 0);
    }
    public int perkRank(PerkId perkId) { return perkRanks.getOrDefault(Objects.requireNonNull(perkId, "perkId"), 0); }

    public SkillProfile withExperience(SkillId skill, long total) {
        requireGameplay(skill);
        if (total < 0) throw new IllegalArgumentException("Skill experience cannot be negative");
        EnumMap<SkillId, Long> updated = new EnumMap<>(SkillId.class);
        updated.putAll(totalExperience);
        if (total == 0) updated.remove(skill); else updated.put(skill, total);
        return new SkillProfile(playerKey, updated, newGamePlusRanks, perkRanks, spentPerkPoints, adminBonusPerkPoints, revision);
    }
    public SkillProfile withRevision(long value) {
        return new SkillProfile(playerKey, totalExperience, newGamePlusRanks, perkRanks, spentPerkPoints, adminBonusPerkPoints, value);
    }
    public SkillProfile withPurchasedPerk(PerkId perkId, int newRank, int pointCost) {
        Objects.requireNonNull(perkId, "perkId");
        if (newRank < 1 || pointCost < 1 || newRank != perkRank(perkId) + 1) {
            throw new IllegalArgumentException("A perk purchase must add exactly one positive rank");
        }
        Map<PerkId, Integer> updated = new HashMap<>(perkRanks);
        updated.put(perkId, newRank);
        return new SkillProfile(playerKey, totalExperience, newGamePlusRanks, updated,
            Math.addExact(spentPerkPoints, pointCost), adminBonusPerkPoints, revision);
    }
    public SkillProfile withAdminBonusPerkPoints(int newBonus) {
        if (newBonus < 0) throw new IllegalArgumentException("Admin bonus perk points cannot be negative");
        return new SkillProfile(playerKey, totalExperience, newGamePlusRanks, perkRanks, spentPerkPoints, newBonus, revision);
    }
    public SkillProfile withNewGamePlus(SkillId skill, Map<PerkId, Integer> remainingPerks, int refundedPoints) {
        requireGameplay(skill);
        Objects.requireNonNull(remainingPerks, "remainingPerks");
        if (refundedPoints < 0 || refundedPoints > spentPerkPoints) throw new IllegalArgumentException("Invalid refunded perk points");
        EnumMap<SkillId, Long> experience = new EnumMap<>(SkillId.class);
        experience.putAll(totalExperience);
        experience.remove(skill);
        EnumMap<SkillId, Integer> ranks = new EnumMap<>(SkillId.class);
        ranks.putAll(newGamePlusRanks);
        if (newGamePlusRank(skill) != 0) throw new IllegalStateException("New Game+ is already active for this skill");
        ranks.put(skill, 1);
        return new SkillProfile(playerKey, experience, ranks, remainingPerks,
            spentPerkPoints - refundedPoints, adminBonusPerkPoints, revision);
    }
    private static void requireGameplay(SkillId skill) {
        Objects.requireNonNull(skill, "skill");
        if (!skill.gainsExperience()) throw new IllegalArgumentException("Derived skills cannot store direct progression");
    }
}
