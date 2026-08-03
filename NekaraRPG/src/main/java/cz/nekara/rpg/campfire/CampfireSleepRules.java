package cz.nekara.rpg.campfire;

public final class CampfireSleepRules {
    private CampfireSleepRules() {
    }

    public static boolean canSkipNight(
            boolean enabled,
            int onlinePlayers,
            boolean lying,
            long lyingMilliseconds,
            long requiredMilliseconds,
            boolean normalWorld,
            long worldTime
    ) {
        return enabled
                && onlinePlayers == 1
                && lying
                && lyingMilliseconds >= requiredMilliseconds
                && normalWorld
                && worldTime >= 12_542L
                && worldTime < 23_460L;
    }
}
