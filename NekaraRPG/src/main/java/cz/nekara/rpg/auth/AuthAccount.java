package cz.nekara.rpg.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record AuthAccount(
        String username,
        String normalizedUsername,
        UUID lastKnownUuid,
        String passwordHash,
        Instant createdAt,
        Instant lastLoginAt
) {
    public AuthAccount {
        if (username == null || !username.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("Invalid Minecraft username.");
        }
        if (!normalize(username).equals(normalizedUsername)) {
            throw new IllegalArgumentException("Normalized username does not match username.");
        }
    }

    public static String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }

    public AuthAccount withSuccessfulLogin(UUID uuid, Instant loginAt) {
        return new AuthAccount(username, normalizedUsername, uuid, passwordHash, createdAt, loginAt);
    }

    public AuthAccount withPasswordHash(String updatedPasswordHash) {
        return new AuthAccount(username, normalizedUsername, lastKnownUuid,
                updatedPasswordHash, createdAt, lastLoginAt);
    }
}
