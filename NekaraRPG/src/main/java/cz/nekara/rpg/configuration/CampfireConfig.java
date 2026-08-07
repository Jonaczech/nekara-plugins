package cz.nekara.rpg.configuration;

public record CampfireConfig(
        double radius,
        int updatePeriodTicks,
        double healAmount,
        int healPeriodSeconds,
        int hungerRestoreAmount,
        int hungerRestorePeriodSeconds,
        int restedChargeSeconds,
        int restedDurationSeconds,
        double restedHungerLossMultiplier,
        RestedExperienceConfig restedExperience,
        RestedEffectConfig restedEffect,
        CampingConfig camping,
        double groupMultiplierPerExtraPlayer,
        double maximumGroupMultiplier,
        CampfireVisualConfig visuals
) {
}
