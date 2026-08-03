package cz.nekara.rpg.skills.experience;

import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class ExperienceGrantGuard {
    private final long timeToLiveMillis;
    private final int capacity;
    private final LongSupplier currentTimeMillis;
    private final LinkedHashMap<ExperienceFingerprint, Long> expirations = new LinkedHashMap<>();

    public ExperienceGrantGuard(Duration timeToLive, int capacity) {
        this(timeToLive, capacity, System::currentTimeMillis);
    }

    public ExperienceGrantGuard(Duration timeToLive, int capacity, LongSupplier currentTimeMillis) {
        Objects.requireNonNull(timeToLive, "timeToLive");
        Objects.requireNonNull(currentTimeMillis, "currentTimeMillis");
        if (timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("Experience grant TTL must be positive");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("Experience grant capacity must be positive");
        }
        this.timeToLiveMillis = timeToLive.toMillis();
        if (timeToLiveMillis < 1) {
            throw new IllegalArgumentException("Experience grant TTL must be at least one millisecond");
        }
        this.capacity = capacity;
        this.currentTimeMillis = currentTimeMillis;
    }

    public synchronized boolean tryAcquire(ExperienceFingerprint fingerprint) {
        Objects.requireNonNull(fingerprint, "fingerprint");
        long now = currentTimeMillis.getAsLong();
        purgeExpired(now);

        Long expiration = expirations.get(fingerprint);
        if (expiration != null && expiration > now) {
            return false;
        }

        long newExpiration = Math.addExact(now, timeToLiveMillis);
        expirations.put(fingerprint, newExpiration);
        while (expirations.size() > capacity) {
            Iterator<ExperienceFingerprint> iterator = expirations.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        return true;
    }

    public synchronized int trackedFingerprintCount() {
        purgeExpired(currentTimeMillis.getAsLong());
        return expirations.size();
    }

    private void purgeExpired(long now) {
        Iterator<Map.Entry<ExperienceFingerprint, Long>> iterator = expirations.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() <= now) {
                iterator.remove();
            }
        }
    }
}
