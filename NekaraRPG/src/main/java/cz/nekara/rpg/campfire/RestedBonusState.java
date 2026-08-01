package cz.nekara.rpg.campfire;

public final class RestedBonusState {
    private long expiresAtMillis;
    private int durationSeconds;
    private boolean hasteEnabled;
    private double hungerLossCarry;

    public RestedBonusState(long expiresAtMillis, int durationSeconds, boolean hasteEnabled) {
        this.expiresAtMillis = expiresAtMillis;
        this.durationSeconds = durationSeconds;
        this.hasteEnabled = hasteEnabled;
    }

    public boolean isActive(long nowMillis) {
        return nowMillis < expiresAtMillis;
    }

    public long remainingSeconds(long nowMillis) {
        long remainingMillis = Math.max(0L, expiresAtMillis - nowMillis);
        return (remainingMillis + 999L) / 1_000L;
    }

    public void refresh(long expiresAtMillis, int durationSeconds, boolean hasteEnabled) {
        this.expiresAtMillis = expiresAtMillis;
        this.durationSeconds = durationSeconds;
        this.hasteEnabled = hasteEnabled;
    }

    public int durationSeconds() {
        return durationSeconds;
    }

    public boolean hasteEnabled() {
        return hasteEnabled;
    }

    public double hungerLossCarry() {
        return hungerLossCarry;
    }

    public void hungerLossCarry(double hungerLossCarry) {
        this.hungerLossCarry = hungerLossCarry;
    }
}
