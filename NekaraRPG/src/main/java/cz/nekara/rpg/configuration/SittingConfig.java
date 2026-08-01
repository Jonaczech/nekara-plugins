package cz.nekara.rpg.configuration;

import org.bukkit.entity.EntityType;

import java.util.Set;

public record SittingConfig(
        boolean requireGround,
        double seatYOffset,
        boolean allowCreative,
        boolean allowFlying,
        boolean standOnDamage,
        boolean detectExternalSeats,
        Set<EntityType> externalSeatEntityTypes
) {
}
