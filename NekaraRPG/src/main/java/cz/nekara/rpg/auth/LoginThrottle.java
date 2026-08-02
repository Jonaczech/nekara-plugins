package cz.nekara.rpg.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public final class LoginThrottle {
    private final int maximumAttempts;
    private final Duration lockoutDuration;
    private final Map<String, AttemptState> attempts = new HashMap<>();

    public LoginThrottle(int maximumAttempts, Duration lockoutDuration) {
        if (maximumAttempts < 1 || lockoutDuration.isNegative() || lockoutDuration.isZero()) {
            throw new IllegalArgumentException("Invalid login throttle settings.");
        }
        this.maximumAttempts = maximumAttempts;
        this.lockoutDuration = lockoutDuration;
    }

    public synchronized Duration remainingLockout(String username, Instant now) {
        String key = AuthAccount.normalize(username);
        AttemptState state = attempts.get(key);
        if (state == null || state.lockedUntil() == null) {
            return Duration.ZERO;
        }
        if (!now.isBefore(state.lockedUntil())) {
            attempts.remove(key);
            return Duration.ZERO;
        }
        return Duration.between(now, state.lockedUntil());
    }

    public synchronized Failure registerFailure(String username, Instant now) {
        String key = AuthAccount.normalize(username);
        AttemptState previous = attempts.get(key);
        if (previous != null && previous.lockedUntil() != null && now.isBefore(previous.lockedUntil())) {
            return new Failure(0, Duration.between(now, previous.lockedUntil()));
        }
        int failures = previous == null ? 1 : previous.failures() + 1;
        if (failures >= maximumAttempts) {
            Instant lockedUntil = now.plus(lockoutDuration);
            attempts.put(key, new AttemptState(0, lockedUntil));
            return new Failure(0, lockoutDuration);
        }
        attempts.put(key, new AttemptState(failures, null));
        return new Failure(maximumAttempts - failures, Duration.ZERO);
    }

    public synchronized void registerSuccess(String username) {
        attempts.remove(AuthAccount.normalize(username));
    }

    public record Failure(int remainingAttempts, Duration lockout) {
        public boolean locked() {
            return !lockout.isZero();
        }
    }

    private record AttemptState(int failures, Instant lockedUntil) {
    }
}
