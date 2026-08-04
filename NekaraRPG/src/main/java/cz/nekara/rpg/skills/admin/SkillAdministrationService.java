package cz.nekara.rpg.skills.admin;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.perks.PerkCatalog;
import cz.nekara.rpg.skills.perks.PerkDefinition;
import cz.nekara.rpg.skills.perks.PerkId;
import cz.nekara.rpg.skills.profile.ConcurrentProfileUpdateException;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import java.time.Clock;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SkillAdministrationService {
    private static final int INSPECTION_AUDIT_LIMIT = 5;

    private final SkillAdministrationRepository repository;
    private final SkillProgressionCurve progressionCurve;
    private final SkillProgressResolver progressResolver;
    private final PerkCatalog perkCatalog;
    private final Clock clock;
    private final int maximumSaveAttempts;

    public SkillAdministrationService(
        SkillAdministrationRepository repository,
        SkillProgressionCurve progressionCurve,
        PerkCatalog perkCatalog,
        Clock clock,
        int maximumSaveAttempts
    ) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.progressionCurve = Objects.requireNonNull(progressionCurve, "progressionCurve");
        this.progressResolver = new SkillProgressResolver(progressionCurve);
        this.perkCatalog = Objects.requireNonNull(perkCatalog, "perkCatalog");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (maximumSaveAttempts < 1 || maximumSaveAttempts > 10) {
            throw new IllegalArgumentException("Maximum save attempts must be between 1 and 10");
        }
        this.maximumSaveAttempts = maximumSaveAttempts;
    }

    public SkillAdminInspection inspect(String playerKey) {
        SkillProfile profile = repository.find(playerKey)
            .orElseGet(() -> SkillProfile.empty(playerKey));
        return new SkillAdminInspection(
            profile,
            progressResolver.resolve(profile),
            repository.findRecentAuditEntries(playerKey, INSPECTION_AUDIT_LIMIT)
        );
    }

    public SkillAdminResult execute(
        SkillAdminActor actor,
        String targetPlayerKey,
        String targetDisplayName,
        SkillAdminOperation operation
    ) {
        Objects.requireNonNull(actor, "actor");
        requireText(targetPlayerKey, "Target player key");
        requireText(targetDisplayName, "Target player name");
        Objects.requireNonNull(operation, "operation");

        ConcurrentProfileUpdateException lastConflict = null;
        for (int attempt = 0; attempt < maximumSaveAttempts; attempt++) {
            SkillProfile current = repository.find(targetPlayerKey)
                .orElseGet(() -> SkillProfile.empty(targetPlayerKey));
            PreparedMutation prepared = prepare(current, operation);
            if (!prepared.changed()) {
                return result(current, operation, prepared.status(), prepared.affectedValue());
            }
            SkillAuditRecord audit = new SkillAuditRecord(
                actor,
                targetDisplayName,
                operation.type().auditId(),
                prepared.auditDetail(),
                clock.millis()
            );
            try {
                SkillProfile saved = repository.saveAdminMutation(
                    prepared.profile(), current.revision(), audit);
                return result(saved, operation, SkillAdminStatus.CHANGED, prepared.affectedValue());
            } catch (ConcurrentProfileUpdateException conflict) {
                lastConflict = conflict;
            }
        }
        throw Objects.requireNonNull(lastConflict, "lastConflict");
    }

    private PreparedMutation prepare(SkillProfile profile, SkillAdminOperation operation) {
        return switch (operation.type()) {
            case GRANT_EXPERIENCE -> grantExperience(profile, operation.skill(), operation.amount());
            case GRANT_PERK -> grantPerk(profile, operation.perkId(), operation.rank());
            case ADJUST_BONUS_PERK_POINTS -> adjustBonusPerkPoints(profile, operation.amount());
            case RESET_SKILL -> resetSkill(profile, operation.skill());
            case RESET_PERKS -> resetPerks(profile);
            case RESET_ALL -> resetAll(profile);
        };
    }

    private PreparedMutation grantExperience(SkillProfile profile, SkillId skill, long amount) {
        long current = profile.totalExperience(skill);
        long cap = progressionCurve.cumulativeExperienceForLevel(progressionCurve.maxLevel());
        long requestedTotal;
        try {
            requestedTotal = Math.addExact(current, amount);
        } catch (ArithmeticException overflow) {
            requestedTotal = Long.MAX_VALUE;
        }
        long next = Math.min(cap, requestedTotal);
        long granted = next - current;
        if (granted == 0) {
            return unchanged(profile, SkillAdminStatus.ALREADY_CAPPED);
        }
        return changed(
            profile.withExperience(skill, next),
            granted,
            "skill=" + skill.id() + ";requested=" + amount + ";granted=" + granted
                + ";previous_total=" + current + ";new_total=" + next
        );
    }

    private PreparedMutation grantPerk(SkillProfile profile, PerkId perkId, int requestedRank) {
        PerkDefinition perk = perkCatalog.require(perkId);
        if (requestedRank > perk.maxRank()) {
            throw new IllegalArgumentException(
                "Requested rank exceeds maximum rank " + perk.maxRank() + " for " + perkId);
        }
        int currentRank = profile.perkRank(perkId);
        if (currentRank >= requestedRank) {
            return unchanged(profile, SkillAdminStatus.RANK_ALREADY_PRESENT);
        }
        int addedRanks = requestedRank - currentRank;
        int pointCost = Math.multiplyExact(addedRanks, perk.pointCostPerRank());
        int spentPoints = Math.addExact(profile.spentPerkPoints(), pointCost);
        Map<PerkId, Integer> perks = new HashMap<>(profile.perkRanks());
        perks.put(perkId, requestedRank);
        SkillProfile updated = new SkillProfile(
            profile.playerKey(),
            profile.totalExperience(),
            perks,
            spentPoints,
            profile.adminBonusPerkPoints(),
            profile.revision()
        );
        return changed(
            updated,
            requestedRank,
            "perk=" + perkId.value() + ";previous_rank=" + currentRank + ";new_rank="
                + requestedRank + ";charged_points=" + pointCost
        );
    }

    private PreparedMutation adjustBonusPerkPoints(SkillProfile profile, long adjustment) {
        long requested = (long) profile.adminBonusPerkPoints() + adjustment;
        int next = (int) Math.max(0, Math.min(Integer.MAX_VALUE, requested));
        if (next == profile.adminBonusPerkPoints()) {
            return unchanged(profile, SkillAdminStatus.BONUS_POINTS_ALREADY_EMPTY);
        }
        return changed(
            profile.withAdminBonusPerkPoints(next),
            Math.abs((long) next - profile.adminBonusPerkPoints()),
            "previous_bonus=" + profile.adminBonusPerkPoints() + ";adjustment=" + adjustment
                + ";new_bonus=" + next
        );
    }

    private PreparedMutation resetSkill(SkillProfile profile, SkillId skill) {
        long previous = profile.totalExperience(skill);
        if (previous == 0) {
            return unchanged(profile, SkillAdminStatus.SKILL_ALREADY_EMPTY);
        }
        return changed(
            profile.withExperience(skill, 0),
            previous,
            "skill=" + skill.id() + ";previous_total=" + previous
        );
    }

    private PreparedMutation resetPerks(SkillProfile profile) {
        if (profile.perkRanks().isEmpty() && profile.spentPerkPoints() == 0) {
            return unchanged(profile, SkillAdminStatus.PERKS_ALREADY_EMPTY);
        }
        SkillProfile updated = new SkillProfile(
            profile.playerKey(),
            profile.totalExperience(),
            Map.of(),
            0,
            profile.adminBonusPerkPoints(),
            profile.revision()
        );
        return changed(
            updated,
            profile.perkRanks().size(),
            "removed_perks=" + profile.perkRanks().size()
                + ";refunded_points=" + profile.spentPerkPoints()
        );
    }

    private PreparedMutation resetAll(SkillProfile profile) {
        boolean hasExperience = SkillId.gameplaySkills().stream()
            .anyMatch(skill -> profile.totalExperience(skill) > 0);
        if (!hasExperience && profile.perkRanks().isEmpty() && profile.spentPerkPoints() == 0
            && profile.adminBonusPerkPoints() == 0) {
            return unchanged(profile, SkillAdminStatus.PROFILE_ALREADY_EMPTY);
        }
        long previousExperience = 0;
        for (long experience : profile.totalExperience().values()) {
            previousExperience = Math.addExact(previousExperience, experience);
        }
        SkillProfile updated = new SkillProfile(
            profile.playerKey(),
            new EnumMap<>(SkillId.class),
            Map.of(),
            0,
            0,
            profile.revision()
        );
        return changed(
            updated,
            previousExperience,
            "removed_experience=" + previousExperience + ";removed_perks="
                + profile.perkRanks().size() + ";refunded_points=" + profile.spentPerkPoints()
                + ";removed_bonus_points=" + profile.adminBonusPerkPoints()
        );
    }

    private SkillAdminResult result(
        SkillProfile profile,
        SkillAdminOperation operation,
        SkillAdminStatus status,
        long affectedValue
    ) {
        return new SkillAdminResult(
            profile, progressResolver.resolve(profile), operation, status, affectedValue);
    }

    private static PreparedMutation changed(
        SkillProfile profile,
        long affectedValue,
        String auditDetail
    ) {
        return new PreparedMutation(
            profile, SkillAdminStatus.CHANGED, affectedValue, auditDetail);
    }

    private static PreparedMutation unchanged(SkillProfile profile, SkillAdminStatus status) {
        return new PreparedMutation(profile, status, 0, "unchanged=true");
    }

    private static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
    }

    private record PreparedMutation(
        SkillProfile profile,
        SkillAdminStatus status,
        long affectedValue,
        String auditDetail
    ) {
        boolean changed() {
            return status == SkillAdminStatus.CHANGED;
        }
    }
}
