package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.Objects;

public final class PerkPurchasePolicy {
    public PerkPurchaseDecision evaluate(
        SkillProfile profile,
        SkillProgressSnapshot progress,
        PerkDefinition perk
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(perk, "perk");
        int availablePoints = Math.max(0, progress.power().level() + profile.adminBonusPerkPoints()
            - profile.spentPerkPoints());
        int currentRank = profile.perkRank(perk.id());
        if (currentRank >= perk.maxRank()) {
            return new PerkPurchaseDecision(PerkPurchaseStatus.MAX_RANK, availablePoints);
        }
        if (progress.skill(perk.skill()).level() < perk.requiredSkillLevel()) {
            return new PerkPurchaseDecision(PerkPurchaseStatus.LEVEL_REQUIRED, availablePoints);
        }
        for (PerkRequirement requirement : perk.requirements()) {
            if (profile.perkRank(requirement.perkId()) < requirement.minimumRank()) {
                return new PerkPurchaseDecision(
                    PerkPurchaseStatus.PREREQUISITE_REQUIRED, availablePoints);
            }
        }
        if (availablePoints < perk.pointCostPerRank()) {
            return new PerkPurchaseDecision(
                PerkPurchaseStatus.INSUFFICIENT_POINTS, availablePoints);
        }
        return new PerkPurchaseDecision(PerkPurchaseStatus.PURCHASED, availablePoints);
    }
}
