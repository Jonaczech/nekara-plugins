package cz.nekara.rpg.campfire;

import java.util.UUID;

public final class CampfireRestSession {
    private final UUID playerId;
    private final CampfireKey campfire;
    private final long startedAtMillis;
    private long nextHealAtMillis;
    private long nextHungerRestoreAtMillis;
    private double hungerRestoreCarry;
    private boolean restedGranted;

    public CampfireRestSession(
            UUID playerId,
            CampfireKey campfire,
            long startedAtMillis,
            int healPeriodSeconds,
            int hungerRestorePeriodSeconds
    ) {
        this.playerId = playerId;
        this.campfire = campfire;
        this.startedAtMillis = startedAtMillis;
        this.nextHealAtMillis = startedAtMillis + healPeriodSeconds * 1_000L;
        this.nextHungerRestoreAtMillis = startedAtMillis + hungerRestorePeriodSeconds * 1_000L;
    }

    public UUID playerId() {
        return playerId;
    }

    public CampfireKey campfire() {
        return campfire;
    }

    public long elapsedSeconds(long nowMillis) {
        return Math.max(0L, (nowMillis - startedAtMillis) / 1_000L);
    }

    public boolean shouldHeal(long nowMillis) {
        return nowMillis >= nextHealAtMillis;
    }

    public void scheduleNextHeal(long nowMillis, int periodSeconds) {
        nextHealAtMillis = nowMillis + periodSeconds * 1_000L;
    }

    public boolean shouldRestoreHunger(long nowMillis) {
        return nowMillis >= nextHungerRestoreAtMillis;
    }

    public void scheduleNextHungerRestore(long nowMillis, int periodSeconds) {
        nextHungerRestoreAtMillis = nowMillis + periodSeconds * 1_000L;
    }

    public double hungerRestoreCarry() {
        return hungerRestoreCarry;
    }

    public void hungerRestoreCarry(double hungerRestoreCarry) {
        this.hungerRestoreCarry = hungerRestoreCarry;
    }

    public boolean restedGranted() {
        return restedGranted;
    }

    public void restedGranted(boolean restedGranted) {
        this.restedGranted = restedGranted;
    }
}
