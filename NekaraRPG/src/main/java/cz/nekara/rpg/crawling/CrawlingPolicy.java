package cz.nekara.rpg.crawling;

/** Pure rules for the server-authoritative voluntary crawling state. */
public final class CrawlingPolicy {
    private CrawlingPolicy() {
    }

    public static boolean canStart(boolean onGround, boolean dead, boolean sleeping, boolean gliding,
                                   boolean swimming, boolean insideVehicle, boolean flying) {
        return onGround && !dead && !sleeping && !gliding && !swimming && !insideVehicle && !flying;
    }
}
