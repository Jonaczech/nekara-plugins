package cz.nekara.rpg.auth;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthSessionManagerTest {
    @Test
    void restoresOnlyTheSameNormalizedNicknameAndAddress() throws Exception {
        AuthSessionManager sessions = new AuthSessionManager(Duration.ofMinutes(10));
        Instant now = Instant.parse("2026-08-02T15:00:00Z");
        InetAddress original = InetAddress.getByName("192.0.2.10");

        sessions.remember("JonaCzech", original, now);

        assertTrue(sessions.isValid("jonaczech", original, now.plusSeconds(30)));
        assertFalse(sessions.isValid("jonaczech",
                InetAddress.getByName("192.0.2.11"), now.plusSeconds(30)));
        assertFalse(sessions.isValid("OtherPlayer", original, now.plusSeconds(30)));
    }

    @Test
    void expiresAndCanBeExplicitlyInvalidated() throws Exception {
        AuthSessionManager sessions = new AuthSessionManager(Duration.ofMinutes(10));
        Instant now = Instant.parse("2026-08-02T15:00:00Z");
        InetAddress address = InetAddress.getByName("2001:db8::10");

        sessions.remember("Player", address, now);
        assertFalse(sessions.isValid("Player", address, now.plusSeconds(600)));
        assertEquals(0, sessions.size());

        sessions.remember("Player", address, now);
        sessions.invalidate("PLAYER");
        assertFalse(sessions.isValid("Player", address, now.plusSeconds(1)));
    }
}
