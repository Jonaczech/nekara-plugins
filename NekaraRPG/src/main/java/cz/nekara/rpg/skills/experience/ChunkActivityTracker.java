package cz.nekara.rpg.skills.experience;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class ChunkActivityTracker {
    private final long windowMillis;
    private final int capacity;
    private final LongSupplier currentTimeMillis;
    private final LinkedHashMap<ChunkKey, Deque<Long>> awards = new LinkedHashMap<>(16, 0.75f, true);

    public ChunkActivityTracker(Duration window, int capacity) {
        this(window, capacity, System::currentTimeMillis);
    }

    public ChunkActivityTracker(Duration window, int capacity, LongSupplier currentTimeMillis) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(currentTimeMillis, "currentTimeMillis");
        if (window.isZero() || window.isNegative() || window.toMillis() < 1) {
            throw new IllegalArgumentException("Chunk activity window must be at least one millisecond");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("Chunk activity capacity must be positive");
        }
        this.windowMillis = window.toMillis();
        this.capacity = capacity;
        this.currentTimeMillis = currentTimeMillis;
    }

    public synchronized int recentAwards(ChunkKey key) {
        Objects.requireNonNull(key, "key");
        long now = currentTimeMillis.getAsLong();
        purgeExpired(now);
        Deque<Long> timestamps = awards.get(key);
        return timestamps == null ? 0 : timestamps.size();
    }

    public synchronized void recordAward(ChunkKey key) {
        reserveAward(key);
    }

    public synchronized int reserveAward(ChunkKey key) {
        Objects.requireNonNull(key, "key");
        long now = currentTimeMillis.getAsLong();
        purgeExpired(now);
        Deque<Long> timestamps = awards.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        int recentAwards = timestamps.size();
        timestamps.addLast(now);
        while (awards.size() > capacity) {
            Iterator<ChunkKey> iterator = awards.keySet().iterator();
            iterator.next();
            iterator.remove();
        }
        return recentAwards;
    }

    public synchronized void clear() {
        awards.clear();
    }

    private void purgeExpired(long now) {
        long cutoff = now - windowMillis;
        Iterator<Map.Entry<ChunkKey, Deque<Long>>> iterator = awards.entrySet().iterator();
        while (iterator.hasNext()) {
            Deque<Long> timestamps = iterator.next().getValue();
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.removeFirst();
            }
            if (timestamps.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public record ChunkKey(UUID worldId, int x, int z) {
        public ChunkKey {
            Objects.requireNonNull(worldId, "worldId");
        }
    }
}
