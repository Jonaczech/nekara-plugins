package cz.nekara.rpg.campfire;

public final class CampfireSleepRules {
    private CampfireSleepRules() {
    }

    public static boolean canSkipNight(
            boolean enabled,
            int onlinePlayers,
            boolean lyingAtActiveCampfire,
            long eligibleMilliseconds,
            long requiredMilliseconds,
            boolean normalWorld,
            long worldTime
    ) {
        return enabled
                && onlinePlayers == 1
                && lyingAtActiveCampfire
                && eligibleMilliseconds >= requiredMilliseconds
                && normalWorld
                && worldTime >= 12_542L
                && worldTime < 23_460L;
    }
}
