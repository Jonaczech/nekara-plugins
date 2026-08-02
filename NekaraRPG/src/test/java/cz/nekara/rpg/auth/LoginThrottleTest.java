package cz.nekara.rpg.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginThrottleTest {
    @Test
    void locksNormalizedNicknameAfterConfiguredFailures() {
        LoginThrottle throttle = new LoginThrottle(3, Duration.ofSeconds(60));
        Instant now = Instant.parse("2026-08-02T12:00:00Z");

        assertEquals(2, throttle.registerFailure("Jonaczech", now).remainingAttempts());
        assertEquals(1, throttle.registerFailure("JONACZECH", now).remainingAttempts());
        LoginThrottle.Failure locked = throttle.registerFailure("jonaczech", now);

        assertTrue(locked.locked());
        assertEquals(Duration.ofSeconds(60), throttle.remainingLockout("JoNaCzEcH", now));
    }

    @Test
    void lockExpiresAndSuccessfulLoginClearsFailures() {
        LoginThrottle throttle = new LoginThrottle(2, Duration.ofSeconds(30));
        Instant now = Instant.parse("2026-08-02T12:00:00Z");

        throttle.registerFailure("Player", now);
        throttle.registerSuccess("player");
        assertFalse(throttle.registerFailure("PLAYER", now).locked());
        assertTrue(throttle.registerFailure("player", now).locked());
        assertEquals(Duration.ZERO,
                throttle.remainingLockout("Player", now.plusSeconds(31)));
    }
}
