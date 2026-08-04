package cz.nekara.rpg.skills.stats;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.perks.PerkCatalog;
import cz.nekara.rpg.skills.perks.PerkDefinition;
import cz.nekara.rpg.skills.perks.StatPerkEffect;
import cz.nekara.rpg.skills.profile.SkillProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PerkStatResolver {
    private final PerkCatalog catalog;
    private final StatEngine statEngine;

    public PerkStatResolver(PerkCatalog catalog) {
        this(catalog, new StatEngine());
    }

    public PerkStatResolver(PerkCatalog catalog, StatEngine statEngine) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.statEngine = Objects.requireNonNull(statEngine, "statEngine");
    }

    public StatSnapshot resolve(SkillProfile profile, SkillId skill) {
        return resolve(profile, skill, 1.0);
    }

    public StatSnapshot resolve(SkillProfile profile, SkillId skill, double multiplier) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(skill, "skill");
        if (!Double.isFinite(multiplier) || multiplier < 1.0) throw new IllegalArgumentException("Invalid New Game+ multiplier");
        List<StatModifier> modifiers = new ArrayList<>();
        for (PerkDefinition perk : catalog.forSkill(skill)) {
            int rank = profile.perkRank(perk.id());
            if (rank == 0) {
                continue;
            }
            if (rank < 0 || rank > perk.maxRank()) {
                throw new IllegalStateException("Stored rank is invalid for perk " + perk.id().value());
            }
            for (int index = 0; index < perk.effects().size(); index++) {
                if (perk.effects().get(index) instanceof StatPerkEffect effect) {
                    modifiers.add(new StatModifier(
                        "perk:" + perk.id().value() + ":" + index,
                        effect.statId(),
                        effect.operation(),
                        scaledAmount(effect, rank) * multiplier
                    ));
                }
            }
        }
        return statEngine.resolve(modifiers);
    }

    private static double scaledAmount(StatPerkEffect effect, int rank) {
        return switch (effect.operation()) {
            case ADD -> effect.amountPerRank() * rank;
            case MULTIPLY -> Math.pow(effect.amountPerRank(), rank);
        };
    }
}
