package cz.nekara.rpg.skills.experience;

import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.profile.ConcurrentProfileUpdateException;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import java.util.Objects;
import java.util.Optional;

public final class SkillExperienceService {
    private final SkillProfileRepository repository;
    private final SkillProgressionCurve progressionCurve;
    private final SkillProgressResolver progressResolver;
    private final ExperiencePolicy policy;
    private final ExperienceGrantGuard grantGuard;
    private final int maximumSaveAttempts;

    public SkillExperienceService(
        SkillProfileRepository repository,
        SkillProgressionCurve progressionCurve,
        ExperiencePolicy policy,
        ExperienceGrantGuard grantGuard,
        int maximumSaveAttempts
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.progressionCurve = Objects.requireNonNull(progressionCurve, "progressionCurve");
        this.progressResolver = new SkillProgressResolver(progressionCurve);
        this.policy = Objects.requireNonNull(policy, "policy");
        this.grantGuard = Objects.requireNonNull(grantGuard, "grantGuard");
        if (maximumSaveAttempts < 1 || maximumSaveAttempts > 10) {
            throw new IllegalArgumentException("Maximum save attempts must be between 1 and 10");
        }
        this.maximumSaveAttempts = maximumSaveAttempts;
    }

    public ExperienceAwardResult award(ExperienceAwardRequest request) {
        Objects.requireNonNull(request, "request");
        ExperienceDecision decision = policy.evaluate(request.context());
        if (!decision.allowed()) {
            return ExperienceAwardResult.withoutProfile(ExperienceAwardStatus.DENIED);
        }
        if (!grantGuard.tryAcquire(request.fingerprint())) {
            return ExperienceAwardResult.withoutProfile(ExperienceAwardStatus.DUPLICATE);
        }

        long scaledExperience = Math.max(1, (long) Math.floor(
            request.baseExperience() * decision.multiplier()));
        long capExperience = progressionCurve.cumulativeExperienceForLevel(
            progressionCurve.maxLevel());

        ConcurrentProfileUpdateException lastConflict = null;
        for (int attempt = 0; attempt < maximumSaveAttempts; attempt++) {
            SkillProfile profile = repository.find(request.playerKey())
                .orElseGet(() -> SkillProfile.empty(request.playerKey()));
            long currentExperience = profile.totalExperience(request.skill());
            if (currentExperience >= capExperience) {
                return result(ExperienceAwardStatus.CAPPED, 0, profile);
            }

            long uncappedExperience;
            try {
                uncappedExperience = Math.addExact(currentExperience, scaledExperience);
            } catch (ArithmeticException overflow) {
                uncappedExperience = Long.MAX_VALUE;
            }
            long nextExperience = Math.min(capExperience, uncappedExperience);
            long awardedExperience = nextExperience - currentExperience;
            SkillProfile updated = profile.withExperience(request.skill(), nextExperience);
            try {
                SkillProfile saved = repository.save(updated, profile.revision());
                return result(ExperienceAwardStatus.AWARDED, awardedExperience, saved);
            } catch (ConcurrentProfileUpdateException conflict) {
                lastConflict = conflict;
            }
        }
        throw Objects.requireNonNull(lastConflict, "lastConflict");
    }

    private ExperienceAwardResult result(
        ExperienceAwardStatus status,
        long awardedExperience,
        SkillProfile profile
    ) {
        return new ExperienceAwardResult(
            status,
            awardedExperience,
            Optional.of(profile),
            Optional.of(progressResolver.resolve(profile))
        );
    }
}
