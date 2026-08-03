package cz.nekara.rpg.skills.experience;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.LongSupplier;

public final class RecentActionGuard {
    private final long timeToLiveMillis;
    private final LongSupplier currentTimeMillis;
    private final Map<String, Action> actions = new HashMap<>();

    public RecentActionGuard(Duration timeToLive) {
        this(timeToLive, System::currentTimeMillis);
    }

    public RecentActionGuard(Duration timeToLive, LongSupplier currentTimeMillis) {
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()
            || timeToLive.toMillis() < 1) {
            throw new IllegalArgumentException("Recent action TTL must be at least one millisecond");
        }
        if (currentTimeMillis == null) {
            throw new NullPointerException("currentTimeMillis");
        }
        this.timeToLiveMillis = timeToLive.toMillis();
        this.currentTimeMillis = currentTimeMillis;
    }

    public synchronized void record(String actorKey, String sourceKey) {
        requireKey(actorKey, "actorKey");
        requireKey(sourceKey, "sourceKey");
        long now = currentTimeMillis.getAsLong();
        purgeExpired(now);
        actions.put(actorKey, new Action(sourceKey, Math.addExact(now, timeToLiveMillis)));
    }

    public synchronized boolean consume(String actorKey, String sourceKey) {
        requireKey(actorKey, "actorKey");
        requireKey(sourceKey, "sourceKey");
        long now = currentTimeMillis.getAsLong();
        purgeExpired(now);
        Action action = actions.remove(actorKey);
        return action != null && action.sourceKey().equals(sourceKey);
    }

    public synchronized void forget(String actorKey) {
        requireKey(actorKey, "actorKey");
        actions.remove(actorKey);
    }

    public synchronized void clear() {
        actions.clear();
    }

    private void purgeExpired(long now) {
        Iterator<Action> iterator = actions.values().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().expiresAtMillis() <= now) {
                iterator.remove();
            }
        }
    }

    private static void requireKey(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }

    private record Action(String sourceKey, long expiresAtMillis) {
    }
}
