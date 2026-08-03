package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import cz.nekara.rpg.skills.profile.SkillProfile;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class PerkMechanicResolver {
    private final PerkCatalog catalog;

    public PerkMechanicResolver(PerkCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    public Set<MechanicId> resolve(SkillProfile profile, SkillId skill) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(skill, "skill");
        EnumSet<MechanicId> mechanics = EnumSet.noneOf(MechanicId.class);
        for (PerkDefinition perk : catalog.forSkill(skill)) {
            int rank = profile.perkRank(perk.id());
            if (rank == 0) {
                continue;
            }
            if (rank < 0 || rank > perk.maxRank()) {
                throw new IllegalStateException("Stored rank is invalid for perk " + perk.id().value());
            }
            for (PerkEffectDefinition effect : perk.effects()) {
                if (effect instanceof MechanicPerkEffect mechanic) {
                    mechanics.add(mechanic.mechanicId());
                }
            }
        }
        return Set.copyOf(mechanics);
    }

    public boolean has(SkillProfile profile, SkillId skill, MechanicId mechanic) {
        return resolve(profile, skill).contains(Objects.requireNonNull(mechanic, "mechanic"));
    }
}
