package cz.nekara.fishing.configuration;

public record FishingConfig(
        boolean cancelOnTeleport,
        boolean cancelOnWorldChange,
        boolean cancelOnItemChange,
        boolean cancelOnDamage,
        boolean requireFishingRodInMainHand,
        boolean allowCreative,
        boolean allowSpectator
) {
}
