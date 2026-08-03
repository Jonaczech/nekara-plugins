package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.profile.ConcurrentProfileUpdateException;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.Objects;

public final class PerkPurchaseService {
    private final SkillProfileRepository repository;
    private final SkillProgressResolver progressResolver;
    private final PerkCatalog catalog;
    private final PerkPurchasePolicy policy;
    private final int maximumSaveAttempts;

    public PerkPurchaseService(
        SkillProfileRepository repository,
        SkillProgressResolver progressResolver,
        PerkCatalog catalog,
        PerkPurchasePolicy policy,
        int maximumSaveAttempts
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.progressResolver = Objects.requireNonNull(progressResolver, "progressResolver");
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.policy = Objects.requireNonNull(policy, "policy");
        if (maximumSaveAttempts < 1 || maximumSaveAttempts > 10) {
            throw new IllegalArgumentException("Maximum save attempts must be between 1 and 10");
        }
        this.maximumSaveAttempts = maximumSaveAttempts;
    }

    public PerkPurchaseResult purchase(String playerKey, PerkId perkId) {
        PerkDefinition perk = catalog.require(Objects.requireNonNull(perkId, "perkId"));
        ConcurrentProfileUpdateException lastConflict = null;
        for (int attempt = 0; attempt < maximumSaveAttempts; attempt++) {
            SkillProfile profile = repository.find(playerKey)
                .orElseGet(() -> SkillProfile.empty(playerKey));
            SkillProgressSnapshot progress = progressResolver.resolve(profile);
            PerkPurchaseDecision decision = policy.evaluate(profile, progress, perk);
            if (!decision.allowed()) {
                return new PerkPurchaseResult(decision.status(), profile, progress);
            }
            SkillProfile updated = profile.withPurchasedPerk(
                perk.id(), profile.perkRank(perk.id()) + 1, perk.pointCostPerRank());
            try {
                SkillProfile saved = repository.save(updated, profile.revision());
                return new PerkPurchaseResult(
                    PerkPurchaseStatus.PURCHASED, saved, progressResolver.resolve(saved));
            } catch (ConcurrentProfileUpdateException conflict) {
                lastConflict = conflict;
            }
        }
        throw Objects.requireNonNull(lastConflict, "lastConflict");
    }
}
