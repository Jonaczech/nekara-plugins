package cz.nekara.rpg.skills.newgameplus;

import cz.nekara.rpg.configuration.NewGamePlusConfig;
import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.SkillProgressionCurve;
import cz.nekara.rpg.skills.perks.PerkCatalog;
import cz.nekara.rpg.skills.profile.ConcurrentProfileUpdateException;
import cz.nekara.rpg.skills.profile.SkillProfile;
import cz.nekara.rpg.skills.profile.SkillProfileRepository;
import cz.nekara.rpg.skills.profile.SkillProgressResolver;
import cz.nekara.rpg.skills.profile.SkillProgressSnapshot;
import java.util.HashMap;
import java.util.Objects;

public final class NewGamePlusService {
    private final SkillProfileRepository repository; private final SkillProgressResolver resolver;
    private final SkillProgressionCurve curve; private final PerkCatalog catalog; private final NewGamePlusConfig config;
    public NewGamePlusService(SkillProfileRepository repository, SkillProgressionCurve curve, PerkCatalog catalog, NewGamePlusConfig config) {
        this.repository = Objects.requireNonNull(repository); this.curve = Objects.requireNonNull(curve);
        this.catalog = Objects.requireNonNull(catalog); this.config = Objects.requireNonNull(config); this.resolver = new SkillProgressResolver(curve);
    }
    public NewGamePlusResult rebirth(String playerKey, SkillId skill) {
        SkillProfile current = repository.find(playerKey).orElseGet(() -> SkillProfile.empty(playerKey));
        var progress = resolver.resolve(current);
        if (!config.enabled()) return result(NewGamePlusStatus.DISABLED, current, progress, 0);
        if (progress.skill(skill).level() < curve.maxLevel()) return result(NewGamePlusStatus.NOT_MAX_LEVEL, current, progress, 0);
        if (current.newGamePlusRank(skill) >= 1) return result(NewGamePlusStatus.MAXIMUM_RANK_REACHED, current, progress, 0);
        for (int attempt = 0; attempt < 3; attempt++) {
            HashMap<cz.nekara.rpg.skills.perks.PerkId, Integer> remaining = new HashMap<>(current.perkRanks());
            int refund = 0;
            for (var perk : catalog.forSkill(skill)) {
                int rank = remaining.getOrDefault(perk.id(), 0);
                if (rank > 0) { remaining.remove(perk.id()); refund = Math.addExact(refund, rank * perk.pointCostPerRank()); }
            }
            SkillProfile next = current.withNewGamePlus(skill, remaining, refund);
            try { SkillProfile saved = repository.save(next, current.revision()); return result(NewGamePlusStatus.REBORN, saved, resolver.resolve(saved), refund); }
            catch (ConcurrentProfileUpdateException conflict) { current = repository.find(playerKey).orElseThrow(); progress = resolver.resolve(current); }
        }
        throw new IllegalStateException("New Game+ profile changed repeatedly");
    }
    private static NewGamePlusResult result(NewGamePlusStatus status, SkillProfile profile, SkillProgressSnapshot progress, int refund) { return new NewGamePlusResult(status, profile, progress, refund); }
}
