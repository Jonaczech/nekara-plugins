package cz.nekara.rpg.skills.perks;

import java.util.Objects;

public record MechanicPerkEffect(MechanicId mechanicId) implements PerkEffectDefinition {
    public MechanicPerkEffect {
        Objects.requireNonNull(mechanicId, "mechanicId");
    }
}
