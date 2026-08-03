package cz.nekara.rpg.modules.skills;

import cz.nekara.rpg.skills.perks.MechanicId;
import cz.nekara.rpg.skills.stats.StatSnapshot;

import java.util.Set;

record SkillRuntimeState(StatSnapshot stats, Set<MechanicId> mechanics) {
    SkillRuntimeState {
        mechanics = Set.copyOf(mechanics);
    }

    boolean has(MechanicId mechanic) {
        return mechanics.contains(mechanic);
    }
}
