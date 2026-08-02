package cz.nekara.rpg.auth;

import java.net.InetAddress;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class AuthSessionManager {
    private final Duration duration;
    private final Map<String, Session> sessions = new HashMap<>();

    public AuthSessionManager(Duration duration) {
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Session duration must be positive.");
        }
        this.duration = duration;
    }

    public synchronized void remember(String username, InetAddress address, Instant now) {
        Session previous = sessions.put(AuthAccount.normalize(username),
                new Session(address.getAddress().clone(), now.plus(duration)));
        if (previous != null) {
            Arrays.fill(previous.address(), (byte) 0);
        }
    }

    public synchronized boolean isValid(String username, InetAddress address, Instant now) {
        String key = AuthAccount.normalize(username);
        Session session = sessions.get(key);
        if (session == null) {
            return false;
        }
        if (!now.isBefore(session.expiresAt())) {
            sessions.remove(key);
            return false;
        }
        byte[] candidate = address.getAddress();
        return MessageDigest.isEqual(session.address(), candidate);
    }

    public synchronized void invalidate(String username) {
        Session removed = sessions.remove(AuthAccount.normalize(username));
        if (removed != null) {
            Arrays.fill(removed.address(), (byte) 0);
        }
    }

    public synchronized void clear() {
        for (Session session : sessions.values()) {
            Arrays.fill(session.address(), (byte) 0);
        }
        sessions.clear();
    }

    public synchronized int size() {
        return sessions.size();
    }

    private record Session(byte[] address, Instant expiresAt) {
    }
}
